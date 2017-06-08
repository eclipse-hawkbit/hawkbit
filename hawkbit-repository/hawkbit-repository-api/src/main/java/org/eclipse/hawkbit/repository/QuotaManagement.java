/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Central service for defined limits of the repository.
 *
 */
public interface QuotaManagement {

    /**
     * @return Maximum number of {@link ActionStatus} entries that the
     *         controller can report for an {@link Action}.
     */
    int getMaxStatusEntriesPerAction();

    /**
     * @return maximum number of attributes that the controller can report;
     */
    int getMaxAttributeEntriesPerTarget();

    /**
     * @return maximum number of allowed {@link RolloutGroup}s per
     *         {@link Rollout}.
     */
    int getMaxRolloutGroupsPerRollout();

    /**
     * @return maximum number of
     *         {@link ControllerManagement#getActionHistoryMessages(Long, int)}
     *         for an individual {@link ActionStatus}.
     */
    int getMaxMessagesPerActionStatus();

}
