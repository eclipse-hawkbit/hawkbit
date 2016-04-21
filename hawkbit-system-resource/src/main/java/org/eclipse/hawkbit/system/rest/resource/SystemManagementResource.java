/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.system.rest.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.report.model.SystemUsageReport;
import org.eclipse.hawkbit.report.model.TenantUsage;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.system.json.model.systemmanagement.SystemCache;
import org.eclipse.hawkbit.system.json.model.systemmanagement.SystemStatisticsRest;
import org.eclipse.hawkbit.system.json.model.systemmanagement.SystemTenantServiceUsage;
import org.eclipse.hawkbit.system.rest.api.SystemManagementRestApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link SystemManagement} capabilities by REST.
 *
 */
@RestController
public class SystemManagementResource implements SystemManagementRestApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemManagementResource.class);

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private CacheManager cacheManager;

    /**
     * Deletes the tenant data of a given tenant. USE WITH CARE!
     *
     * @param tenant
     *            to delete
     * @return HttpStatus.OK
     */
    @Override
    public ResponseEntity<Void> deleteTenant(@PathVariable("tenant") final String tenant) {
        systemManagement.deleteTenant(tenant);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Collects and returns system usage statistics. It provides a system wide
     * overview and tenant based stats.
     *
     * @return system usage statistics
     */
    @Override
    public ResponseEntity<SystemStatisticsRest> getSystemUsageStats() {
        final SystemUsageReport report = systemManagement.getSystemUsageStatistics();

        final SystemStatisticsRest result = new SystemStatisticsRest().setOverallActions(report.getOverallActions())
                .setOverallArtifacts(report.getOverallArtifacts())
                .setOverallArtifactVolumeInBytes(report.getOverallArtifactVolumeInBytes())
                .setOverallTargets(report.getOverallTargets()).setOverallTenants(report.getTenants().size());

        result.setTenantStats(
                report.getTenants().stream().map(SystemManagementResource::convertTenant).collect(Collectors.toList()));

        return ResponseEntity.ok(result);
    }

    private static SystemTenantServiceUsage convertTenant(final TenantUsage tenant) {
        final SystemTenantServiceUsage result = new SystemTenantServiceUsage(tenant.getTenantName());
        result.setActions(tenant.getActions());
        result.setArtifacts(tenant.getArtifacts());
        result.setOverallArtifactVolumeInBytes(tenant.getOverallArtifactVolumeInBytes());
        result.setTargets(tenant.getTargets());

        return result;
    }

    /**
     * Returns a list of all caches.
     *
     * @return a list of caches for all tenants
     */
    @Override
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    public ResponseEntity<Collection<SystemCache>> getCaches() {
        final Collection<String> cacheNames = cacheManager.getCacheNames();
        return ResponseEntity
                .ok(cacheNames.stream().map(cacheManager::getCache).map(this::cacheRest).collect(Collectors.toList()));
    }

    /**
     * Invalidates all caches for all tenants.
     *
     * @return a list of cache names which has been invalidated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    @Override
    public ResponseEntity<Collection<String>> invalidateCaches() {
        final Collection<String> cacheNames = cacheManager.getCacheNames();
        LOGGER.info("Invalidating caches {}", cacheNames);
        cacheNames.forEach(cacheName -> cacheManager.getCache(cacheName).clear());
        return ResponseEntity.ok(cacheNames);
    }

    private SystemCache cacheRest(final Cache cache) {
        final Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof com.google.common.cache.Cache) {
            return guavaCache(cache, nativeCache);
        } else {
            return new SystemCache(cache.getName(), Collections.emptyList());
        }
    }

    @SuppressWarnings("unchecked")
    private SystemCache guavaCache(final Cache cache, final Object nativeCache) {
        final com.google.common.cache.Cache<Object, Object> guavaCache = (com.google.common.cache.Cache<Object, Object>) nativeCache;
        final List<String> keys = guavaCache.asMap().keySet().stream().map(key -> key.toString())
                .collect(Collectors.toList());
        return new SystemCache(cache.getName(), keys);
    }
}
