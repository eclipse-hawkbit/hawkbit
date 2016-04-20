/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.system.rest.api;

import java.util.Collection;

import org.eclipse.hawkbit.system.json.model.systemmanagement.SystemCache;
import org.eclipse.hawkbit.system.json.model.systemmanagement.SystemStatisticsRest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * {@link SystemManagement} capabilities by REST.
 *
 */
@RequestMapping(SystemRestConstant.SYSTEM_ADMIN_MAPPING)
public interface SystemManagementRestApi {

    /**
     * Deletes the tenant data of a given tenant. USE WITH CARE!
     *
     * @param tenant
     *            to delete
     * @return HttpStatus.OK
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/tenants/{tenant}")
    ResponseEntity<Void> deleteTenant(@PathVariable final String tenant);

    /**
     * Collects and returns system usage statistics. It provides a system wide
     * overview and tenant based stats.
     *
     * @return system usage statistics
     */
    @RequestMapping(method = RequestMethod.GET, value = "/usage", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<SystemStatisticsRest> getSystemUsageStats();

    /**
     * Returns a list of all caches.
     *
     * @return a list of caches for all tenants
     */
    @RequestMapping(method = RequestMethod.GET, value = "/caches")
    ResponseEntity<Collection<SystemCache>> getCaches();

    /**
     * Invalidates all caches for all tenants.
     *
     * @return a list of cache names which has been invalidated
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/caches")
    ResponseEntity<Collection<String>> invalidateCaches();

}
