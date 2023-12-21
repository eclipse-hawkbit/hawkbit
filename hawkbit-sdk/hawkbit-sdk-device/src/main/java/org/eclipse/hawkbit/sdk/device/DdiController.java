/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.device;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiConfigData;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment;
import org.eclipse.hawkbit.ddi.json.model.DdiDeploymentBase;
import org.eclipse.hawkbit.ddi.json.model.DdiUpdateMode;
import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract class representing DDI device connecting directly to hawkVit.
 */
@Slf4j
@Getter
public class DdiController {

    private static final String LOG_PREFIX = "[{}:{}] ";

    // TODO - make them configurable
    private static final long IMMEDIATE_MS = 10;
    private static final long DEFAULT_POLL_MS = 5_000;

    private static final String DEPLOYMENT_BASE_LINK = "deploymentBase";
    private static final String CONFIRMATION_BASE_LINK = "confirmationBase";

    private final String tenantId;
    private final String controllerId;
    private final DdiRootControllerRestApi ddiApi;

    // configuration
    private final boolean downloadAuthenticationEnabled;
    private final String gatewayToken;
    private final String targetSecurityToken;
    @Setter
    @Accessors(chain = true)
    private long overridePollMillis = -1; // -1 means disabled

    // state
    private volatile ScheduledExecutorService executorService;
    private volatile Long currentActionId;
    private volatile UpdateStatus updateStatus;

    /**
     * Creates a new device instance.
     *
     * @param tenant the tenant of the device belongs to
     * @param controller the the controller
     * @param hawkbitClient a factory for creaint to {@link DdiRootControllerRestApi} (and moreused)
     *                      for communication to hawkBit
     */
    public DdiController(final Tenant tenant, final Controller controller, final HawkbitClient hawkbitClient) {
        this.tenantId = tenant.getTenantId();
        gatewayToken = tenant.getGatewayToken();
        downloadAuthenticationEnabled = tenant.isDownloadAuthenticationEnabled();
        this.controllerId = controller.getControllerId();
        this.targetSecurityToken = controller.getSecurityToken();
        ddiApi = hawkbitClient.ddiService(DdiRootControllerRestApi.class, tenant, controller);
    }

    // expects single threaded {@link java.util.concurrent.ScheduledExecutorService}
    public void start(final ScheduledExecutorService executorService) {
        Objects.requireNonNull(executorService, "Require non null executor!");

        this.executorService = executorService;
        executorService.submit(this::poll);
    }

    public void stop() {
        executorService = null;
        currentActionId = null;
    }

    private void poll() {
        Optional.ofNullable(executorService).ifPresent(executor -> {
            getControllerBase().ifPresentOrElse(
                    controllerBase -> {
                        final Optional<Link> confirmationBaseLink = getRequiredLink(controllerBase, CONFIRMATION_BASE_LINK);
                        if (confirmationBaseLink.isPresent()) {
                            final long actionId = getActionId(confirmationBaseLink.get());
                            log.info(LOG_PREFIX + "Confirmation is required for action {}!", getTenantId(),
                                    getControllerId(), actionId);
                            // TODO - confirmation handler
                            sendConfirmationFeedback(actionId);
                            executor.schedule(this::poll, IMMEDIATE_MS, TimeUnit.MILLISECONDS);
                        } else {
                            getRequiredLink(controllerBase, DEPLOYMENT_BASE_LINK).flatMap(this::getActionWithDeployment).ifPresentOrElse(actionWithDeployment -> {
                                final long actionId = actionWithDeployment.getKey();
                                if (currentActionId == null) {
                                    log.info(LOG_PREFIX + "Process action {}", getTenantId(), getControllerId(),
                                            actionId);
                                    final DdiDeployment deployment = actionWithDeployment.getValue().getDeployment();
                                    final DdiDeployment.HandlingType updateType = deployment.getUpdate();
                                    final List<DdiChunk> modules = deployment.getChunks();

                                    currentActionId = actionId;
                                    executor.submit(new UpdateProcessor(actionId, updateType, modules));
                                } else if (currentActionId != actionId) {
                                    // TODO - cancel and start new one?
                                    log.info(LOG_PREFIX + "Action {} is canceled while in process!", getTenantId(),
                                            getControllerId(), getCurrentActionId());
                                } // else same action - already processing
                            }, () -> {
                                if (currentActionId != null) {
                                    // TODO - cancel current?
                                    log.info(LOG_PREFIX + "Action {} is canceled while in process!", getTenantId(),
                                            getControllerId(), getCurrentActionId());
                                }
                            });
                            executor.schedule(this::poll, getPollMillis(controllerBase), TimeUnit.MILLISECONDS);
                        }
                    },
                    () -> {
                        // error has occurred or no controller base hasn't been acquired
                        executor.schedule(this::poll, DEFAULT_POLL_MS, TimeUnit.MILLISECONDS);
                    }
            );
        });
    }

