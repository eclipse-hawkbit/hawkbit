/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;
import java.util.Arrays;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * An base definition class for an event which contains an id.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RemoteIdEvent extends RemoteTenantAwareEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long entityId;
    private String entityClass;
    private String interfaceClass;

    protected RemoteIdEvent(final String tenant, final Long entityId, final Class<? extends TenantAwareBaseEntity> entityClass) {
        super(tenant, entityId);
        this.entityClass = entityClass.getName();
        this.interfaceClass = entityClass.isInterface() ? entityClass.getName() : getInterfaceEntity(entityClass).getName();
        this.entityId = entityId;
    }

    private static Class<?> getInterfaceEntity(final Class<? extends TenantAwareBaseEntity> baseEntity) {
        final Class<?>[] interfaces = baseEntity.getInterfaces();
        return Arrays.stream(interfaces).filter(TenantAwareBaseEntity.class::isAssignableFrom).findFirst().orElse(baseEntity);
    }
}