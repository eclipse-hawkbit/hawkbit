/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ddi.client;

import org.springframework.stereotype.Component;

@Component
public class DdiExampleClient {

    private final String controllerId;
    private final String name;
    private final String description;
    private final String baseUrl;
    final DdiDefaultFeignClient ddiDefaultFeignClient;

    public DdiExampleClient(final String baseUrl, final String controllerId, final String name,
            final String description, final String tenant) {
        super();
        this.controllerId = controllerId;
        this.name = name;
        this.description = description;
        this.baseUrl = baseUrl;
        ddiDefaultFeignClient = new DdiDefaultFeignClient(baseUrl, tenant);
    }

    public void startDdiClient() {

        // final ResponseEntity<DdiControllerBase> response =

        ddiDefaultFeignClient.getRootControllerResourceClient().getControllerBase(controllerId);

        // final DdiControllerBase controllerBase = response.getBody();

        System.out.println("test");

        // TODO notify every 10 seconds on the rollout server

        // TODO if new update available -> start download and installation
        // process
        // report status messages

    }

}
