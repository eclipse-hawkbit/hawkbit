/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.hawkbit.ddi.client.strategy.PersistenceStrategy;
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

public class DdiExampleClient implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DdiExampleClient.class);

    private final String controllerId;
    private Long actionIdOfLastInstalltion;
    private final DdiDefaultFeignClient ddiDefaultFeignClient;
    private final long pollingIntervalInMillis;
    private final PersistenceStrategy persistenceStrategy;

    public DdiExampleClient(final String baseUrl, final String controllerId, final String tenant,
            final long pollingIntervalInMillis, final PersistenceStrategy persistenceStrategy) {
        this.controllerId = controllerId;
        this.ddiDefaultFeignClient = new DdiDefaultFeignClient(baseUrl, tenant);
        this.actionIdOfLastInstalltion = null;
        this.pollingIntervalInMillis = pollingIntervalInMillis;
        this.persistenceStrategy = persistenceStrategy;
    }

    @Override
    public void run() {

        ResponseEntity<DdiControllerBase> response;

        while (!Thread.currentThread().isInterrupted()) {
            response = ddiDefaultFeignClient.getRootControllerResourceClient().getControllerBase(controllerId);
            final DdiControllerBase controllerBase = response.getBody();
            final Link controllerDeploymentBaseLink = controllerBase.getLink("deploymentBase");

            if (controllerDeploymentBaseLink != null) {
                final Long actionId = getActionIdOutOfLink(controllerDeploymentBaseLink);
                final Integer resource = getResourceOutOfLink(controllerDeploymentBaseLink);
                if (actionId != actionIdOfLastInstalltion) {

                    startDownload(actionId, resource);

                    simulateSuccessfulInstallation(actionId);

                    actionIdOfLastInstalltion = actionId;
                }
            }

            try {
                Thread.sleep(pollingIntervalInMillis);
                System.out.println("polling ...");
            } catch (final InterruptedException e) {
                LOGGER.error("Error during sleep");
            }
        }

    }

    private void startDownload(final Long actionId, final Integer resource) {

        // resource has not been downloaded and installed
        final ResponseEntity<DdiDeploymentBase> respone = ddiDefaultFeignClient.getRootControllerResourceClient()
                .getControllerBasedeploymentAction(controllerId, Long.valueOf(actionId), Integer.valueOf(resource));
        final DdiDeploymentBase ddiDeploymentBase = respone.getBody();
        final List<DdiChunk> chunks = ddiDeploymentBase.getDeployment().getChunks();
        for (final DdiChunk chunk : chunks) {
            final List<DdiArtifact> artifactList = chunk.getArtifacts();
            final Link downloadLink = ddiDeploymentBase.getDeployment().getChunks().get(0).getArtifacts().get(0)
                    .getLink("download-http");
            final String[] downloadLinkSep = downloadLink.getHref().split(Pattern.quote("/"));
            final Long softwareModuleId = Long.valueOf(downloadLinkSep[8]);
            // download all artifacts
            for (final DdiArtifact ddiArtifact : artifactList) {
                downloadArtifact(actionId, softwareModuleId, ddiArtifact.getFilename());
            }
        }
    }

    private void downloadArtifact(final Long actionId, final Long softwareModuleId, final String artifact) {

        sendFeedBackMessage(actionId, ExecutionStatus.PROCEEDING, FinalResult.NONE,
                "Starting download of artifact " + artifact);
        System.out.println("Starting download for artifact " + artifact);

        final ResponseEntity<InputStream> responseDownloadArtifact = ddiDefaultFeignClient
                .getRootControllerResourceClient().downloadArtifact(controllerId, softwareModuleId, artifact);
        final HttpStatus statsuCode = responseDownloadArtifact.getStatusCode();
        System.out.println("Finished download with stataus " + statsuCode);
        persistenceStrategy.handleInputStream(responseDownloadArtifact.getBody(), artifact);

        sendFeedBackMessage(actionId, ExecutionStatus.PROCEEDING, FinalResult.NONE, "Downloaded artifact " + artifact);
    }

    private void sendFeedBackMessage(final Long actionId, final ExecutionStatus executionStatus,
            final FinalResult finalResult, final String message) {

        final DdiResult result = new DdiResult(finalResult, null);
        final List<String> details = new ArrayList<>();
        details.add(message);
        final DdiStatus ddiStatus = new DdiStatus(executionStatus, result, details);
        final String time = null;
        final DdiActionFeedback feedback = new DdiActionFeedback(actionId, time, ddiStatus);
        final ResponseEntity<Void> response = ddiDefaultFeignClient.getRootControllerResourceClient()
                .postBasedeploymentActionFeedback(feedback, controllerId, actionId);

        final HttpStatus statsuCode = response.getStatusCode();
        System.out.println("Message send with stataus " + statsuCode);
    }

    private void simulateSuccessfulInstallation(final Long actionId) {
        sendFeedBackMessage(actionId, ExecutionStatus.PROCEEDING, FinalResult.SUCESS,
                "Simulated installation successful");
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

    // private RootControllerResourceClient getDownloadFeignClient() {
    //
    // final Builder feignBuilder = Feign.builder().contract(new
    // IgnoreMultipleConsumersProducersSpringMvcContract())
    // .requestInterceptor(new
    // ApplicationJsonRequestHeaderInterceptor()).logLevel(Level.FULL)
    // .logger(new Logger.ErrorLogger()).encoder(new
    // JacksonEncoder()).decoder(new Decoder() {
    // @Override
    // public Object decode(final Response response, final Type type)
    // throws IOException, DecodeException, FeignException {
    //
    // // TODO download
    // final InputStream stream = response.body().asInputStream();
    //
    // final FileSystem local = FileSystems.getDefault();
    //
    // System.out.println("Status is " + response.status());
    //
    // final ResponseEntity<Void> test = new ResponseEntity<Void>(
    // HttpStatus.valueOf(response.status()));
    //
    // return test;
    // }
    // });
    //
    // final RootControllerResourceClient rootControllerResourceClient =
    // feignBuilder
    // .target(RootControllerResourceClient.class,
    // "http://localhost:8080/DEFAULT/controller/v1");
    //
    // return rootControllerResourceClient;
    //
    // }

}
