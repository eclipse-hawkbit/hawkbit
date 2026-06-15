/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.event.entity.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager.CacheEvictEvent;

/**
 * Defines the remote event of updating a {@link TenantConfiguration}.
 */
@NoArgsConstructor // for serialization libs like jackson
public class TenantConfigurationUpdatedEvent extends RemoteEntityEvent<TenantConfiguration> implements EntityUpdatedEvent, CacheEvictEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    public TenantConfigurationUpdatedEvent(final TenantConfiguration tenantConfiguration) {
        super(tenantConfiguration);
    }

    // overrides since the default impl is based on entity id while the tenant cache is key (string) based
    @Override
    public String getCacheKey() {
        return getEntity().map(TenantConfiguration::getKey).orElse(null); // null will clean all tenant cache
    }
}