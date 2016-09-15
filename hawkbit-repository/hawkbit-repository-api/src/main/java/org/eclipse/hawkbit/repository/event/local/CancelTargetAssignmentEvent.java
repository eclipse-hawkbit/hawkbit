/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.local;

import java.net.URI;

/**
 * Event that gets sent when the assignment of a distribution set to a target
 * gets canceled.
 *
 *
 *
 */
public class CancelTargetAssignmentEvent extends DefaultEvent {

    private final String controllerId;
    private final Long actionId;
    private final URI targetAdress;

    /**
     * Creates a new {@link CancelTargetAssignmentEvent}.
     *
     * @param revision
     *            the revision for this event
     * @param tenant
     *            the tenant for this event
     * @param controllerId
     *            the ID of the controller
     * @param actionId
     *            the action id of the assignment
     * @param targetAdress
     *            the targetAdress of the target
     */
    public CancelTargetAssignmentEvent(final long revision, final String tenant, final String controllerId,
            final Long actionId, final URI targetAdress) {
        super(revision, tenant);
        this.controllerId = controllerId;
        this.actionId = actionId;
        this.targetAdress = targetAdress;
    }

    /**
     * @return the action id of the assignment
     */
    public Long getActionId() {
        return actionId;
    }

    /**
     * @return the controllerId of the Target which has been assigned to the
     *         distribution set
     */
    public String getControllerId() {
        return controllerId;
    }

    /**
     *
     * @return the targetr adress.
     */
    public URI getTargetAdress() {
        return targetAdress;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "TargetAssignDistributionSetEvent [targetAdress=" + targetAdress + ", controllerId=" + controllerId
                + ", actionId=" + actionId + "]";
    }
}
