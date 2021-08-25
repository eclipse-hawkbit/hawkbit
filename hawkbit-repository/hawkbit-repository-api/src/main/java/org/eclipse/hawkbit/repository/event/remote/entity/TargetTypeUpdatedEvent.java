/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.entity.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.model.TargetType;

/**
 * Defines the remote event for updating a {@link TargetType}.
 *
 */
public class TargetTypeUpdatedEvent extends RemoteEntityEvent<TargetType>
        implements EntityUpdatedEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public TargetTypeUpdatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     *
     * @param baseEntity
     *            TargetType
     * @param applicationId
     *            the origin application id
     */
    public TargetTypeUpdatedEvent(final TargetType baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
