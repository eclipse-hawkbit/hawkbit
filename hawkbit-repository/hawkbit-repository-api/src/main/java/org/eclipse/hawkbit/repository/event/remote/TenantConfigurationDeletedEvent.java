/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.entity.EntityDeletedEvent;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * Defines the remote event of deleting a {@link org.eclipse.hawkbit.repository.model.TenantConfiguration}.
 */
@NoArgsConstructor // for serialization libs like jackson
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TenantConfigurationDeletedEvent extends RemoteIdEvent implements EntityDeletedEvent {

    @Serial
    private static final long serialVersionUID = 2L;

    private String configKey;
    @ToString.Exclude
    private String configValue;

    /**
     * @param tenant the tenant
     * @param entityId the entity id
     * @param configKey the config key
     * @param configValue the config value
     * @param entityClass the entity class
     * @param applicationId the origin application id
     */
    public TenantConfigurationDeletedEvent(final String tenant, final Long entityId, final String configKey,
            final String configValue, final Class<? extends TenantAwareBaseEntity> entityClass,
            final String applicationId) {
        super(entityId, tenant, entityClass, applicationId);
        this.configKey = configKey;
        this.configValue = configValue;
    }
}
