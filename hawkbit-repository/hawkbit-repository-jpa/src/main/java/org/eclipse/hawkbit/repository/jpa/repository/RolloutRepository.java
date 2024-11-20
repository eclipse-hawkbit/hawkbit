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
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * The repository interface for the {@link Rollout} model.
 */
@Transactional(readOnly = true)
public interface RolloutRepository
        extends BaseEntityRepository<JpaRollout> {

    /**
     * Retrieves all {@link Rollout} for given status.
     *
     * @param status the status of the rollouts to find
     * @return the list of {@link Rollout} for specific status
     */
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("SELECT sm.id FROM JpaRollout sm WHERE sm.status IN ?1")
    List<Long> findByStatusIn(Collection<RolloutStatus> status);

    /**
     * Retrieves all {@link Rollout} for a specific {@code name}
     *
     * @param name the rollout name
     * @return {@link Rollout} for specific name
     */
    Optional<Rollout> findByName(String name);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will by done by {@link EntityManager}
     * anyhow. The DB should take care of optimizing this away.
     *
     * @param tenant to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaRollout t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    /**
     * Retrieves all {@link Rollout}s for a specific {@link DistributionSet} in
     * a given {@link RolloutStatus}.
     *
     * @param set the distribution set
     * @param status the status of the rollout
     * @return {@link Rollout} for specific distribution set
     */
    List<Rollout> findByDistributionSetAndStatusIn(DistributionSet set, Collection<RolloutStatus> status);

    /**
     * Counts all {@link Rollout}s for a specific {@link DistributionSet} in a
     * given {@link RolloutStatus}.
     *
     * @param distributionSetId the distribution set
     * @param status the status of the rollout
     * @return the count
     */
    long countByDistributionSetIdAndStatusIn(long distributionSetId, Collection<RolloutStatus> status);
}
