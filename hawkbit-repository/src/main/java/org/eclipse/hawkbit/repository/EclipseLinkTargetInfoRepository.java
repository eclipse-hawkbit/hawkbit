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

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom repository implementation as standard spring repository fails as of
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=415027 .
 *
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public class EclipseLinkTargetInfoRepository implements TargetInfoRepository {

    @Autowired
    private EntityManager entityManager;

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void setTargetUpdateStatus(final TargetUpdateStatus status, final List<Long> targets) {
        final Query query = entityManager.createQuery(
                "update TargetInfo ti set ti.updateStatus = :status where ti.targetId in :targets and ti.updateStatus != :status");
        query.setParameter("targets", targets);
        query.setParameter("status", status);

    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @CacheEvict(value = { "targetStatus", "distributionUsageInstalled", "targetsLastPoll" }, allEntries = true)
    public <S extends TargetInfo> S save(final S entity) {

        if (entity.isNew()) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @CacheEvict(value = { "targetStatus", "distributionUsageInstalled", "targetsLastPoll" }, allEntries = true)
    public void deleteByTargetIdIn(final Collection<Long> targetIDs) {
        final javax.persistence.Query query = entityManager
                .createQuery("DELETE FROM TargetInfo ti where ti.targetId IN :target");
        query.setParameter("target", targetIDs);

    }

}
