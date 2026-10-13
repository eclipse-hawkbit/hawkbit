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

import java.time.Instant;
import java.util.function.Consumer;

import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Central system management operations of the update server.
 */
public interface SystemManagement {

    /**
     * Runs consumer for each tenant, except the soft deleted ones, as
     * {@link AccessContext#asSystemAsTenant(String, Runnable)}
     * silently (i.e. exceptions will be logged but operations will continue for further tenants). Same as
     * {@link #forEachTenantAsSystem(boolean, Consumer)} with <code>includeSoftDeleted = false</code>.
     *
     * @param consumer to run as tenant
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    void forEachTenantAsSystem(Consumer<String> consumer);

    /**
     * Runs consumer for each tenant as
     * {@link AccessContext#asSystemAsTenant(String, Runnable)}
     * silently (i.e. exceptions will be logged but operations will continue for further tenants).
     *
     * @param includeSoftDeleted <code>true</code> to run also for the soft deleted tenants (e.g. for cleanups), <code>false</code>
     *         to skip them (e.g. for the regular per tenant processing)
     * @param consumer to run as tenant
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    void forEachTenantAsSystem(boolean includeSoftDeleted, Consumer<String> consumer);

    /**
     * @return {@link TenantMetaData} of {@link AccessContext#tenant()}
     */
    @PreAuthorize("hasAuthority('" + SpPermission.READ_DISTRIBUTION_SET + "')" + " or " + "hasAuthority('READ_" + SpPermission.TARGET + "')" + " or " + "hasAuthority('READ_" + SpPermission.TENANT_CONFIGURATION + "')" + " or " + SpringEvalExpressions.IS_CONTROLLER)
    TenantMetaData getTenantMetadata();

    /**
     * @return {@link TenantMetaData} of {@link AccessContext#tenant()} without details ({@link TenantMetaData#getDefaultDsType()})
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
     * Checks if the current tenant ({@link AccessContext#tenant()}) is soft deleted, i.e. its soft delete time has passed. Unknown
     * tenants are reported as not soft deleted.
     * <p/>
     * The check is cached per tenant (tenant aware cache, default TTL 10s). A soft delete or restore evicts the cache only on the
     * node doing it - there it takes effect immediately, on other nodes after at most the cache TTL.
     *
     * @return <code>true</code> if the current tenant is soft deleted, <code>false</code> otherwise
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    boolean isTenantSoftDeleted();

    /**
     * Deletes immediately (hard delete) all data related to a given tenant (also if it is soft deleted).
     *
     * @param tenant to delete
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    void deleteTenant(@NotNull String tenant);

    /**
     * Soft deletes a given tenant (see {@link #softDeleteTenant(String, Instant, Instant)}) with the default pre-configured soft
     * delete time <code>now + </code>{@link RepositoryProperties#getTenantSoftDeleteAfter()} (or <code>now</code> if it is not
     * positive) and hard delete time <code>now + </code>{@link RepositoryProperties#getTenantHardDeleteAfter()} - both are counted
     * from now (the request time). Soft deletes are disabled if the hard delete after is not positive.
     *
     * @param tenant to be soft deleted
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException if the tenant doesn't exist
     * @throws IllegalStateException if soft deletes are disabled ({@link RepositoryProperties#getTenantHardDeleteAfter()} is not
     *         positive) or the tenant is already soft deleted
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    void softDeleteTenant(@NotNull String tenant);

    /**
     * Soft deletes a tenant at the given time. The tenant stays usable until the soft delete time, from then on it is inaccessible
     * through all APIs, but its data is kept until it is hard deleted at the given hard delete time or until it is restored.
     * <p/>
     * A pending (not yet effective) soft delete is rescheduled by a new call - the soft delete time, the deleting actor and the
     * hard delete time are replaced. A call for an already soft deleted tenant fails and keeps its soft delete marks unchanged.
     *
     * @param tenant to be soft deleted
     * @param softDeleteAt when the tenant becomes soft deleted, a time in the past means immediately
     * @param hardDeleteAt when the tenant shall be hard deleted, must not be before <code>softDeleteAt</code>
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException if the tenant doesn't exist
     * @throws IllegalArgumentException if <code>hardDeleteAt</code> is before <code>softDeleteAt</code>
     * @throws IllegalStateException if the tenant is already soft deleted
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    void softDeleteTenant(@NotNull String tenant, @NotNull Instant softDeleteAt, @NotNull Instant hardDeleteAt);

    /**
     * Restores a soft deleted (not yet hard deleted) tenant - clears its soft delete marks, so it becomes accessible again with
     * all its data. Also cancels a pending (not yet effective) soft delete. For an active tenant it does nothing.
     *
     * @param tenant to restore
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException if the tenant doesn't exist
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    void restoreTenant(@NotNull String tenant);
}
