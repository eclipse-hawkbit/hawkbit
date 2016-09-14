/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client.resource;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.springframework.cloud.netflix.feign.FeignClient;

/**
 * Client binding for the SoftwareModuleType resource of the management API.
 */
@FeignClient(name = "MgmtSoftwareModuleTypeClient", url = "${hawkbit.url:localhost:8080}")
public interface MgmtSoftwareModuleTypeClientResource extends MgmtSoftwareModuleTypeRestApi {
}
