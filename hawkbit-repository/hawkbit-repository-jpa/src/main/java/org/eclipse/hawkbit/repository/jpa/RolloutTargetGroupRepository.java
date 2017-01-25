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

import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroupId;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring data repository for {@link RolloutTargetGroup}.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public interface RolloutTargetGroupRepository
        extends CrudRepository<RolloutTargetGroup, RolloutTargetGroupId>, JpaSpecificationExecutor<RolloutTargetGroup> {

    /**
     * Counts all entries that have the specified rolloutGroup
     * 
     * @param rolloutGroup
     *            the group to filter for
     * @return count of targets in the group
     */
    Long countByRolloutGroup(final JpaRolloutGroup rolloutGroup);

    /**
     * Deletes all {@link RolloutTargetGroupId} by the given
     * {@link RolloutGroup} IDs.
     * 
     * @param rolloutGroupIds
     *            the IDs of the rollout groups to delete the
     *            {@link RolloutTargetGroup}
     */
    @Query("DELETE FROM RolloutTargetGroup g WHERE g.rolloutGroup IN :rolloutGroupIds")
    @Modifying
    void deleteByRolloutGroupIds(@Param("rolloutGroupIds") final Collection<Long> rolloutGroupIds);
}
