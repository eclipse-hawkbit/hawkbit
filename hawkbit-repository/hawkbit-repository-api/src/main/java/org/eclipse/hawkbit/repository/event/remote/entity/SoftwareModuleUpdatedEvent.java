/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.entitiy.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Defines the remote event for updating a {@link SoftwareModule}.
 *
 */
public class SoftwareModuleUpdatedEvent extends RemoteEntityEvent<SoftwareModule> implements EntityUpdatedEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SoftwareModuleUpdatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param baseEntity
     *            the software module
     * @param applicationId
     *            the origin application id
     */
    public SoftwareModuleUpdatedEvent(final SoftwareModule baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
