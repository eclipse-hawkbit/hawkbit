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

import javax.persistence.Entity;

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Usually a JPA spring data repository to handle {@link TargetInfo} entity.
 * However, do to an eclipselink bug with spring boot now a regular interface
 * that is implemented by {@link EclipseLinkTargetInfoRepository}.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
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
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Query("update JpaTargetInfo ti set ti.updateStatus = :status where ti.targetId in :targets and ti.updateStatus != :status")
    void setTargetUpdateStatus(@Param("status") TargetUpdateStatus status, @Param("targets") List<Long> targets);

    /**
     * Save entity and evict cache with it.
     *
     * @param entity
     *            to persists
     *
     * @return persisted or updated {@link Entity}
     */
    <S extends JpaTargetInfo> S save(S entity);
}
