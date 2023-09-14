/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.entity.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.TargetType;

/**
 * Defines the remote event of creating a new {@link TargetType}.
 *
 */
public class TargetTypeCreatedEvent extends RemoteEntityEvent<TargetType>
        implements EntityCreatedEvent {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public TargetTypeCreatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     *
     * @param baseEntity
     *            the TargetType
     * @param applicationId
     *            the origin application id
     */
    public TargetTypeCreatedEvent(final TargetType baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