    private Optional<DdiControllerBase> getControllerBase() {
        log.trace(LOG_PREFIX + "Polling ...", getTenantId(), getControllerId());
        final ResponseEntity<DdiControllerBase> poll;
        try {
            poll = getDdiApi().getControllerBase(getTenantId(), getControllerId());
        } catch (final RuntimeException ex) {
            log.error(LOG_PREFIX + "Failed base poll", getTenantId(), getControllerId(), ex);
            return Optional.empty();
        }

        if (poll.getStatusCode() != HttpStatus.OK) {
            log.error(LOG_PREFIX + "Failed base poll {}", getTenantId(), getControllerId(), poll.getStatusCode());
            return Optional.empty();
        }

        return Optional.ofNullable(poll.getBody());
    }

    private Optional<Link> getRequiredLink(final DdiControllerBase controllerBase, final String nameOfTheLink) {
        final Optional<Link> link = controllerBase != null ? controllerBase.getLink(nameOfTheLink) : Optional.empty();
        link.ifPresentOrElse(
                l -> log.debug(LOG_PREFIX + "Polling finished. Has {} link: {}", getTenantId(), getControllerId(), nameOfTheLink, l),
                () -> log.trace(LOG_PREFIX + "Polling finished. No {} link", getTenantId(), getControllerId(), nameOfTheLink));
        return link;
    }

    private long getPollMillis(final DdiControllerBase controllerBase) {
        if (overridePollMillis >= 0) {
            return overridePollMillis;
        }

        final String pollingTimeFromResponse = controllerBase.getConfig().getPolling().getSleep();
        if (pollingTimeFromResponse == null) {
            return DEFAULT_POLL_MS;
        } else {
            final LocalTime localtime = LocalTime.parse(pollingTimeFromResponse);
            return localtime.getLong(ChronoField.MILLI_OF_DAY);
        }
    }

    private Optional<Map.Entry<Long, DdiDeploymentBase>> getActionWithDeployment(final Link deploymentBaseLink) {
        final long actionId = getActionId(deploymentBaseLink);
        final ResponseEntity<DdiDeploymentBase> action = getDdiApi()
                .getControllerDeploymentBaseAction(getTenantId(), getControllerId(), actionId, -1, null);
        if (action.getStatusCode() != HttpStatus.OK) {
            log.warn(LOG_PREFIX + "Fail to get deployment action: {} -> {}", getTenantId(), getControllerId(), actionId, action.getStatusCode());
            return Optional.empty();
        }

        return Optional.ofNullable(action.getBody() == null ? null : new AbstractMap.SimpleEntry<>(actionId, action.getBody()));
    }

    public void updateAttribute(final String mode, final String key, final String value) {
        final DdiUpdateMode updateMode = switch (mode.toLowerCase()) {
            case "replace" -> DdiUpdateMode.REPLACE;
            case "remove" -> DdiUpdateMode.REMOVE;
            default -> DdiUpdateMode.MERGE;
        };

        final DdiConfigData configData = new DdiConfigData(Collections.singletonMap(key, value), updateMode);

        getDdiApi().putConfigData(configData, getTenantId(), getControllerId());
    }

    private void sendFeedback(final long actionId) {
        getDdiApi().postDeploymentBaseActionFeedback(updateStatus.feedback(), getTenantId(), getControllerId(), actionId);
        currentActionId = null;
    }

    private void sendConfirmationFeedback(final long actionId) {
        final DdiConfirmationFeedback ddiConfirmationFeedback = new DdiConfirmationFeedback(
                DdiConfirmationFeedback.Confirmation.CONFIRMED, 0, Collections.singletonList(
                "the confirmation status for the device is" + DdiConfirmationFeedback.Confirmation.CONFIRMED));
        getDdiApi().postConfirmationActionFeedback(ddiConfirmationFeedback, getTenantId(), getControllerId(), actionId);
    }

    private long getActionId(final Link link) {
        final String href = link.getHref();
        return Long.parseLong(href.substring(href.lastIndexOf('/') + 1, href.indexOf('?')));
    }

