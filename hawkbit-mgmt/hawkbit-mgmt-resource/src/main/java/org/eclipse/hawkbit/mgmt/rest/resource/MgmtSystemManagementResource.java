/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.mgmt.json.model.systemmanagement.MgmtSystemCache;
import org.eclipse.hawkbit.mgmt.json.model.systemmanagement.MgmtSystemStatisticsRest;
import org.eclipse.hawkbit.mgmt.json.model.systemmanagement.MgmtSystemTenantServiceUsage;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSystemManagementRestApi;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.report.model.SystemUsageReportWithTenants;
import org.eclipse.hawkbit.repository.report.model.TenantUsage;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link SystemManagement} capabilities by REST.
 */
@Slf4j
@RestController
public class MgmtSystemManagementResource implements MgmtSystemManagementRestApi {

    private final SystemManagement systemManagement;
    private final CacheManager cacheManager;

    MgmtSystemManagementResource(final SystemManagement systemManagement, final CacheManager cacheManager) {
        this.systemManagement = systemManagement;
        this.cacheManager = cacheManager;
    }

    /**
     * Deletes the tenant data of a given tenant. USE WITH CARE!
     *
     * @param tenant to delete
     * @return HttpStatus.OK
     */
    @Override
    public ResponseEntity<Void> deleteTenant(final String tenant) {
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

        result.setTenantStats(report.getTenants().stream().map(MgmtSystemManagementResource::convertTenant).toList());

        return ResponseEntity.ok(result);
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
                .ok(cacheNames.stream().map(cacheManager::getCache)
                        .filter(Objects::nonNull)
                        .map(cache -> new MgmtSystemCache(cache.getName(), Collections.emptyList()))
                        .toList());
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
        log.info("Invalidating caches {}", cacheNames);
        cacheNames.forEach(cacheName -> cacheManager.getCache(cacheName).clear());
        return ResponseEntity.ok(cacheNames);
    }

    private static MgmtSystemTenantServiceUsage convertTenant(final TenantUsage tenant) {
        final MgmtSystemTenantServiceUsage result = new MgmtSystemTenantServiceUsage();
        result.setTenantName(tenant.getTenantName());
        result.setActions(tenant.getActions());
        result.setArtifacts(tenant.getArtifacts());
        result.setOverallArtifactVolumeInBytes(tenant.getOverallArtifactVolumeInBytes());
        result.setTargets(tenant.getTargets());
        if (!tenant.getUsageData().isEmpty()) {
            result.setUsageData(tenant.getUsageData());
        }
        return result;
    }
}