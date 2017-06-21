/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.mgmt.json.model.systemmanagement.MgmtSystemCache;
import org.eclipse.hawkbit.mgmt.json.model.systemmanagement.MgmtSystemStatisticsRest;
import org.eclipse.hawkbit.mgmt.json.model.systemmanagement.MgmtSystemTenantServiceUsage;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSystemManagementRestApi;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.report.model.SystemUsageReportWithTenants;
import org.eclipse.hawkbit.repository.report.model.TenantUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link SystemManagement} capabilities by REST.
 *
 */
@RestController
public class MgmtSystemManagementResource implements MgmtSystemManagementRestApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(MgmtSystemManagementResource.class);

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
        return ResponseEntity.ok().build();
    }

    /**
     * Collects and returns system usage statistics. It provides a system wide
     * overview and tenant based stats.
     *
     * @return system usage statistics
     */
    @Override
    public ResponseEntity<MgmtSystemStatisticsRest> getSystemUsageStats() {
        final SystemUsageReportWithTenants report = systemManagement.getSystemUsageStatisticsWithTenants();

        final MgmtSystemStatisticsRest result = new MgmtSystemStatisticsRest()
                .setOverallActions(report.getOverallActions()).setOverallArtifacts(report.getOverallArtifacts())
                .setOverallArtifactVolumeInBytes(report.getOverallArtifactVolumeInBytes())
                .setOverallTargets(report.getOverallTargets()).setOverallTenants(report.getTenants().size());

        result.setTenantStats(report.getTenants().stream().map(MgmtSystemManagementResource::convertTenant)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(result);
    }

    private static MgmtSystemTenantServiceUsage convertTenant(final TenantUsage tenant) {
        final MgmtSystemTenantServiceUsage result = new MgmtSystemTenantServiceUsage(tenant.getTenantName());
        result.setActions(tenant.getActions());
        result.setArtifacts(tenant.getArtifacts());
        result.setOverallArtifactVolumeInBytes(tenant.getOverallArtifactVolumeInBytes());
        result.setTargets(tenant.getTargets());
        if (!tenant.getUsageData().isEmpty()) {
            result.setUsageData(tenant.getUsageData());
        }
        return result;
    }

    /**
     * Returns a list of all caches.
     *
     * @return a list of caches for all tenants
     */
    @Override
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    public ResponseEntity<Collection<MgmtSystemCache>> getCaches() {
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

    private MgmtSystemCache cacheRest(final Cache cache) {
        final Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof com.google.common.cache.Cache) {
            return guavaCache(cache, nativeCache);
        } else {
            return new MgmtSystemCache(cache.getName(), Collections.emptyList());
        }
    }

    @SuppressWarnings("unchecked")
    private MgmtSystemCache guavaCache(final Cache cache, final Object nativeCache) {
        final com.google.common.cache.Cache<Object, Object> guavaCache = (com.google.common.cache.Cache<Object, Object>) nativeCache;
        final List<String> keys = guavaCache.asMap().keySet().stream().map(key -> key.toString())
                .collect(Collectors.toList());
        return new MgmtSystemCache(cache.getName(), keys);
    }
}
