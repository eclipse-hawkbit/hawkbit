/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.device;

import com.google.common.io.BaseEncoding;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifactHash;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment;
import org.eclipse.hawkbit.sdk.spi.ArtifactHandler;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Update handler provide plug-in endpoint allowing for customization of the update processing.
 */
public interface UpdateHandler {

    UpdateHandler SKIP = (controller, updateType, modules) ->
            new UpdateProcessor(controller, updateType, modules, ArtifactHandler.SKIP);

    /**
     * Creates an update processor for a single software update
     *
     * @param controller controller instance
     * @param updateType the update handleing type
     * @param modules the modules to be installed
     * @return the update processor
     */
    UpdateProcessor getUpdateProcessor(
            final DdiController controller,
            final DdiDeployment.HandlingType updateType, final List<DdiChunk> modules);

    @Slf4j
    class UpdateProcessor implements Runnable {

        private static final String LOG_PREFIX = "[{}:{}] ";

        private static final String DOWNLOAD_LOG_MESSAGE = "Download ";
        private static final String EXPECTED = "(Expected: ";
        private static final String BUT_GOT_LOG_MESSAGE = " but got: ";
        private static final int MINIMUM_TOKEN_LENGTH_FOR_HINT = 6;

        private final DdiController ddiController;

        private final DdiDeployment.HandlingType updateType;
        private final List<DdiChunk> modules;

        private final ArtifactHandler artifactHandler;
        protected final Map<String, Path> downloads = new HashMap<>();

        public UpdateProcessor(
                final DdiController ddiController,
                final DdiDeployment.HandlingType updateType, final List<DdiChunk> modules,
                final ArtifactHandler artifactHandler) {
            this.ddiController = ddiController;

            this.updateType = updateType;
            this.modules = modules;

            this.artifactHandler = artifactHandler;
        }

        @Override
        public void run() {
            ddiController.sendFeedback(new UpdateStatus(UpdateStatus.Status.PROCEEDING, List.of("Update begin ...")));

            if (!CollectionUtils.isEmpty(modules)) {
                try {
                    final UpdateStatus updateStatus = download();
                    ddiController.sendFeedback(updateStatus);
                    if (updateStatus.status() == UpdateStatus.Status.FAILURE) {
                        return;
                    } else {
                        if (updateType != DdiDeployment.HandlingType.SKIP) {
                            ddiController.sendFeedback(update());
                        }
                    }
                } finally {
                    cleanup();
                }
            }

            if (updateType == DdiDeployment.HandlingType.SKIP) {
                ddiController.sendFeedback(
                        new UpdateStatus(UpdateStatus.Status.SUCCESSFUL, List.of("Update (download-only) completed.")));
            }
        }

        /**
         * Extension point. An overriding implementation could completely skip the default download and provide its own.
         * By contract, it shall fill up {@link #downloads};
         *
         * @return the status of the download
         */
        protected UpdateStatus download() {
            ddiController.sendFeedback(
                    new UpdateStatus(
                            UpdateStatus.Status.DOWNLOAD,
                            modules.stream().flatMap(mod -> mod.getArtifacts().stream())
                                    .map(art -> "Download start for: " + art.getFilename() +
                                            " with size " + art.getSize() +
                                            " and hashes " + art.getHashes() + " ...")
                                    .collect(Collectors.toList())));

            log.info(LOG_PREFIX + "Start download", ddiController.getTenantId(), ddiController.getControllerId());

            final List<UpdateStatus> updateStatusList = new ArrayList<>();
            modules.forEach(module -> module.getArtifacts().forEach(artifact -> {
                if (ddiController.isDownloadAuthenticationEnabled()) {
                    handleArtifact(
                            ddiController.getTargetSecurityToken(), ddiController.getGatewayToken(),
                            updateStatusList, artifact);
                } else {
                    handleArtifact(null, null, updateStatusList, artifact);
                }
            }));

            log.info(LOG_PREFIX + "Download complete.", ddiController.getTenantId(), ddiController.getControllerId());

            final List<String> messages = new LinkedList<>();
            messages.add("Download complete.");
            updateStatusList.forEach(download -> messages.addAll(download.messages()));
            return new UpdateStatus(
                    updateStatusList.stream().anyMatch(status -> status.status() == UpdateStatus.Status.FAILURE) ?
                            UpdateStatus.Status.FAILURE : UpdateStatus.Status.DOWNLOADED,
                    messages);
        }

