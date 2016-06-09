/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import org.eclipse.hawkbit.eventbus.event.AbstractDistributedEvent;

/**
 * Event definition which is been published in case a rollout group has been
 * created for a specific rollout.
 * 
 * @author Michael Hirsch
 *
 */
public class RolloutGroupCreatedEvent extends AbstractDistributedEvent {

    private static final long serialVersionUID = 1L;
    private final Long rolloutId;
    private final Long rolloutGroupId;
    private final int totalRolloutGroup;
    private final int createdRolloutGroup;

    /**
     * Creating a new rollout group created event for a specific rollout.
     * 
     * @param tenant
     *            the tenant of this event
     * @param revision
     *            the revision of the event
     * @param rolloutId
     *            the ID of the rollout the group has been created
     * @param totalRolloutGroup
     *            the total number of rollout groups for this rollout
     * @param createdRolloutGroup
     *            the number of already created groups of the rollout
     */
    public RolloutGroupCreatedEvent(final String tenant, final long revision, final Long rolloutId,
            final Long rolloutGroupId, final int totalRolloutGroup, final int createdRolloutGroup) {
        super(revision, tenant);
        this.rolloutId = rolloutId;
        this.rolloutGroupId = rolloutGroupId;
        this.totalRolloutGroup = totalRolloutGroup;
        this.createdRolloutGroup = createdRolloutGroup;

    }

    public Long getRolloutId() {
        return rolloutId;
    }

    public int getTotalRolloutGroup() {
        return totalRolloutGroup;
    }

    public int getCreatedRolloutGroup() {
        return createdRolloutGroup;
    }

    public Long getRolloutGroupId() {
        return rolloutGroupId;
    }
}
