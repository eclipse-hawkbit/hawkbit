/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.entity.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;

/**
 * Defines the remote event of updating a {@link TenantConfiguration}.
 *
 */
public class TenantConfigurationUpdatedEvent extends RemoteEntityEvent<TenantConfiguration>
        implements EntityUpdatedEvent {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public TenantConfigurationUpdatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param baseEntity
     *            the tenantConfiguration
     * @param applicationId
     *            the origin application id
     */
    public TenantConfigurationUpdatedEvent(final TenantConfiguration baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
