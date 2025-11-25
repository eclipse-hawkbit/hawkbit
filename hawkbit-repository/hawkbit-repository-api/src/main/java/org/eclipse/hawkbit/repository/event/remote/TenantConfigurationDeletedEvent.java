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
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager.CacheEvictEvent;

/**
 * Defines the remote event of deleting a {@link org.eclipse.hawkbit.repository.model.TenantConfiguration}.
 */
@NoArgsConstructor // for serialization libs like jackson
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TenantConfigurationDeletedEvent extends RemoteIdEvent implements EntityDeletedEvent, CacheEvictEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private String configKey;
    @ToString.Exclude
    private String configValue;

    public TenantConfigurationDeletedEvent(
            final String tenant, final Long entityId, final Class<? extends TenantAwareBaseEntity> entityClass,
            final String configKey, final String configValue) {
        super(tenant, entityId, entityClass);
        this.configKey = configKey;
        this.configValue = configValue;
    }

    // overrides since the default impl is based on entity id while the tenant cache is key (string) based
    @Override
    public String getCacheKey() {
        return configKey;
    }
}
