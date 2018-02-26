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
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Defines the remote event for updating a {@link SoftwareModuleType}.
 *
 */
public class DistributionSetTypeUpdatedEvent extends RemoteEntityEvent<DistributionSetType>
        implements EntityUpdatedEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public DistributionSetTypeUpdatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param baseEntity
     *            DistributionSetType
     * @param applicationId
     *            the origin application id
     */
    public DistributionSetTypeUpdatedEvent(final DistributionSetType baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
