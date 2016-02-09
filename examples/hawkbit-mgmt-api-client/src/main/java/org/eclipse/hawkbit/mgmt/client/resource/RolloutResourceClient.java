/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.client.resource;

import org.eclipse.hawkbit.rest.resource.api.RolloutRestApi;
import org.springframework.cloud.netflix.feign.FeignClient;

/**
 * Client binding for the Rollout resource of the management API.
 */
@FeignClient(url = "${hawkbit.endpoint.url:localhost:8080}/rest/v1/rollouts")
public interface RolloutResourceClient extends RolloutRestApi {

}
