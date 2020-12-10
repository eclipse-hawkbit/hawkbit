/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.entity.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;

/**
 * Defines the remote event of creating a new {@link TenantConfiguration}.
 *
 */
public class TenantConfigurationCreatedEvent extends RemoteEntityEvent<TenantConfiguration>
        implements EntityCreatedEvent {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public TenantConfigurationCreatedEvent() {
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
    public TenantConfigurationCreatedEvent(final TenantConfiguration baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
