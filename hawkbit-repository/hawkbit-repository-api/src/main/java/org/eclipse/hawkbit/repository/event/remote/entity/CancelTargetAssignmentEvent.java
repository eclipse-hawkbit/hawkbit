/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.Target;

/**
 * Event that gets sent when the assignment of a distribution set to a target
 * gets canceled.
 */
public class CancelTargetAssignmentEvent extends RemoteEntityEvent<Target> {

    private static final long serialVersionUID = 1L;

    private final Long actionId;

    /**
     * Constructor for json serialization.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param actionId
     *            the actionId
     * @param entityClass
     *            the entity entityClassName
     * @param applicationId
     *            the origin application id
     */
    public CancelTargetAssignmentEvent(final String tenant, final Long entityId, final Long actionId,
            final String entityClass, final String applicationId) {
        super(tenant, entityId, entityClass, applicationId);
        this.actionId = actionId;
    }

    /**
     * Constructor.
     * 
     * @param baseEntity
     *            the target
     * @param actionId
     *            the actionId
     * @param applicationId
     *            the origin application id
     */
    public CancelTargetAssignmentEvent(final Target baseEntity, final Long actionId, final String applicationId) {
        super(baseEntity, applicationId);
        this.actionId = actionId;
    }

    /**
     * @return the action id of the assignment
     */
    public Long getActionId() {
        return actionId;
    }

}
