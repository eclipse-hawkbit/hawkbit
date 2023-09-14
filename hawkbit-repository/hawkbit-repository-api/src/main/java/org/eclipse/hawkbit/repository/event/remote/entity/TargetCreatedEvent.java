/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.entity.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Defines the remote event of creating a new {@link Target}.
 *
 */
public class TargetCreatedEvent extends RemoteEntityEvent<Target> implements EntityCreatedEvent {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public TargetCreatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param baseEntity
     *            the target
     * @param applicationId
     *            the origin application id
     */
    public TargetCreatedEvent(final Target baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
