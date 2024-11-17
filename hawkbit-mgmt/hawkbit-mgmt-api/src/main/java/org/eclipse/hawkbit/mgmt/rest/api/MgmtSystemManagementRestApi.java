/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.Collection;

import org.eclipse.hawkbit.mgmt.json.model.systemmanagement.MgmtSystemCache;
import org.eclipse.hawkbit.mgmt.json.model.systemmanagement.MgmtSystemStatisticsRest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * System management capabilities by REST.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
public interface MgmtSystemManagementRestApi {

    /**
     * Deletes the tenant data of a given tenant. USE WITH CARE!
     *
     * @param tenant to delete
     * @return HttpStatus.OK
     */
    @DeleteMapping(value = MgmtRestConstants.SYSTEM_ADMIN_MAPPING + "/tenants/{tenant}")
    ResponseEntity<Void> deleteTenant(@PathVariable("tenant") String tenant);

    /**
     * Collects and returns system usage statistics. It provides a system wide
     * overview and tenant based stats.
     *
     * @return system usage statistics
     */
    @GetMapping(value = MgmtRestConstants.SYSTEM_ADMIN_MAPPING + "/usage",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSystemStatisticsRest> getSystemUsageStats();

    /**
     * Returns a list of all caches.
     *
     * @return a list of caches for all tenants
     */
    @GetMapping(value = MgmtRestConstants.SYSTEM_ADMIN_MAPPING + "/caches",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Collection<MgmtSystemCache>> getCaches();

    /**
     * Invalidates all caches for all tenants.
     *
     * @return a list of cache names which has been invalidated
     */
    @DeleteMapping(value = MgmtRestConstants.SYSTEM_ADMIN_MAPPING + "/caches")
    ResponseEntity<Collection<String>> invalidateCaches();
}