        /**
         * Extension point. Called after all artifacts has been successfully downloaded. An overriding implementation
         * may get the {@link #downloads} map and apply them
         */
        protected UpdateStatus update() {
            log.info(LOG_PREFIX + "Updated", ddiController.getTenantId(), ddiController.getControllerId());
            return new UpdateStatus(UpdateStatus.Status.SUCCESSFUL, List.of("Update complete."));
        }

        /**
         * Extension point. Called after download and update has been finished. By default, it deletes all downloaded
         * files (if any).
         */
        protected void cleanup() {
            downloads.values().forEach(path -> {
                try {
                    Files.delete(path);
                } catch (final IOException e) {
                    log.warn(LOG_PREFIX + "Failed to cleanup {}",
                            ddiController.getTenantId(), ddiController.getControllerId(),
                            path.toFile().getAbsolutePath(), e);
                }
            });
            log.debug(LOG_PREFIX + "Cleaned up", ddiController.getTenantId(), ddiController.getControllerId());
        }

        private void handleArtifact(
                final String targetToken, final String gatewayToken,
                final List<UpdateStatus> status, final DdiArtifact artifact) {
            artifact.getLink("download").ifPresentOrElse(
                    // HTTPS
                    link -> status.add(downloadUrl(link.getHref(), gatewayToken, targetToken,
                            artifact.getHashes(), artifact.getSize())),
                    // HTTP
                    () -> status.add(downloadUrl(
                            artifact.getLink("download-http")
                                    .map(Link::getHref)
                                    .orElseThrow(() -> new IllegalArgumentException("Nor https nor http found!")),
                            gatewayToken, targetToken,
                            artifact.getHashes(), artifact.getSize()))
            );
        }

        private UpdateStatus downloadUrl(
                final String url, final String gatewayToken, final String targetToken,
                final DdiArtifactHash hash, final long size) {
            if (log.isDebugEnabled()) {
                log.debug(LOG_PREFIX + "Downloading {} with token {}, expected hash {} and size {}",
                        ddiController.getTenantId(), ddiController.getControllerId(), url,
                        hideTokenDetails(targetToken), hash, size);
            }

            try {
                return readAndCheckDownloadUrl(url, gatewayToken, targetToken, hash, size);
            } catch (final IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                log.error(LOG_PREFIX + "Failed to download {}",
                        ddiController.getTenantId(), ddiController.getControllerId(), url, e);
                return new UpdateStatus(
                        UpdateStatus.Status.FAILURE,
                        List.of("Failed to download " + url + ": " + e.getMessage()));
            }
        }

        private UpdateStatus readAndCheckDownloadUrl(final String url, final String gatewayToken,
                final String targetToken, final DdiArtifactHash hash, final long size)
                throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
            final Validator sizeValidator = sizeValidator(size);
            final Validator hashValidator = hashValidator(hash);
            final ArtifactHandler.DownloadHandler downloadHandler = artifactHandler.getDownloadHandler(url);

            try (final CloseableHttpClient httpclient = createHttpClientThatAcceptsAllServerCerts()) {
                final HttpGet request = new HttpGet(url);
                if (StringUtils.hasLength(targetToken)) {
                    request.addHeader(HttpHeaders.AUTHORIZATION, "TargetToken " + targetToken);
                } else if (StringUtils.hasLength(gatewayToken)) {
                    request.addHeader(HttpHeaders.AUTHORIZATION, "GatewayToken " + gatewayToken);
                }

                return httpclient.execute(request, response -> {
                    try {
                        if (response.getCode() != HttpStatus.OK.value()) {
                            throw new IllegalStateException("Unexpected status code: " + response.getCode());
                        }

                        if (response.getEntity().getContentLength() != size) {
                            throw new IllegalArgumentException("Wrong content length " + EXPECTED + size + BUT_GOT_LOG_MESSAGE + response.getEntity()
                                    .getContentLength() + ")!");
                        }

                        final byte[] buff = new byte[32 * 1024];
                        try (final InputStream is = response.getEntity().getContent()) {
                            for (int read; (read = is.read(buff)) != -1; ) {
                                sizeValidator.read(buff, read);
                                hashValidator.read(buff, read);
                                downloadHandler.read(buff, 0, read);
                            }
                        }
                        sizeValidator.validate();
                        hashValidator.validate();

                        final String message = "Downloaded " + url + " (" + size + " bytes)";
                        log.debug(LOG_PREFIX + message, ddiController.getTenantId(), ddiController.getControllerId());
                        downloadHandler.finished(ArtifactHandler.DownloadHandler.Status.SUCCESS);
                        downloadHandler.download().ifPresent(path -> downloads.put(url, path));
                        return new UpdateStatus(UpdateStatus.Status.SUCCESSFUL, List.of(message));
                    } catch (final Exception e) {
                        final String message = e.getMessage();
                        if (log.isTraceEnabled()) {
                            log.error(LOG_PREFIX + DOWNLOAD_LOG_MESSAGE + url + " failed: " + message,
                                    ddiController.getTenantId(), ddiController.getControllerId(), e);
                        } else {
                            log.error(LOG_PREFIX + DOWNLOAD_LOG_MESSAGE + url + " failed: " + message,
                                    ddiController.getTenantId(), ddiController.getControllerId());
                        }
                        downloadHandler.finished(ArtifactHandler.DownloadHandler.Status.ERROR);
                        return new UpdateStatus(UpdateStatus.Status.FAILURE, List.of(message));
                    }
                });
            }
        }

