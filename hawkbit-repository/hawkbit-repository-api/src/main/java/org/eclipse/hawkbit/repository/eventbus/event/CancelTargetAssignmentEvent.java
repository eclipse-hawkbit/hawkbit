/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import org.eclipse.hawkbit.eventbus.event.DefaultEvent;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Event that gets sent when the assignment of a distribution set to a target
 * gets canceled.
 *
 *
 *
 */
public class CancelTargetAssignmentEvent extends DefaultEvent {

    private final Target target;
    private final Long actionId;

    /**
     * Creates a new {@link CancelTargetAssignmentEvent}.
     *
     * @param revision
     *            the revision for this event
     * @param tenant
     *            the tenant for this event
     * @param target
     *            entity
     * @param actionId
     *            the action id of the assignment
     */
    public CancelTargetAssignmentEvent(final long revision, final String tenant, final Target target,
            final Long actionId) {
        super(revision, tenant);
        this.target = target;
        this.actionId = actionId;
    }

    /**
     * @return the action id of the assignment
     */
    public Long getActionId() {
        return actionId;
    }

    /**
     * @return target where the action got canceled
     */
    public Target getTarget() {
        return target;
    }

}
