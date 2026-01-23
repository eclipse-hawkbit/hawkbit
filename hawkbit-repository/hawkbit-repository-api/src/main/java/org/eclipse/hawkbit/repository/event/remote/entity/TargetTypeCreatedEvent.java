/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
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
import org.eclipse.hawkbit.repository.event.entity.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager.CacheEvictEvent;

/**
 * Defines the remote event of creating a new {@link TargetType}.
 */
@NoArgsConstructor // for serialization libs like jackson
public class TargetTypeCreatedEvent extends RemoteEntityEvent<TargetType> implements EntityCreatedEvent, CacheEvictEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    public TargetTypeCreatedEvent(final TargetType targetType) {
        super(targetType);
    }
}