/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Defines the remote event for updating a {@link SoftwareModuleType}.
 *
 */
public class SoftwareModuleTypeUpdatedEvent extends RemoteEntityEvent<SoftwareModuleType> {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SoftwareModuleTypeUpdatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param baseEntity
     *            SoftwareModuleType entity
     * @param applicationId
     *            the origin application id
     */
    public SoftwareModuleTypeUpdatedEvent(final SoftwareModuleType baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
