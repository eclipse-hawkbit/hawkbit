/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.function.Consumer;

import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.context.System;
import org.eclipse.hawkbit.context.Tenant;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Central system management operations of the update server.
 */
public interface SystemManagement {

    /**
     * @param pageable for paging information
     * @return list of all tenant names in the system.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    Page<String> findTenants(@NotNull Pageable pageable);

    /**
     * Runs consumer for each tenant as
     * {@link System#asSystemAsTenant(String, java.util.concurrent.Callable)}
     * silently (i.e. exceptions will be logged but operations will continue for further tenants).
     *
     * @param consumer to run as tenant
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    void forEachTenant(Consumer<String> consumer);

    /**
     * @return {@link TenantMetaData} of {@link Tenant#currentTenant()}
     */
    @PreAuthorize("hasAuthority('" + SpPermission.READ_DISTRIBUTION_SET + "')" + " or " + "hasAuthority('READ_" + SpPermission.TARGET + "')" + " or " + "hasAuthority('READ_" + SpPermission.TENANT_CONFIGURATION + "')" + " or " + SpringEvalExpressions.IS_CONTROLLER)
    TenantMetaData getTenantMetadata();

    /**
     * @return {@link TenantMetaData} of {@link Tenant#currentTenant()} without details ({@link TenantMetaData#getDefaultDsType()})
     */
    @PreAuthorize("hasAuthority('" + SpPermission.READ_DISTRIBUTION_SET + "')" + " or " + "hasAuthority('READ_" + SpPermission.TARGET + "')" + " or " + "hasAuthority('READ_" + SpPermission.TENANT_CONFIGURATION + "')" + " or " + SpringEvalExpressions.IS_CONTROLLER)
    TenantMetaData getTenantMetadataWithoutDetails();

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
     * @param tenant to retrieve data for
     * @return {@link TenantMetaData} of given tenant
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    TenantMetaData createTenantMetadata(@NotNull String tenant);

    /**
     * Update call for {@link TenantMetaData} of the current tenant.
     *
     * @param defaultDsType to update
     * @return updated {@link TenantMetaData} entity
     */
    @PreAuthorize("hasAuthority('UPDATE_" + SpPermission.TENANT_CONFIGURATION + "')")
    TenantMetaData updateTenantMetadata(long defaultDsType);

    /**
     * Deletes all data related to a given tenant.
     *
     * @param tenant to delete
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    void deleteTenant(@NotNull String tenant);
}