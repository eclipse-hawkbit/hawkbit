/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ddi.client.resource;

import org.eclipse.hawkbit.ddi.api.RootControllerDdiApi;
import org.springframework.cloud.netflix.feign.FeignClient;

/**
 * Client binding for the Rootcontroller resource of the DDI API.
 */
@FeignClient(url = "${hawkbit.url:localhost:8080}/{tenant}/controller/v1")
public interface RootControllerResourceClient extends RootControllerDdiApi {

}
