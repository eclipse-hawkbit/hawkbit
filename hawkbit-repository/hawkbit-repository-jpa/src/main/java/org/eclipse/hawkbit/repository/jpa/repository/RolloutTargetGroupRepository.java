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

import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroupId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring data repository for {@link RolloutTargetGroup}.
 */
@Transactional(readOnly = true)
public interface RolloutTargetGroupRepository
        extends CrudRepository<RolloutTargetGroup, RolloutTargetGroupId>, JpaSpecificationExecutor<RolloutTargetGroup> {

    /**
     * Counts all entries that have the specified rolloutGroup
     *
     * @param rolloutGroup the group to filter for
     * @return count of targets in the group
     */
    Long countByRolloutGroup(JpaRolloutGroup rolloutGroup);
}
