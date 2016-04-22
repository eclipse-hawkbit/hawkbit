/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ddi.client.resource;

import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;
import org.springframework.cloud.netflix.feign.FeignClient;

/**
 * Client binding for the Rootcontroller resource of the DDI API.
 */
@FeignClient(url = "${hawkbit.url:localhost:8080}/" + RootControllerResourceClient.PATH)
public interface RootControllerResourceClient extends DdiRootControllerRestApi {

    static String PATH = "{tenant}/controller/v1";

}
