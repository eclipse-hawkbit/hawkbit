/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.event.entity.EntityDeletedEvent;

/**
 *
 * Defines the remote event of deleting a {@link org.eclipse.hawkbit.repository.model.TenantConfiguration}.
 */
public class TenantConfigurationDeletedEvent extends RemoteIdEvent implements EntityDeletedEvent {

    private static final long serialVersionUID = 2L;
    private String configKey;
    private String configValue;

    /**
     * Default constructor.
     */
    public TenantConfigurationDeletedEvent() {
        // for serialization libs like jackson
    }

    /**
     *
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param configKey
     *            the config key
     * @param configValue
     *            the config value
     * @param entityClass
     *            the entity class
     * @param applicationId
     *            the origin application id
     */
    public TenantConfigurationDeletedEvent(final String tenant, final Long entityId, final String configKey,
            final String configValue, final String entityClass, final String applicationId) {
        super(entityId, tenant, entityClass, applicationId);
        this.configKey = configKey;
        this.configValue = configValue;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getConfigValue() {
        return configValue;
    }
}
