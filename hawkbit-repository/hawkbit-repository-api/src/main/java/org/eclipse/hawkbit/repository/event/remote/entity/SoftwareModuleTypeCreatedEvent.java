/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.entitiy.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Defines the remote event of creating a new {@link SoftwareModuleType}.
 *
 */
public class SoftwareModuleTypeCreatedEvent extends RemoteEntityEvent<SoftwareModuleType>
        implements EntityCreatedEvent {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SoftwareModuleTypeCreatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param baseEntity
     *            the SoftwareModuleType
     * @param applicationId
     *            the origin application id
     */
    public SoftwareModuleTypeCreatedEvent(final SoftwareModuleType baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