    private class UpdateProcessor implements Runnable {

        private static final String BUT_GOT_LOG_MESSAGE = " but got: ";
        private static final String DOWNLOAD_LOG_MESSAGE = "Download ";
        private static final int MINIMUM_TOKENLENGTH_FOR_HINT = 6;

        private final long actionId;
        private final DdiDeployment.HandlingType updateType;
        private final List<DdiChunk> modules;

        private UpdateProcessor(
                final long actionId, final DdiDeployment.HandlingType updateType, final List<DdiChunk> modules) {
            this.actionId = actionId;
            this.updateType = updateType;
            this.modules = modules;
        }

        @Override
        public void run() {
            updateStatus = new UpdateStatus(UpdateStatus.Status.RUNNING, List.of("Update begins!"));
            sendFeedback(actionId);

            if (!CollectionUtils.isEmpty(modules)) {
                updateStatus = download();
                sendFeedback(actionId);
                final UpdateStatus updateStatus = getUpdateStatus();
                if (updateStatus != null && updateStatus.status() == UpdateStatus.Status.ERROR) {
                    currentActionId = null;
                    return;
                }
            }

            if (updateType != DdiDeployment.HandlingType.SKIP) {
                updateStatus = new UpdateStatus(UpdateStatus.Status.SUCCESSFUL, List.of("Update complete!"));
                sendFeedback(actionId);
                currentActionId = null;
            }
        }

        private UpdateStatus download() {
            updateStatus = new UpdateStatus(UpdateStatus.Status.DOWNLOADING,
                    modules.stream().flatMap(mod -> mod.getArtifacts().stream())
                            .map(art -> "Download starts for: " + art.getFilename() + " with SHA1 hash "
                                    + art.getHashes().getSha1() + " and size " + art.getSize())
                            .collect(Collectors.toList()));
            sendFeedback(actionId);

            log.info(LOG_PREFIX + "Start download", getTenantId(), getControllerId());

            final List<UpdateStatus> updateStatusList = new ArrayList<>();
            modules.forEach(module -> module.getArtifacts().forEach(artifact -> {
                if (downloadAuthenticationEnabled) {
                    handleArtifact(getTargetSecurityToken(), gatewayToken, updateStatusList, artifact);
                } else {
                    handleArtifact(null, null, updateStatusList, artifact);
                }
            }));

            log.info(LOG_PREFIX + "Download complete", getTenantId(), getControllerId());

            final List<String> messages = new LinkedList<>();
            messages.add("Download complete!");
            updateStatusList.forEach(download -> messages.addAll(download.messages()));
            return new UpdateStatus(
                    updateStatusList.stream().anyMatch(status -> status.status() == UpdateStatus.Status.ERROR) ?
                        UpdateStatus.Status.ERROR : UpdateStatus.Status.DOWNLOADED,
                    messages);
        }

        private void handleArtifact(
                final String targetToken, final String gatewayToken,
                final List<UpdateStatus> status, final DdiArtifact artifact) {
            artifact.getLink("download").ifPresentOrElse(
                    // HTTPS
                    link -> status.add(downloadUrl(link.getHref(), gatewayToken, targetToken,
                            artifact.getHashes().getSha1(), artifact.getSize()))
                    ,
                    // HTTP
                    () -> status.add(downloadUrl(
                            artifact.getLink("download-http")
                                    .map(Link::getHref)
                                    .orElseThrow(() -> new IllegalArgumentException("Nor https nor http found!")),
                            gatewayToken, targetToken,
                            artifact.getHashes().getSha1(), artifact.getSize()))
            );
        }

        private UpdateStatus downloadUrl(
                final String url, final String gatewayToken, final String targetToken,
                final String sha1Hash, final long size) {
            if (log.isDebugEnabled()) {
                log.debug(LOG_PREFIX + "Downloading {} with token {}, expected sha1 hash {} and size {}", getTenantId(), getControllerId(), url,
                        hideTokenDetails(targetToken), sha1Hash, size);
            }

            try {
                return readAndCheckDownloadUrl(url, gatewayToken, targetToken, sha1Hash, size);
            } catch (IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                log.error(LOG_PREFIX + "Failed to download {}", getTenantId(), getControllerId(), url, e);
                return new UpdateStatus(UpdateStatus.Status.ERROR, List.of("Failed to download " + url + ": " + e.getMessage()));
            }

        }

