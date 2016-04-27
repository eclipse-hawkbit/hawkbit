/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.client;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.hawkbit.ddi.client.authenctication.AuthenticationInterceptor;
import org.eclipse.hawkbit.ddi.client.resource.RootControllerResourceClient;
import org.eclipse.hawkbit.ddi.client.strategy.ArtifactsPersistenceStrategy;
import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeploymentBase;
import org.eclipse.hawkbit.ddi.json.model.DdiResult;
import org.eclipse.hawkbit.ddi.json.model.DdiResult.FinalResult;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus.ExecutionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * DDI example client based on defualt DDI feign client.
 */
public class DdiExampleClient implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DdiExampleClient.class);

    private final String controllerId;
    private Long actionIdOfLastInstalltion;
    private final RootControllerResourceClient rootControllerResourceClient;
    private final ArtifactsPersistenceStrategy persistenceStrategy;
    private DdiClientStatus clientStatus;

    private FinalResult finalResultOfCurrentUpdate;

    /**
     * Constructor for the DDI example client.
     * 
     * @param baseUrl
     *            the base url of the hawkBit server
     * @param controllerId
     *            the controller id that will be simulated
     * @param tenant
     *            the tenant
     * @param persistenceStrategy
     *            the persistence strategy for downloading artifacts
     */
    public DdiExampleClient(final String baseUrl, final String controllerId, final String tenant,
            final ArtifactsPersistenceStrategy persistenceStrategy) {
        this(baseUrl, controllerId, tenant, persistenceStrategy, null);
    }

    /**
     * Constructor for the DDI example client.
     * 
     * @param baseUrl
     *            the base url of the hawkBit server
     * @param controllerId
     *            the controller id that will be simulated
     * @param tenant
     *            the tenant
     * @param persistenceStrategy
     *            the persistence strategy for downloading artifacts
     * @param authenticationInterceptor
     *            the authentication intercepter to authenticate the DDI client,
     *            might be {@code null}
     */
    public DdiExampleClient(final String baseUrl, final String controllerId, final String tenant,
            final ArtifactsPersistenceStrategy persistenceStrategy, final AuthenticationInterceptor authenticationInterceptor) {
        this.controllerId = controllerId;
        this.rootControllerResourceClient = new DdiDefaultFeignClient(baseUrl, tenant, authenticationInterceptor)
                .getRootControllerResourceClient();
        this.actionIdOfLastInstalltion = null;
        this.persistenceStrategy = persistenceStrategy;
        this.clientStatus = DdiClientStatus.DOWN;
    }

    @Override
    public void run() {
        clientStatus = DdiClientStatus.UP;
        ResponseEntity<DdiControllerBase> response;
        while (clientStatus == DdiClientStatus.UP) {
            LOGGER.info(" Controller {} polling from hawkBit server", controllerId);
            response = rootControllerResourceClient.getControllerBase(controllerId);
            final String pollingTimeFormReponse = response.getBody().getConfig().getPolling().getSleep();
            final LocalTime localtime = LocalTime.parse(pollingTimeFormReponse);
            final long pollingIntervalInMillis = localtime.getLong(ChronoField.MILLI_OF_DAY);
            final Link controllerDeploymentBaseLink = response.getBody().getLink("deploymentBase");
            if (controllerDeploymentBaseLink != null) {
                final Long actionId = getActionIdOutOfLink(controllerDeploymentBaseLink);
                final Integer resource = getResourceOutOfLink(controllerDeploymentBaseLink);
                if (actionId != actionIdOfLastInstalltion) {
                    finalResultOfCurrentUpdate = FinalResult.NONE;
                    startDownload(actionId, resource);
                    finishUpdateProcess(actionId);
                    actionIdOfLastInstalltion = actionId;
                }
            }
            try {
                Thread.sleep(pollingIntervalInMillis);
            } catch (final InterruptedException e) {
                LOGGER.error("Error during sleep");
            }
        }
    }

    /**
     * Stop the DDI example client
     */
    public void stop() {
        clientStatus = DdiClientStatus.DOWN;
    }

    private void startDownload(final Long actionId, final Integer resource) {
        final ResponseEntity<DdiDeploymentBase> respone = rootControllerResourceClient
                .getControllerBasedeploymentAction(controllerId, Long.valueOf(actionId), Integer.valueOf(resource));
        final DdiDeploymentBase ddiDeploymentBase = respone.getBody();
        final List<DdiChunk> chunks = ddiDeploymentBase.getDeployment().getChunks();
        for (final DdiChunk chunk : chunks) {
            final List<DdiArtifact> artifactList = chunk.getArtifacts();
            if (artifactList.isEmpty()) {
                sendFeedBackMessage(actionId, ExecutionStatus.PROCEEDING, FinalResult.NONE,
                        "No artifacts to download for softwaremodule " + chunk.getName());
            } else {
                for (final DdiArtifact ddiArtifact : artifactList) {
                    if (finalResultOfCurrentUpdate != FinalResult.FAILURE) {
                        downloadArtifact(actionId, ddiArtifact);
                    }
                }
            }

        }
    }

    private void downloadArtifact(final Long actionId, final DdiArtifact ddiArtifact) {

        final String artifact = ddiArtifact.getFilename();
        final Link downloadLink = ddiArtifact.getLink("download-http");
        final String[] downloadLinkSep = downloadLink.getHref().split(Pattern.quote("/"));
        final Long softwareModuleId = Long.valueOf(downloadLinkSep[8]);

        sendFeedBackMessage(actionId, ExecutionStatus.PROCEEDING, FinalResult.NONE,
                "Starting download of artifact " + artifact);
        LOGGER.info("Starting download of artifact " + artifact);

        final ResponseEntity<InputStream> responseDownloadArtifact = rootControllerResourceClient
                .downloadArtifact(controllerId, softwareModuleId, artifact);
        final HttpStatus statsuCode = responseDownloadArtifact.getStatusCode();
        LOGGER.info("Finished download with stataus {}", statsuCode);

        try {
            persistenceStrategy.handleInputStream(responseDownloadArtifact.getBody(), artifact);
            sendFeedBackMessage(actionId, ExecutionStatus.PROCEEDING, FinalResult.NONE,
                    "Downloaded artifact " + artifact);
        } catch (final IOException e) {
            sendFeedBackMessage(actionId, ExecutionStatus.PROCEEDING, FinalResult.NONE,
                    "Downloaded of artifact " + artifact + "failed");
            finalResultOfCurrentUpdate = FinalResult.FAILURE;
        }

    }

    private void sendFeedBackMessage(final Long actionId, final ExecutionStatus executionStatus,
            final FinalResult finalResult, final String message) {
        final DdiResult result = new DdiResult(finalResult, null);
        final List<String> details = new ArrayList<>();
        details.add(message);
        final DdiStatus ddiStatus = new DdiStatus(executionStatus, result, details);
        final String time = String.valueOf(LocalDateTime.now());
        final DdiActionFeedback feedback = new DdiActionFeedback(actionId, time, ddiStatus);
        rootControllerResourceClient.postBasedeploymentActionFeedback(feedback, controllerId, actionId);
        LOGGER.info("Sent feedback message to HaktBit");
    }

    private void finishUpdateProcess(final Long actionId) {

        if (finalResultOfCurrentUpdate == FinalResult.FAILURE) {
            sendFeedBackMessage(actionId, ExecutionStatus.CLOSED, FinalResult.FAILURE, "Error during update process");
        }

        if (finalResultOfCurrentUpdate == FinalResult.NONE) {
            sendFeedBackMessage(actionId, ExecutionStatus.CLOSED, FinalResult.SUCESS,
                    "Simulated installation successful");
        }

    }

    private Long getActionIdOutOfLink(final Link controllerDeploymentBaseLink) {
        final String[] ending = splitControllerDeploymentBaseLinkInActionIdAndResource(controllerDeploymentBaseLink);
        return Long.valueOf(ending[0]);
    }

    private Integer getResourceOutOfLink(final Link controllerDeploymentBaseLink) {
        final String[] ending = splitControllerDeploymentBaseLinkInActionIdAndResource(controllerDeploymentBaseLink);
        return Integer.valueOf(ending[1].substring(2));
    }

    private String[] splitControllerDeploymentBaseLinkInActionIdAndResource(final Link controllerDeploymentBaseLink) {
        final String link = controllerDeploymentBaseLink.getHref();
        final String[] segments = link.split(Pattern.quote("/"));
        return segments[8].split(Pattern.quote("?"));
    }

    /**
     * Enum for DDI running status.
     */
    public enum DdiClientStatus {
        UP, DOWN;
    }

}
