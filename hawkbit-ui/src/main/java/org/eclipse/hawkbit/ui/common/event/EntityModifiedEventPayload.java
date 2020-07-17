/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;

/**
 * Payload containing information about an modified entity
 */
public class EntityModifiedEventPayload {

    private final EntityModifiedEventType entityModifiedEventType;
    private final Class<? extends ProxyIdentifiableEntity> parentType;
    private final Long parentId;
    private final Class<? extends ProxyIdentifiableEntity> entityType;
    private final Collection<Long> entityIds;

    /**
     * Constructor for EntityModifiedEventPayload
     *
     * @param entityModifiedEventType
     *          EntityModifiedEventType
     * @param entityType
     *          Event payload entity type
     * @param entityId
     *          Entity id
     */
    public EntityModifiedEventPayload(final EntityModifiedEventType entityModifiedEventType,
            final Class<? extends ProxyIdentifiableEntity> entityType, final Long entityId) {
        this(entityModifiedEventType, entityType, Collections.singletonList(entityId));
    }

    /**
     * Constructor for EntityModifiedEventPayload
     *
     * @param entityModifiedEventType
     *          EntityModifiedEventType
     * @param entityType
     *          Event payload entity type
     * @param entityIds
     *          List of entity id
     */
    public EntityModifiedEventPayload(final EntityModifiedEventType entityModifiedEventType,
            final Class<? extends ProxyIdentifiableEntity> entityType, final Collection<Long> entityIds) {
        this(entityModifiedEventType, null, null, entityType, entityIds);
    }

    /**
     * Constructor for EntityModifiedEventPayload
     *
     * @param entityModifiedEventType
     *          EntityModifiedEventType
     * @param parentType
     *          Event payload parent type
     * @param entityType
     *          Event payload entity type
     * @param entityId
     *          Entity id
     */
    public EntityModifiedEventPayload(final EntityModifiedEventType entityModifiedEventType,
            final Class<? extends ProxyIdentifiableEntity> parentType,
            final Class<? extends ProxyIdentifiableEntity> entityType, final Long entityId) {
        this(entityModifiedEventType, parentType, entityType, Collections.singletonList(entityId));
    }

    /**
     * Constructor for EntityModifiedEventPayload
     *
     * @param entityModifiedEventType
     *          EntityModifiedEventType
     * @param parentType
     *          Event payload parent type
     * @param entityType
     *          Event payload entity type
     * @param entityIds
     *          List of entity id
     */
    public EntityModifiedEventPayload(final EntityModifiedEventType entityModifiedEventType,
            final Class<? extends ProxyIdentifiableEntity> parentType,
            final Class<? extends ProxyIdentifiableEntity> entityType, final Collection<Long> entityIds) {
        this(entityModifiedEventType, parentType, null, entityType, entityIds);
    }

    /**
     * Constructor for EntityModifiedEventPayload
     *
     * @param entityModifiedEventType
     *          EntityModifiedEventType
     * @param parentType
     *          Event payload parent type
     * @param parentId
     *          Parent id
     * @param entityType
     *          Event payload entity type
     * @param entityId
     *          Entity id
     */
    public EntityModifiedEventPayload(final EntityModifiedEventType entityModifiedEventType,
            final Class<? extends ProxyIdentifiableEntity> parentType, final Long parentId,
            final Class<? extends ProxyIdentifiableEntity> entityType, final Long entityId) {
        this(entityModifiedEventType, parentType, parentId, entityType, Collections.singletonList(entityId));
    }

    /**
     * Constructor for EntityModifiedEventPayload
     *
     * @param entityModifiedEventType
     *          EntityModifiedEventType
     * @param parentType
     *          Event payload parent type
     * @param parentId
     *          Parent id
     * @param entityType
     *          Event payload entity type
     * @param entityIds
     *          List of entity id
     */
    public EntityModifiedEventPayload(final EntityModifiedEventType entityModifiedEventType,
            final Class<? extends ProxyIdentifiableEntity> parentType, final Long parentId,
            final Class<? extends ProxyIdentifiableEntity> entityType, final Collection<Long> entityIds) {
        this.entityModifiedEventType = entityModifiedEventType;
        this.parentType = parentType;
        this.parentId = parentId;
        this.entityType = entityType;
        this.entityIds = entityIds;
    }

    /**
     * @return Entity modified event type
     */
    public EntityModifiedEventType getEntityModifiedEventType() {
        return entityModifiedEventType;
    }

    /**
     * @return Event payload parent type
     */
    public Class<? extends ProxyIdentifiableEntity> getParentType() {
        return parentType;
    }

    /**
     * @return Parent id
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * @return  Event payload entity type
     */
    public Class<? extends ProxyIdentifiableEntity> getEntityType() {
        return entityType;
    }

    /**
     * @return List of entity id
     */
    public Collection<Long> getEntityIds() {
        return entityIds;
    }

    /**
     * Static method for constructor EntityModifiedEventPayload
     *
     * @param eventPayloadIdentifier
     *          EntityModifiedEventPayloadIdentifier
     * @param parentId
     *          Parent id
     * @param entityIds
     *          list of entity id
     *
     * @return Payload containing information about an modified entity
     */
    public static EntityModifiedEventPayload of(final EntityModifiedEventPayloadIdentifier eventPayloadIdentifier,
            final Long parentId, final Collection<Long> entityIds) {
        return new EntityModifiedEventPayload(eventPayloadIdentifier.getModifiedEventType(),
                eventPayloadIdentifier.getParentType(), parentId, eventPayloadIdentifier.getEntityType(), entityIds);
    }

    /**
     * Static method for constructor EntityModifiedEventPayload
     *
     * @param eventPayloadIdentifier
     *          EntityModifiedEventPayloadIdentifier
     * @param entityIds
     *          list of entity id
     *
     * @return Payload containing information about an modified entity
     */
    public static EntityModifiedEventPayload of(final EntityModifiedEventPayloadIdentifier eventPayloadIdentifier,
            final Collection<Long> entityIds) {
        return of(eventPayloadIdentifier, null, entityIds);
    }

    /**
     * Event type of modified entity
     */
    public enum EntityModifiedEventType {
        ENTITY_ADDED, ENTITY_UPDATED, ENTITY_REMOVED;
    }
}