        private UpdateStatus readAndCheckDownloadUrl(final String url, final String gatewayToken,
                final String targetToken, final String sha1Hash, final long size)
                throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
            long overallread;
            final CloseableHttpClient httpclient = createHttpClientThatAcceptsAllServerCerts();
            final HttpGet request = new HttpGet(url);

            if (StringUtils.hasLength(targetToken)) {
                request.addHeader(HttpHeaders.AUTHORIZATION, "TargetToken " + targetToken);
            } else if (StringUtils.hasLength(gatewayToken)) {
                request.addHeader(HttpHeaders.AUTHORIZATION, "GatewayToken " + gatewayToken);
            }

            final String sha1HashResult;
            try (final CloseableHttpResponse response = httpclient.execute(request)) {

                if (response.getCode() != HttpStatus.OK.value()) {
                    final String message = wrongStatusCode(url, response);
                    return new UpdateStatus(UpdateStatus.Status.ERROR, List.of(message));
                }

                if (response.getEntity().getContentLength() != size) {
                    final String message = wrongContentLength(url, size, response);
                    return new UpdateStatus(UpdateStatus.Status.ERROR, List.of(message));
                }

                // Exception squid:S2070 - not used for hashing sensitive
                // data
                @SuppressWarnings("squid:S2070")
                final MessageDigest md = MessageDigest.getInstance("SHA-1");

                overallread = getOverallRead(response, md);

                if (overallread != size) {
                    final String message = incompleteRead(url, size, overallread);
                    return new UpdateStatus(UpdateStatus.Status.ERROR, List.of(message));
                }

                sha1HashResult = BaseEncoding.base16().lowerCase().encode(md.digest());
            }

            if (!sha1Hash.equalsIgnoreCase(sha1HashResult)) {
                final String message = wrongHash(url, sha1Hash, overallread, sha1HashResult);
                return new UpdateStatus(UpdateStatus.Status.ERROR, List.of(message));
            }

            final String message = "Downloaded " + url + " (" + overallread + " bytes)";
            log.debug(message);
            return new UpdateStatus(UpdateStatus.Status.SUCCESSFUL, List.of(message));
        }

        private static long getOverallRead(final CloseableHttpResponse response, final MessageDigest md)
                throws IOException {

            long overallread;

            try (final OutputStream os = ByteStreams.nullOutputStream();
                    final BufferedOutputStream bos = new BufferedOutputStream(new DigestOutputStream(os, md))) {

                try (BufferedInputStream bis = new BufferedInputStream(response.getEntity().getContent())) {
                    overallread = ByteStreams.copy(bis, bos);
                }
            }

            return overallread;
        }

        private static String hideTokenDetails(final String targetToken) {
            if (targetToken == null) {
                return "<NULL!>";
            }

            if (targetToken.isEmpty()) {
                return "<EMTPTY!>";
            }

            if (targetToken.length() <= MINIMUM_TOKENLENGTH_FOR_HINT) {
                return "***";
            }

            return targetToken.substring(0, 2) + "***"
                    + targetToken.substring(targetToken.length() - 2, targetToken.length());
        }

        private String wrongHash(final String url, final String sha1Hash, final long overallread,
                final String sha1HashResult) {
            final String message = LOG_PREFIX + DOWNLOAD_LOG_MESSAGE + url + " failed with SHA1 hash missmatch (Expected: "
                    + sha1Hash + BUT_GOT_LOG_MESSAGE + sha1HashResult + ") (" + overallread + " bytes)";
            log.error(message, getTenantId(), getControllerId());
            return message;
        }

        private String incompleteRead(final String url, final long size, final long overallread) {
            final String message = LOG_PREFIX + DOWNLOAD_LOG_MESSAGE + url + " is incomplete (Expected: " + size
                    + BUT_GOT_LOG_MESSAGE + overallread + ")";
            log.error(message, getTenantId(), getControllerId());
            return message;
        }

        private String wrongContentLength(final String url, final long size,
                final CloseableHttpResponse response) {
            final String message = LOG_PREFIX + DOWNLOAD_LOG_MESSAGE + url + " has wrong content length (Expected: " + size
                    + BUT_GOT_LOG_MESSAGE + response.getEntity().getContentLength() + ")";
            log.error(message, getTenantId(), getControllerId());
            return message;
        }

        private String wrongStatusCode(final String url, final CloseableHttpResponse response) {
            final String message = LOG_PREFIX + DOWNLOAD_LOG_MESSAGE + url + " failed (" + response.getCode() + ")";
            log.error(message, getTenantId(), getControllerId());
            return message;
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
    }
}
