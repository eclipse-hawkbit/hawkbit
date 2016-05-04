/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.hawkbit.dmf.json.model.Artifact;
import org.eclipse.hawkbit.dmf.json.model.SoftwareModule;
import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Protocol;
import org.eclipse.hawkbit.simulator.UpdateStatus.ResponseStatus;
import org.eclipse.hawkbit.simulator.amqp.SpSenderService;
import org.eclipse.hawkbit.simulator.event.InitUpdate;
import org.eclipse.hawkbit.simulator.event.ProgressUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    private static final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(4);

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
            final List<SoftwareModule> modules, final String targetSecurityToken, final UpdaterCallback callback) {
        AbstractSimulatedDevice device = repository.get(tenant, id);

        // plug and play - non existing device will be auto created
        if (device == null) {
            device = repository.add(deviceFactory.createSimulatedDevice(id, tenant, Protocol.DMF_AMQP, -1, null, null));
        }

        device.setProgress(0.0);

        if (modules == null || modules.isEmpty()) {
            device.setSwversion(swVersion);
        } else {
            device.setSwversion(modules.stream().map(sm -> sm.getModuleVersion()).collect(Collectors.joining(", ")));
        }
        device.setTargetSecurityToken(targetSecurityToken);
        eventbus.post(new InitUpdate(device));

        threadPool.schedule(
                new DeviceSimulatorUpdateThread(device, spSenderService, actionId, eventbus, callback, modules), 2_000,
                TimeUnit.MILLISECONDS);
    }

    private static final class DeviceSimulatorUpdateThread implements Runnable {
        private static final Random rndSleep = new SecureRandom();

        private final AbstractSimulatedDevice device;
        private final SpSenderService spSenderService;
        private final long actionId;
        private final EventBus eventbus;
        private final UpdaterCallback callback;
        private final List<SoftwareModule> modules;

        private DeviceSimulatorUpdateThread(final AbstractSimulatedDevice device, final SpSenderService spSenderService,
                final long actionId, final EventBus eventbus, final UpdaterCallback callback,
                final List<SoftwareModule> modules) {
            this.device = device;
            this.spSenderService = spSenderService;
            this.actionId = actionId;
            this.eventbus = eventbus;
            this.callback = callback;
            this.modules = modules;
        }

        @Override
        public void run() {
            if (device.getProgress() <= 0 && modules != null) {
                device.setUpdateStatus(simulateDownloads(device.getTargetSecurityToken()));
                if (device.getUpdateStatus().getResponseStatus().equals(ResponseStatus.ERROR)) {
                    callback.updateFinished(device, actionId);
                    eventbus.post(new ProgressUpdate(device));
                    return;
                }
            }

            final double newProgress = device.getProgress() + 0.2;
            device.setProgress(newProgress);
            if (newProgress < 1.0) {
                threadPool.schedule(
                        new DeviceSimulatorUpdateThread(device, spSenderService, actionId, eventbus, callback, modules),
                        rndSleep.nextInt(5_000), TimeUnit.MILLISECONDS);
            } else {
                callback.updateFinished(device, actionId);
            }
            eventbus.post(new ProgressUpdate(device));
        }

        private UpdateStatus simulateDownloads(final String targetToken) {
            final List<UpdateStatus> status = new ArrayList<>();

            LOGGER.info("Simulate downloads for {}", device.getId());

            modules.forEach(module -> module.getArtifacts()
                    .forEach(artifact -> handleArtifacts(targetToken, status, artifact)));

            final UpdateStatus result = new UpdateStatus(ResponseStatus.SUCCESSFUL);
            result.getStatusMessages().add("Simulation complete!");
            status.forEach(download -> {
                result.getStatusMessages().addAll(download.getStatusMessages());
                if (download.getResponseStatus().equals(ResponseStatus.ERROR)) {
                    result.setResponseStatus(ResponseStatus.ERROR);
                }
            });

            LOGGER.info("Download simulations complete for {}", device.getId());

            return result;
        }

        private static void handleArtifacts(final String targetToken, final List<UpdateStatus> status,
                final Artifact artifact) {
            artifact.getUrls().entrySet().forEach(entry -> {
                switch (entry.getKey()) {
                case HTTP:
                case HTTPS:
                    status.add(downloadUrl(entry.getValue(), targetToken, artifact.getHashes().getSha1()));
                    break;
                default:
                    // not supported yet
                    break;
                }
            });
        }

        private static UpdateStatus downloadUrl(final String url, final String targetToken, final String sha1Hash) {
            LOGGER.debug("Downloading " + url);

            long overallread = 0;
            try {
                final CloseableHttpClient httpclient = createHttpClientThatAcceptsAllServerCerts();
                final HttpGet request = new HttpGet(url);
                request.addHeader("TargetToken", targetToken);

                final String sha1HashResult;
                try (final CloseableHttpResponse response = httpclient.execute(request)) {
                    final File tempFile = File.createTempFile("uploadFile", null);
                    final MessageDigest md = MessageDigest.getInstance("SHA-1");

                    try (final DigestOutputStream dos = new DigestOutputStream(new FileOutputStream(tempFile), md)) {
                        overallread = ByteStreams.copy(response.getEntity().getContent(), dos);
                        sha1HashResult = BaseEncoding.base16().lowerCase().encode(md.digest());
                    } finally {
                        tempFile.delete();
                    }
                }

                if (!sha1Hash.equals(sha1HashResult)) {
                    final String message = "Download " + url + " failed with SHA1 hash missmatch (Expected: " + sha1Hash
                            + " but got: " + sha1HashResult + ")";
                    LOGGER.debug(message);
                    return new UpdateStatus(ResponseStatus.ERROR, message);
                }

            } catch (IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                LOGGER.error("Failed to download {}", url, e);
                return new UpdateStatus(ResponseStatus.ERROR, "Failed to download " + url + ": " + e.getMessage());
            }

            final String message = "Downloaded " + url + " (" + overallread + " bytes)";
            LOGGER.debug(message);
            return new UpdateStatus(ResponseStatus.SUCCESSFUL, message);
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
     * send the result to the hawkBit update server back. *
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
