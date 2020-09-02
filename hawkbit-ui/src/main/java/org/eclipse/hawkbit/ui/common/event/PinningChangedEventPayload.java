/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;

/**
 * Payload event for pinning changed
 *
 * @param <T>
 *          Generic type
 */
public class PinningChangedEventPayload<T> {

    private final PinningChangedEventType pinningChangedEventType;
    private final Class<? extends ProxyIdentifiableEntity> entityType;
    private final T entityId;

    /**
     * Constructor for PinningChangedEventPayload
     *
     * @param pinningChangedEventType
     *          PinningChangedEventType
     * @param entityType
     *          Event payload of identifiable entity type
     * @param entityId
     *          Generic type id
     */
    public PinningChangedEventPayload(final PinningChangedEventType pinningChangedEventType,
            final Class<? extends ProxyIdentifiableEntity> entityType, final T entityId) {
        this.pinningChangedEventType = pinningChangedEventType;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    /**
     * @return pinning change event
     */
    public PinningChangedEventType getPinningChangedEventType() {
        return pinningChangedEventType;
    }

    /**
     * @return Event payload of identifiable entity type
     */
    public Class<? extends ProxyIdentifiableEntity> getEntityType() {
        return entityType;
    }

    /**
     * @return id
     */
    public T getEntityId() {
        return entityId;
    }

    /**
     * Type of pinning change event
     */
    public enum PinningChangedEventType {
        ENTITY_PINNED, ENTITY_UNPINNED;
    }
}
