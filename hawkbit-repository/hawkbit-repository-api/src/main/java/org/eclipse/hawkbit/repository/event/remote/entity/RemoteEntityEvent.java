/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import java.io.Serial;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.event.remote.EventEntityManagerHolder;
import org.eclipse.hawkbit.repository.event.remote.RemoteIdEvent;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A base definition class for remote events which contain a tenant aware base entity.
 *
 * @param <E> the type of the entity
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class RemoteEntityEvent<E extends TenantAwareBaseEntity> extends RemoteIdEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private transient E entity;

    /**
     * Constructor.
     *
     * @param baseEntity the base entity
     * @param applicationId the origin application id
     */
    protected RemoteEntityEvent(final E baseEntity, final String applicationId) {
        super(baseEntity.getId(), baseEntity.getTenant(), baseEntity.getClass(), applicationId);
        this.entity = baseEntity;
    }

    @JsonIgnore
    public Optional<E> getEntity() {
        if (entity == null) {
            entity = reloadEntityFromRepository();
        }
        return Optional.ofNullable(entity);
    }

    @SuppressWarnings("unchecked")
    private E reloadEntityFromRepository() {
        try {
            final Class<E> clazz = (Class<E>) Class.forName(getEntityClass());
            return EventEntityManagerHolder.getInstance().getEventEntityManager().findEntity(
                    getTenant(), getEntityId(), clazz);
        } catch (final ClassNotFoundException e) {
            log.error("Cannot reload entity because class is not found", e);
        }
        return null;
    }
}