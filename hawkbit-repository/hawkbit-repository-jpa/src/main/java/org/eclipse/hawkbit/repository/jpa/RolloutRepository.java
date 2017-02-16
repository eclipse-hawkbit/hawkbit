/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The repository interface for the {@link Rollout} model.
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public interface RolloutRepository
        extends BaseEntityRepository<JpaRollout, Long>, JpaSpecificationExecutor<JpaRollout> {

    /**
     * Updates the {@code lastCheck} field of the {@link Rollout} for rollouts
     * in a specific status and only if the {@code lastCheck} is overdue.
     * 
     * @param lastCheck
     *            the time in milliseconds to set to the lastCheck column
     * @param delay
     *            the delay between last checks
     * @param status
     *            the status which the rollout should have to update the last
     *            check field
     * @return the count of the updated rows. Zero if no row has been updated
     */
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    @Query("UPDATE JpaRollout r SET r.lastCheck = :lastCheck WHERE r.lastCheck < (:lastCheck - :delay) AND r.status=:status")
    int updateLastCheck(@Param("lastCheck") final long lastCheck, @Param("delay") final long delay,
            @Param("status") final RolloutStatus status);

    /**
     * Retrieves all {@link Rollout} for a specific {@code lastCheck} time and
     * for a specific status.
     * 
     * @param lastCheck
     *            the lastCheck time to find the specific rollout.
     * @param status
     *            the status of the rollout to find
     * @return the list of {@link Rollout} for specific lastCheck time and
     *         status
     */
    List<JpaRollout> findByLastCheckAndStatus(long lastCheck, RolloutStatus status);

    /**
     * Retrieves all {@link Rollout} for a specific {@code name}
     * 
     * @param name
     *            the rollout name
     * @return {@link Rollout} for specific name
     */
    Optional<Rollout> findByName(String name);
}