        private static String hideTokenDetails(final String targetToken) {
            if (targetToken == null) {
                return "<NULL!>";
            }

            if (targetToken.isEmpty()) {
                return "<EMPTY!>";
            }

            if (targetToken.length() <= MINIMUM_TOKEN_LENGTH_FOR_HINT) {
                return "***";
            }

            return targetToken.substring(0, 2) + "***" + targetToken.substring(targetToken.length() - 2);
        }

        private static CloseableHttpClient createHttpClientThatAcceptsAllServerCerts()
                throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
            return HttpClients
                    .custom()
                    .setConnectionManager(
                            PoolingHttpClientConnectionManagerBuilder.create()
                                    .setSSLSocketFactory(
                                            new SSLConnectionSocketFactory(
                                                    SSLContextBuilder
                                                            .create()
                                                            .loadTrustMaterial(null, (chain, authType) -> true)
                                                            .build()))
                                    .build()
                    )
                    .build();
        }


        private interface Validator {

            void read(final byte[] buff, final int len);

            void validate();
        }

        private static Validator sizeValidator(final long size) {
            return new Validator() {

                private int read;

                @Override
                public void read(final byte[] buff, final int len) {
                    read += len;
                    if (read > size) {
                        throw new SecurityException("Size mismatch: read more " + EXPECTED + size + BUT_GOT_LOG_MESSAGE + read + ")!");
                    }
                }

                @Override
                public void validate() {
                    if (read != size) {
                        throw new SecurityException("Size mismatch " + EXPECTED + size + BUT_GOT_LOG_MESSAGE + read + ")!");
                    }
                }
            };
        }

        private static Validator hashValidator(final DdiArtifactHash hash) throws NoSuchAlgorithmException {

            class HashValidator {

                private final String expected;
                private final MessageDigest messageDigest;

                private HashValidator(final String expected, final MessageDigest messageDigest) {
                    this.expected = expected;
                    this.messageDigest = messageDigest;
                }

                private void update(final byte[] buff, final int len) {
                    messageDigest.update(buff, 0, len);
                }

                private void check() {
                    final String actual = BaseEncoding.base16().lowerCase().encode(messageDigest.digest());
                    if (!actual.equals(expected)) {
                        throw new SecurityException(
                                messageDigest.getAlgorithm() + " hash mismatch " + EXPECTED + expected + BUT_GOT_LOG_MESSAGE + actual + ")!");
                    }
                }
            }

            final List<HashValidator> hashValidators = new ArrayList<>(3);
            if (!ObjectUtils.isEmpty(hash.getSha256())) {
                hashValidators.add(new HashValidator(hash.getSha256(), MessageDigest.getInstance("SHA-256")));
            }
            if (!ObjectUtils.isEmpty(hash.getSha1())) {
                hashValidators.add(new HashValidator(hash.getSha1(), MessageDigest.getInstance("SHA-1")));
            }
            if (!ObjectUtils.isEmpty(hash.getMd5())) {
                hashValidators.add(new HashValidator(hash.getMd5(), MessageDigest.getInstance("MD5")));
            }
            if (hashValidators.isEmpty()) {
                throw new SecurityException("No hashes in " + hash + "!");
            }

            return new Validator() {
                @Override
                public void read(final byte[] buff, final int len) {
                    hashValidators.forEach(hashValidator -> hashValidator.update(buff, len));
                }

                @Override
                public void validate() {
                    hashValidators.forEach(HashValidator::check);
                }
            };
        }
    }
}