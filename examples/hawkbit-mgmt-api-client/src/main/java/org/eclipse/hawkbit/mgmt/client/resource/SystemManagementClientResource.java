/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client.resource;

import org.eclipse.hawkbit.system.rest.api.SystemManagementRestApi;
import org.springframework.cloud.netflix.feign.FeignClient;

/**
 * Client binding for the {@link SystemManagementRestApi}.
 *
 */
@FeignClient(url = "${hawkbit.url:localhost:8080}/" + SystemManagementClientResource.PATH)
public interface SystemManagementClientResource extends SystemManagementRestApi {
    static String PATH = "system/admin";

}
