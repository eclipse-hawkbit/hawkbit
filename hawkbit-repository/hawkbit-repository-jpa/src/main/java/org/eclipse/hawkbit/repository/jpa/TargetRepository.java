/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link Target} repository.
 *
 */
@Transactional(readOnly = true)
public interface TargetRepository extends BaseEntityRepository<JpaTarget, Long>, JpaSpecificationExecutor<JpaTarget> {
    /**
     * Sets {@link JpaTarget#getAssignedDistributionSet()}.
     *
     * @param set
     *            to use
     * @param status
     *            to set
     * @param modifiedAt
     *            current time
     * @param modifiedBy
     *            current auditor
     * @param targets
     *            to update
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaTarget t SET t.assignedDistributionSet = :set, t.lastModifiedAt = :lastModifiedAt, t.lastModifiedBy = :lastModifiedBy, t.updateStatus = :status WHERE t.id IN :targets")
    void setAssignedDistributionSetAndUpdateStatus(@Param("status") TargetUpdateStatus status,
            @Param("set") JpaDistributionSet set, @Param("lastModifiedAt") Long modifiedAt,
            @Param("lastModifiedBy") String modifiedBy, @Param("targets") Collection<Long> targets);

    /**
     * Sets {@link JpaTarget#getAssignedDistributionSet()},
     * {@link JpaTarget#getInstalledDistributionSet()} and
     * {@link JpaTarget#getInstallationDate()}
     *
     * @param set
     *            to use
     * @param status
     *            to set
     * @param modifiedAt
     *            current time
     * @param modifiedBy
     *            current auditor
     * @param targets
     *            to update
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaTarget t SET t.assignedDistributionSet = :set, t.installedDistributionSet = :set, t.installationDate = :lastModifiedAt, t.lastModifiedAt = :lastModifiedAt, t.lastModifiedBy = :lastModifiedBy, t.updateStatus = :status WHERE t.id IN :targets")
    void setAssignedAndInstalledDistributionSetAndUpdateStatus(@Param("status") TargetUpdateStatus status,
            @Param("set") JpaDistributionSet set, @Param("lastModifiedAt") Long modifiedAt,
            @Param("lastModifiedBy") String modifiedBy, @Param("targets") Collection<Long> targets);

    /**
     * Deletes the {@link Target}s with the given target IDs.
     *
     * @param targetIDs
     *            to be deleted
     */
    @Modifying
    @Transactional
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("DELETE FROM JpaTarget t WHERE t.id IN ?1")
    void deleteByIdIn(Collection<Long> targetIDs);

    /**
     * Finds all {@link Target}s in the repository.
     *
     * Calls version with (empty) spec to allow injecting further specs
     *
     * @return {@link List} of {@link Target}s
     */
    @Override
    @NonNull
    default List<JpaTarget> findAll() {
        return this.findAll(Specification.where(null));
    }

    /**
     * Finds all {@link Target}s in the repository sorted.
     *
     * Calls version with (empty) spec to allow injecting further specs
     *
     * @param sort instructions to sort result by
     * @return {@link List} of {@link Target}s
     */
    @Override
    @NonNull
    default Iterable<JpaTarget> findAll(@NonNull Sort sort) {
        return this.findAll(Specification.where(null), sort);
    }

    /**
     * Finds a page of {@link Target}s in the repository.
     *
     * Calls version with (empty) spec to allow injecting further specs
     *
     * @param pageable paging context
     * @return {@link List} of {@link Target}s
     */
    @Override
    @NonNull
    default Page<JpaTarget> findAll(@NonNull Pageable pageable) {
        return this.findAll(Specification.where(null), pageable);
    }

    /**
     * Finds {@link Target}s in the repository matching a list of ids.
     *
     * Calls version based on spec to allow injecting further specs
     *
     * @param ids ids to filter for
     * @return {@link List} of {@link Target}s
     */
    @Override
    @NonNull
    default List<JpaTarget> findAllById(Iterable<Long> ids) {
        final List<Long> collectedIds = StreamSupport.stream(ids.spliterator(), true).collect(Collectors.toList());
        return this.findAll(Specification.where(TargetSpecifications.hasIdIn(collectedIds)));
    }

    /**
     * Finds {@link Target} in the repository matching an id.
     *
     * Calls version based on spec to allow injecting further specs
     *
     * @param id id to filter for
     * @return {@link Optional} of {@link Target}
     */
    @Override
    @NonNull
    default Optional<JpaTarget> findById(@NonNull Long id) {
        return this.findOne(Specification.where(TargetSpecifications.hasId(id)));
    }

    /**
     * Checks whether {@link Target} in the repository matching an id exists or not.
     *
     * Calls version based on spec to allow injecting further specs
     *
     * @param id id to check for
     * @return true if target with id exists
     */
    @Override
    default boolean existsById(@NonNull Long id) {
        return this.exists(TargetSpecifications.hasId(id));
    }

    /**
     * Checks whether {@link Target} in the repository matching a spec exists or not.
     *
     * @param spec to check for existence
     * @return true if target with id exists
     */
    default boolean exists(@NonNull Specification<JpaTarget> spec) {
        return this.count(spec) > 0;
    }

    /**
     * Count number of {@link Target}s in the repository.
     *
     * Calls version with an empty spec to allow injecting further specs
     *
     * @return number of targets in the repository
     */
    @Override
    default long count() {
        return this.count(Specification.where(null));
    }

    /**
     * Counts {@link Target} instances of given type in the repository.
     *
     * @param targetTypeId
     *            to search for
     * @return number of found {@link Target}s
     */
    long countByTargetTypeId(Long targetTypeId);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will by done by {@link EntityManager} anyhow.
     * The DB should take care of optimizing this away.
     *
     * @param tenant
     *            to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaTarget t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}
