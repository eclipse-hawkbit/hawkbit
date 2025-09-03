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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifactHash;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.spi.ArtifactHandler;
import org.springframework.hateoas.Link;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

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

        protected final Map<String, Path> downloads = new HashMap<>();
        private static final String LOG_PREFIX = "[{}:{}] ";
        private static final String DOWNLOAD_LOG_MESSAGE = "Download ";
        private static final String EXPECTED = "(Expected: ";
        private static final String BUT_GOT_LOG_MESSAGE = " but got: ";
        private final DdiController ddiController;
        private final DdiDeployment.HandlingType updateType;
        private final List<DdiChunk> modules;
        private final ArtifactHandler artifactHandler;

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
                    if (updateStatus.status() != UpdateStatus.Status.FAILURE && updateType != DdiDeployment.HandlingType.SKIP) {
                        ddiController.sendFeedback(update());
                    }
                } catch (final Exception ex) {
                    log.error("Failed to download artifact. Reason : ", ex);
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
                                    .map(art -> "Download start for: " + art.getFilename() + " with size " + art.getSize() + " and hashes " + art.getHashes() + " ...")
                                    .toList()));

            log.info(LOG_PREFIX + "Start download", ddiController.getTenantId(), ddiController.getControllerId());

            final List<UpdateStatus> updateStatusList = new ArrayList<>();
            modules.forEach(module -> module.getArtifacts().forEach(artifact -> handleArtifact(updateStatusList, artifact)));

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
                    final String actual = HexFormat.of().withLowerCase().formatHex(messageDigest.digest());
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

        private void handleArtifact(final List<UpdateStatus> status, final DdiArtifact artifact) {
            artifact.getLink("download").ifPresentOrElse(
                    // HTTPS
                    link -> status.add(downloadUrl(link, artifact.getHashes(), artifact.getSize())),
                    // HTTP
                    () -> status.add(downloadUrl(
                            artifact.getLink("download-http").orElseThrow(() -> new IllegalArgumentException("Nor https nor http found!")),
                            artifact.getHashes(), artifact.getSize()))
            );
        }

        private UpdateStatus downloadUrl(final Link link, final DdiArtifactHash hash, final long size) {
            if (log.isDebugEnabled()) {
                log.debug(LOG_PREFIX + "Downloading {}, expected hash {} and size {}",
                        ddiController.getTenantId(), ddiController.getControllerId(), link.getHref(), hash, size);
            }

            try {
                return readAndCheckDownloadUrl(link, hash, size);
            } catch (final NoSuchAlgorithmException | IOException e) {
                log.error(LOG_PREFIX + "Failed to download {}",
                        ddiController.getTenantId(), ddiController.getControllerId(), link.getHref(), e);
                return new UpdateStatus(
                        UpdateStatus.Status.FAILURE,
                        List.of("Failed to download " + link.getHref() + ": " + e.getMessage()));
            }
        }

        private UpdateStatus readAndCheckDownloadUrl(final Link link, final DdiArtifactHash hash, final long size)
                throws NoSuchAlgorithmException, IOException {
            final Validator sizeValidator = sizeValidator(size);
            final Validator hashValidator = hashValidator(hash);
            final String href = link.getHref();
            final ArtifactHandler.DownloadHandler downloadHandler = artifactHandler.getDownloadHandler(href);
            return HawkbitClient.getLink(link, InputStream.class, ddiController.getTenant(), ddiController.getController(), is -> {
                try {
                    final byte[] buff = new byte[32 * 1024];
                    for (int read; (read = is.read(buff)) != -1; ) {
                        sizeValidator.read(buff, read);
                        hashValidator.read(buff, read);
                        downloadHandler.read(buff, 0, read);
                    }
                    sizeValidator.validate();
                    hashValidator.validate();

                    final String message = "Downloaded " + href + " (" + size + " bytes)";
                    log.debug(LOG_PREFIX + message, ddiController.getTenantId(), ddiController.getControllerId());
                    downloadHandler.finished(ArtifactHandler.DownloadHandler.Status.SUCCESS);
                    downloadHandler.download().ifPresent(path -> downloads.put(href, path));
                    return new UpdateStatus(UpdateStatus.Status.SUCCESSFUL, List.of(message));
                } catch (final Exception e) {
                    final String message = e.getMessage();
                    if (log.isTraceEnabled()) {
                        log.error(LOG_PREFIX + DOWNLOAD_LOG_MESSAGE + "{} failed: {}",
                                ddiController.getTenantId(), ddiController.getControllerId(), href, message, e);
                    } else {
                        log.error(LOG_PREFIX + DOWNLOAD_LOG_MESSAGE + "{} failed: {}" + message,
                                ddiController.getTenantId(), ddiController.getControllerId(), href, message);
                    }
                    downloadHandler.finished(ArtifactHandler.DownloadHandler.Status.ERROR);
                    return new UpdateStatus(UpdateStatus.Status.FAILURE, List.of(message));
                }
            });
        }

        private interface Validator {

            void read(final byte[] buff, final int len);

            void validate();
        }
    }
}