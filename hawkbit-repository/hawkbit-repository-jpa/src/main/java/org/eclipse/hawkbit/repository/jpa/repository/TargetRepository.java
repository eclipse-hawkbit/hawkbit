/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import java.util.Collection;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link Target} repository.
 */
@Transactional(readOnly = true)
public interface TargetRepository extends BaseEntityRepository<JpaTarget> {

    default Optional<JpaTarget> findByControllerId(final String controllerId) {
        return findOne(TargetSpecifications.hasControllerId(controllerId));
    }

    default JpaTarget getByControllerId(final String controllerId) {
        return findOne(TargetSpecifications.hasControllerId(controllerId))
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    // TODO AC - remove it and use specification

    /**
     * @deprecated remove it and use specification
     */
    // no access check
    @Deprecated(forRemoval = true)
    @Modifying
    @Transactional
    @Query("UPDATE JpaTarget t SET t.assignedDistributionSet = :set, t.lastModifiedAt = :lastModifiedAt, t.lastModifiedBy = :lastModifiedBy, t.updateStatus = :status WHERE t.id IN :targets")
    void setAssignedDistributionSetAndUpdateStatus(@Param("status") TargetUpdateStatus status,
            @Param("set") JpaDistributionSet set, @Param("lastModifiedAt") Long modifiedAt,
            @Param("lastModifiedBy") String modifiedBy, @Param("targets") Collection<Long> targets);

    // TODO AC - remove it and use specification

    /**
     * @deprecated will be removed
     */
    // no access check
    @Deprecated(forRemoval = true)
    @Modifying
    @Transactional
    @Query("UPDATE JpaTarget t SET t.assignedDistributionSet = :set, t.installedDistributionSet = :set, t.installationDate = :lastModifiedAt, t.lastModifiedAt = :lastModifiedAt, t.lastModifiedBy = :lastModifiedBy, t.updateStatus = :status WHERE t.id IN :targets")
    void setAssignedAndInstalledDistributionSetAndUpdateStatus(@Param("status") TargetUpdateStatus status,
            @Param("set") JpaDistributionSet set, @Param("lastModifiedAt") Long modifiedAt,
            @Param("lastModifiedBy") String modifiedBy, @Param("targets") Collection<Long> targets);

    /**
     * Counts {@link Target} instances of given type in the repository.
     * <p/>
     * No access control applied
     *
     * @param targetTypeId to search for
     * @return number of found {@link Target}s
     */
    long countByTargetTypeId(Long targetTypeId);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will by done by {@link EntityManager} anyhow.
     * The DB should take care of optimizing this away.
     *
     * @param tenant to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaTarget t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}
