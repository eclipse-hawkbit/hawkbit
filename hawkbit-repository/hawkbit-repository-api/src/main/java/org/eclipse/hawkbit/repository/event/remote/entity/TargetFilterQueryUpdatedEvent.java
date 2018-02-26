/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.entitiy.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * Defines the remote event for updating a {@link TargetFilterQuery}.
 *
 */
public class TargetFilterQueryUpdatedEvent extends RemoteEntityEvent<TargetFilterQuery> implements EntityUpdatedEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public TargetFilterQueryUpdatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param baseEntity
     *            TargetFilterQuery entity
     * @param applicationId
     *            the origin application id
     */
    public TargetFilterQueryUpdatedEvent(final TargetFilterQuery baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
