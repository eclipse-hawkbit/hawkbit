/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.report.model.SystemUsageReport;
import org.eclipse.hawkbit.repository.report.model.SystemUsageReportWithTenants;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Central system management operations of the update server.
 *
 */
public interface SystemManagement {

    /**
     * Checks if a specific tenant exists. The tenant will not be created lazy.
     *
     * @return {@code true} in case the tenant exits or {@code false} if not
     */
    String currentTenant();

    /**
     * Deletes all data related to a given tenant.
     *
     * @param tenant
     *            to delete
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    void deleteTenant(@NotNull String tenant);

    /**
     *
     * @param pageable
     *            for paging information
     * @return list of all tenant names in the system.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    Page<String> findTenants(@NotNull Pageable pageable);

    /**
     * Runs consumer for each teant as
     * {@link TenantAware#runAsTenant(String, org.eclipse.hawkbit.tenancy.TenantAware.TenantRunner)}
     * sliently (i.e. exceptions will be logged but operations will continue for
     * further tenants).
     * 
     * @param consumer
     *            to run as teanant
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    void forEachTenant(Consumer<String> consumer);

    /**
     * Calculated system usage statistics, both overall for the entire system
     * and per tenant;
     *
     * @return SystemUsageReport of the current system
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    SystemUsageReportWithTenants getSystemUsageStatisticsWithTenants();

    /**
     * Calculated overall system usage statistics
     *
     * @return SystemUsageReport of the current system
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    SystemUsageReport getSystemUsageStatistics();

    /**
     * @return {@link TenantMetaData} of {@link TenantAware#getCurrentTenant()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_READ_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    TenantMetaData getTenantMetadata();

    /**
     * Returns {@link TenantMetaData} of given and current tenant. Creates for
     * new tenants also two {@link SoftwareModuleType} (os and app) and
     * {@link RepositoryConstants#DEFAULT_DS_TYPES_IN_TENANT}
     * {@link DistributionSetType}s (os and os_app).
     *
     * DISCLAIMER: this variant is used during initial login (where the tenant
     * is not yet in the session). Please user {@link #getTenantMetadata()} for
     * regular requests.
     *
     * @param tenant
     *            to retrieve data for
     * @return {@link TenantMetaData} of given tenant
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    TenantMetaData getTenantMetadata(@NotNull String tenant);

    /**
     * Update call for {@link TenantMetaData} of the current tenant.
     *
     * @param defaultDsType
     *            to update
     * @return updated {@link TenantMetaData} entity
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION)
    TenantMetaData updateTenantMetadata(long defaultDsType);

    /**
     * Returns {@link TenantMetaData} of given tenant ID.
     *
     * @param tenantId
     *            to retrieve data for
     * @return {@link TenantMetaData} of given tenant
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    TenantMetaData getTenantMetadata(long tenantId);

}
