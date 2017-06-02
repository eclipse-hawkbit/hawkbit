/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.hawkbit.dmf.json.model.DmfArtifact;
import org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule;
import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Protocol;
import org.eclipse.hawkbit.simulator.UpdateStatus.ResponseStatus;
import org.eclipse.hawkbit.simulator.amqp.SpSenderService;
import org.eclipse.hawkbit.simulator.event.InitUpdate;
import org.eclipse.hawkbit.simulator.event.ProgressUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.eventbus.EventBus;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;

/**
 * Update simulation handler.
 *
 */
@Service
public class DeviceSimulatorUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceSimulatorUpdater.class);

    @Autowired
    private ScheduledExecutorService threadPool;

    @Autowired
    private SpSenderService spSenderService;

    @Autowired
    private SimulatedDeviceFactory deviceFactory;

    @Autowired
    private EventBus eventbus;

    @Autowired
    private DeviceSimulatorRepository repository;

    /**
     * Starting an simulated update process of an simulated device.
     *
     * @param tenant
     *            the tenant of the device
     * @param id
     *            the ID of the simulated device
     * @param actionId
     *            the actionId from the hawkbit update server to start the
     *            update.
     * @param modules
     *            the software module version from the hawkbit update server
     * @param swVersion
     *            the software version as static value in case modules is null
     * @param targetSecurityToken
     *            the target security token for download authentication
     * @param callback
     *            the callback which gets called when the simulated update
     *            process has been finished
     */
    public void startUpdate(final String tenant, final String id, final long actionId, final String swVersion,
            final List<DmfSoftwareModule> modules, final String targetSecurityToken, final UpdaterCallback callback) {
        AbstractSimulatedDevice device = repository.get(tenant, id);

        // plug and play - non existing device will be auto created
        if (device == null) {
            device = repository
                    .add(deviceFactory.createSimulatedDevice(id, tenant, Protocol.DMF_AMQP, 1800, null, null));
        }

        device.setProgress(0.0);

        if (CollectionUtils.isEmpty(modules)) {
            device.setSwversion(swVersion);
        } else {
            device.setSwversion(
                    modules.stream().map(DmfSoftwareModule::getModuleVersion).collect(Collectors.joining(", ")));
        }
        device.setTargetSecurityToken(targetSecurityToken);
        eventbus.post(new InitUpdate(device));

        threadPool.schedule(new DeviceSimulatorUpdateThread(device, spSenderService, actionId, eventbus, threadPool,
                callback, modules), 2_000, TimeUnit.MILLISECONDS);
    }

    private static final class DeviceSimulatorUpdateThread implements Runnable {

        private static final String BUT_GOT_LOG_MESSAGE = " but got: ";

        private static final String DOWNLOAD_LOG_MESSAGE = "Download ";

        private static final int MINIMUM_TOKENLENGTH_FOR_HINT = 6;

        private static final Random rndSleep = new SecureRandom();

        private final AbstractSimulatedDevice device;
        private final SpSenderService spSenderService;
        private final long actionId;
        private final EventBus eventbus;
        private final ScheduledExecutorService threadPool;
        private final UpdaterCallback callback;
        private final List<DmfSoftwareModule> modules;

        private DeviceSimulatorUpdateThread(final AbstractSimulatedDevice device, final SpSenderService spSenderService,
                final long actionId, final EventBus eventbus, final ScheduledExecutorService threadPool,
                final UpdaterCallback callback, final List<DmfSoftwareModule> modules) {
            this.device = device;
            this.spSenderService = spSenderService;
            this.actionId = actionId;
            this.eventbus = eventbus;
            this.callback = callback;
            this.modules = modules;
            this.threadPool = threadPool;
        }

        @Override
        public void run() {
            if (device.getProgress() <= 0 && modules != null) {
                device.setUpdateStatus(simulateDownloads(device.getTargetSecurityToken()));
                if (isErrorResponse(device.getUpdateStatus())) {
                    device.setProgress(1.0);
                    callback.updateFinished(device, actionId);
                    eventbus.post(new ProgressUpdate(device));
                    return;
                }
                // download is 80% of the game after all
                device.setProgress(0.8);
            }

            final double newProgress = device.getProgress() + 0.2;
            device.setProgress(newProgress);
            if (newProgress < 1.0) {
                threadPool.schedule(new DeviceSimulatorUpdateThread(device, spSenderService, actionId, eventbus,
                        threadPool, callback, modules), rndSleep.nextInt(5_000), TimeUnit.MILLISECONDS);
            } else {
                callback.updateFinished(device, actionId);
            }
            eventbus.post(new ProgressUpdate(device));
        }

        private UpdateStatus simulateDownloads(final String targetToken) {
            final List<UpdateStatus> status = new ArrayList<>();

            LOGGER.info("Simulate downloads for {}", device.getId());

            modules.forEach(
                    module -> module.getArtifacts().forEach(artifact -> handleArtifact(targetToken, status, artifact)));

            final UpdateStatus result = new UpdateStatus(ResponseStatus.SUCCESSFUL);
            result.getStatusMessages().add("Simulation complete!");
            status.forEach(download -> {
                result.getStatusMessages().addAll(download.getStatusMessages());
                if (isErrorResponse(download)) {
                    result.setResponseStatus(ResponseStatus.ERROR);
                }
            });

            LOGGER.info("Download simulations complete for {}", device.getId());

            return result;
        }

        private static boolean isErrorResponse(final UpdateStatus status) {
            if (status == null) {
                return false;
            }

            return ResponseStatus.ERROR.equals(status.getResponseStatus());
        }

        private static void handleArtifact(final String targetToken, final List<UpdateStatus> status,
                final DmfArtifact artifact) {

            if (artifact.getUrls().containsKey("HTTPS")) {
                status.add(downloadUrl(artifact.getUrls().get("HTTPS"), targetToken, artifact.getHashes().getSha1(),
                        artifact.getSize()));
            } else if (artifact.getUrls().containsKey("HTTP")) {
                status.add(downloadUrl(artifact.getUrls().get("HTTP"), targetToken, artifact.getHashes().getSha1(),
                        artifact.getSize()));
            }
        }

        private static UpdateStatus downloadUrl(final String url, final String targetToken, final String sha1Hash,
                final long size) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Downloading {} with token {}, expected sha1 hash {} and size {}", url,
                        hideTokenDetails(targetToken), sha1Hash, size);
            }

            try {
                return readAndCheckDownloadUrl(url, targetToken, sha1Hash, size);
            } catch (IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                LOGGER.error("Failed to download " + url, e);
                return new UpdateStatus(ResponseStatus.ERROR, "Failed to download " + url + ": " + e.getMessage());
            }

        }

        private static UpdateStatus readAndCheckDownloadUrl(final String url, final String targetToken,
                final String sha1Hash, final long size)
                throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
            long overallread;
            final CloseableHttpClient httpclient = createHttpClientThatAcceptsAllServerCerts();
            final HttpGet request = new HttpGet(url);
            request.addHeader(HttpHeaders.AUTHORIZATION, "TargetToken " + targetToken);

            final String sha1HashResult;
            try (final CloseableHttpResponse response = httpclient.execute(request)) {

                if (response.getStatusLine().getStatusCode() != HttpStatus.OK.value()) {
                    final String message = wrongStatusCode(url, response);
                    return new UpdateStatus(ResponseStatus.ERROR, message);
                }

                if (response.getEntity().getContentLength() != size) {
                    final String message = wrongContentLength(url, size, response);
                    return new UpdateStatus(ResponseStatus.ERROR, message);
                }

                // Exception squid:S2070 - not used for hashing sensitive
                // data
                @SuppressWarnings("squid:S2070")
                final MessageDigest md = MessageDigest.getInstance("SHA-1");

                overallread = getOverallRead(response, md);

                if (overallread != size) {
                    final String message = incompleteRead(url, size, overallread);
                    return new UpdateStatus(ResponseStatus.ERROR, message);
                }

                sha1HashResult = BaseEncoding.base16().lowerCase().encode(md.digest());
            }

            if (!sha1Hash.equalsIgnoreCase(sha1HashResult)) {
                final String message = wrongHash(url, sha1Hash, overallread, sha1HashResult);
                return new UpdateStatus(ResponseStatus.ERROR, message);
            }

            final String message = "Downloaded " + url + " (" + overallread + " bytes)";
            LOGGER.debug(message);
            return new UpdateStatus(ResponseStatus.SUCCESSFUL, message);
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

        private static String wrongHash(final String url, final String sha1Hash, final long overallread,
                final String sha1HashResult) {
            final String message = DOWNLOAD_LOG_MESSAGE + url + " failed with SHA1 hash missmatch (Expected: "
                    + sha1Hash + BUT_GOT_LOG_MESSAGE + sha1HashResult + ") (" + overallread + " bytes)";
            LOGGER.error(message);
            return message;
        }

        private static String incompleteRead(final String url, final long size, final long overallread) {
            final String message = DOWNLOAD_LOG_MESSAGE + url + " is incomplete (Expected: " + size
                    + BUT_GOT_LOG_MESSAGE + overallread + ")";
            LOGGER.error(message);
            return message;
        }

        private static String wrongContentLength(final String url, final long size,
                final CloseableHttpResponse response) {
            final String message = DOWNLOAD_LOG_MESSAGE + url + " has wrong content length (Expected: " + size
                    + BUT_GOT_LOG_MESSAGE + response.getEntity().getContentLength() + ")";
            LOGGER.error(message);
            return message;
        }

        private static String wrongStatusCode(final String url, final CloseableHttpResponse response) {
            final String message = DOWNLOAD_LOG_MESSAGE + url + " failed (" + response.getStatusLine().getStatusCode()
                    + ")";
            LOGGER.error(message);
            return message;
        }

        private static CloseableHttpClient createHttpClientThatAcceptsAllServerCerts()
                throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
            final SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, (chain, authType) -> true);
            final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
            return HttpClients.custom().setSSLSocketFactory(sslsf).build();
        }
    }

    /**
     * Callback interface which is called when the simulated update process has
     * been finished and the caller of starting the simulated update process can
     * send the result back to the hawkBit update server.
     */
    @FunctionalInterface
    public interface UpdaterCallback {
        /**
         * Callback method to indicate that the simulated update process has
         * been finished.
         *
         * @param device
         *            the device which has been updated
         * @param actionId
         *            the ID of the action from the hawkbit update server
         */
        void updateFinished(AbstractSimulatedDevice device, final Long actionId);
    }

}
