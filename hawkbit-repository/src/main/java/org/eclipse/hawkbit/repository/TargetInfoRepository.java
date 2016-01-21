/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;

import javax.persistence.Entity;
import javax.transaction.Transactional;

import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Usually a JPA spring data repository to handle {@link TargetInfo} entity.
 * However, do to an eclipselink bug with spring boot now a regular interface
 * that is implemented by {@link EclipseLinkTargetInfoRepository}.
 *
 *
 *
 */
public interface TargetInfoRepository {

    /**
     * Sets new TargetUpdateStatus of given target if is not already on that
     * value.
     *
     * @param status
     *            to set
     * @param targets
     *            to set it for
     */
    @Modifying
    @Transactional
    @Query("update TargetInfo ti set ti.updateStatus = :status where ti.targetId in :targets and ti.updateStatus != :status")
    void setTargetUpdateStatus(@Param("status") TargetUpdateStatus status, @Param("targets") List<Long> targets);

    /**
     * Save entity and evict cache with it.
     *
     * @param entity
     *            to persists
     *
     * @return persisted or updated {@link Entity}
     */
    @CacheEvict(value = { "targetStatus", "distributionUsageInstalled", "targetsLastPoll" }, allEntries = true)
    <S extends TargetInfo> S save(S entity);

    /**
     * Deletes info entries by ID.
     *
     * @param targetIDs
     *            to delete
     */
    @Modifying
    @Transactional
    @CacheEvict(value = { "targetStatus", "distributionUsageInstalled", "targetsLastPoll" }, allEntries = true)
    void deleteByTargetIdIn(final Collection<Long> targetIDs);
}
