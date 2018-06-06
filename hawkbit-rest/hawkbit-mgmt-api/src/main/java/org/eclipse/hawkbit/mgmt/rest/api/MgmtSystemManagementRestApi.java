/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.Collection;

import org.eclipse.hawkbit.mgmt.json.model.systemmanagement.MgmtSystemCache;
import org.eclipse.hawkbit.mgmt.json.model.systemmanagement.MgmtSystemStatisticsRest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * System management capabilities by REST.
 *
 */
@RequestMapping(MgmtRestConstants.SYSTEM_ADMIN_MAPPING)
public interface MgmtSystemManagementRestApi {

    /**
     * Deletes the tenant data of a given tenant. USE WITH CARE!
     *
     * @param tenant
     *            to delete
     * @return HttpStatus.OK
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/tenants/{tenant}")
    ResponseEntity<Void> deleteTenant(@PathVariable("tenant") String tenant);

    /**
     * Collects and returns system usage statistics. It provides a system wide
     * overview and tenant based stats.
     *
     * @return system usage statistics
     */
    @RequestMapping(method = RequestMethod.GET, value = "/usage", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSystemStatisticsRest> getSystemUsageStats();

    /**
     * Returns a list of all caches.
     *
     * @return a list of caches for all tenants
     */
    @RequestMapping(method = RequestMethod.GET, value = "/caches", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Collection<MgmtSystemCache>> getCaches();

    /**
     * Invalidates all caches for all tenants.
     *
     * @return a list of cache names which has been invalidated
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/caches")
    ResponseEntity<Collection<String>> invalidateCaches();

}
