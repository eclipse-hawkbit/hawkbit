/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ddi.client;

import java.util.regex.Pattern;

import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeploymentBase;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;

//@Component
public class DdiExampleClient implements Runnable {

    private final String controllerId;

    final DdiDefaultFeignClient ddiDefaultFeignClient;

    public DdiExampleClient(final String baseUrl, final String controllerId, final String tenant) {
        super();
        this.controllerId = controllerId;
        ddiDefaultFeignClient = new DdiDefaultFeignClient(baseUrl, tenant);
    }

    @Override
    public void run() {

        ResponseEntity<DdiControllerBase> response;

        for (int i = 0; i < 20; i++) {

            response = ddiDefaultFeignClient.getRootControllerResourceClient().getControllerBase(controllerId);
            final DdiControllerBase controllerBase = response.getBody();
            final Link controllerDeploymentBaseLink = controllerBase.getLink("deploymentBase");

            if (controllerDeploymentBaseLink != null) {
                // TOD actung download nur einmal starten
                startDownload(controllerDeploymentBaseLink);
            }

            try {
                Thread.sleep(2000);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public void startDdiClient() {

        //

        // TODO notify every 10 seconds on the rollout server

        // TODO if new update available -> start download and installation
        // process
        // report status messages

    }

    private void startDownload(final Link controllerDeploymentBaseLink) {

        // controllerDeploymentBaseLink.

        // final List<String> varibles = controllerDeploymentBaseLink.get

        final String link = controllerDeploymentBaseLink.getHref();
        final String[] segs = link.split(Pattern.quote("/"));
        final String[] ending = segs[8].split(Pattern.quote("?"));
        final String actionId = ending[0];
        final String resource = ending[1].substring(2);

        final ResponseEntity<DdiDeploymentBase> respone = ddiDefaultFeignClient.getRootControllerResourceClient()
                .getControllerBasedeploymentAction(controllerId, Long.valueOf(actionId), Integer.valueOf(resource));

        final DdiDeploymentBase ddiDeploymentBase = respone.getBody();

        final Link downloadLink = ddiDeploymentBase.getDeployment().getChunks().get(0).getArtifacts().get(0)
                .getLink("download");

        System.out.println("download startet ....");

    }

    private void startSimulatedInstalltion() {

    }

}
