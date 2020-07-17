/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

import java.util.Objects;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.springframework.util.StringUtils;

import com.google.common.base.MoreObjects;

/**
 * Payload identifier containing information about an modified event
 */
public class EntityModifiedEventPayloadIdentifier {
    private final Class<? extends ProxyIdentifiableEntity> parentType;
    private final Class<? extends ProxyIdentifiableEntity> entityType;
    private final EntityModifiedEventType modifiedEventType;
    private final String eventTypeMessageKey;

    /**
     * Constructor for EntityModifiedEventPayloadIdentifier
     *
     * @param entityType
     *          Event payload of identifiable entity type
     * @param modifiedEventType
     *          EntityModifiedEventType
     */
    public EntityModifiedEventPayloadIdentifier(final Class<? extends ProxyIdentifiableEntity> entityType,
            final EntityModifiedEventType modifiedEventType) {
        this(entityType, modifiedEventType, null);
    }

    /**
     * Constructor for EntityModifiedEventPayloadIdentifier
     *
     * @param entityType
     *          Event payload of identifiable entity type
     * @param modifiedEventType
     *          EntityModifiedEventType
     * @param eventTypeMessageKey
     *          Key for event type message
     */
    public EntityModifiedEventPayloadIdentifier(final Class<? extends ProxyIdentifiableEntity> entityType,
            final EntityModifiedEventType modifiedEventType, final String eventTypeMessageKey) {
        this(null, entityType, modifiedEventType, eventTypeMessageKey);
    }

    /**
     * Constructor for EntityModifiedEventPayloadIdentifier
     *
     * @param parentType
     *          Event payload of identifiable parent type
     * @param entityType
     *          Event payload of identifiable entity type
     * @param modifiedEventType
     *          EntityModifiedEventType
     */
    public EntityModifiedEventPayloadIdentifier(final Class<? extends ProxyIdentifiableEntity> parentType,
            final Class<? extends ProxyIdentifiableEntity> entityType,
            final EntityModifiedEventType modifiedEventType) {
        this(parentType, entityType, modifiedEventType, null);
    }

    /**
     * Constructor for EntityModifiedEventPayloadIdentifier
     *
     * @param parentType
     *          Event payload of identifiable parent type
     * @param entityType
     *          Event payload of identifiable entity type
     * @param modifiedEventType
     *          EntityModifiedEventType
     * @param eventTypeMessageKey
     *          Key for event type message
     */
    public EntityModifiedEventPayloadIdentifier(final Class<? extends ProxyIdentifiableEntity> parentType,
            final Class<? extends ProxyIdentifiableEntity> entityType, final EntityModifiedEventType modifiedEventType,
            final String eventTypeMessageKey) {
        this.parentType = parentType;
        this.entityType = entityType;
        this.modifiedEventType = modifiedEventType;
        this.eventTypeMessageKey = eventTypeMessageKey;
    }

    /**
     * @return Event payload of identifiable parent type
     */
    public Class<? extends ProxyIdentifiableEntity> getParentType() {
        return parentType;
    }

    /**
     * @return Event payload of identifiable entity type
     */
    public Class<? extends ProxyIdentifiableEntity> getEntityType() {
        return entityType;
    }

    /**
     * @return Event type of modified entity
     */
    public EntityModifiedEventType getModifiedEventType() {
        return modifiedEventType;
    }

    /**
     * @return Key of event type message
     */
    public String getEventTypeMessageKey() {
        return eventTypeMessageKey;
    }

    /**
     * @return <code>true</code> if the message key is not empty, otherwise
     *         <code>false</code>
     */
    public boolean shouldBeDeffered() {
        return !StringUtils.isEmpty(eventTypeMessageKey);
    }

    /**
     * Static method for constructor EntityModifiedEventPayloadIdentifier
     *
     * @param eventPayload
     *          EntityModifiedEventPayload
     *
     * @return Payload identifier containing information about an modified event
     */
    public static EntityModifiedEventPayloadIdentifier of(final EntityModifiedEventPayload eventPayload) {
        return new EntityModifiedEventPayloadIdentifier(eventPayload.getParentType(), eventPayload.getEntityType(),
                eventPayload.getEntityModifiedEventType());
    }

    @Override
    public int hashCode() {
        // eventTypeMessageKey is omitted intentionally, because it is not
        // relevant for event identification
        return getParentType() != null
                ? Objects.hash(getParentType().getName(), getEntityType().getName(), modifiedEventType)
                : Objects.hash(getEntityType().getName(), modifiedEventType);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EntityModifiedEventPayloadIdentifier other = (EntityModifiedEventPayloadIdentifier) obj;

        // eventTypeMessageKey is omitted intentionally, because it is not
        // relevant for event identification
        return Objects.equals(this.getParentType(), other.getParentType())
                && Objects.equals(this.getEntityType(), other.getEntityType())
                && Objects.equals(this.modifiedEventType, other.modifiedEventType);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Parent Type", getParentType() != null ? getParentType().getName() : "-")
                .add("Entity Type", getEntityType().getName()).add("ModifiedEventType", modifiedEventType.name())
                .toString();
    }
}
