/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model.helper;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.eventbus.EventBus;

/**
 * A singleton bean which holds the {@link EventBus} to have to the cache
 * manager in beans not instantiated by spring e.g. JPA entities or
 * CacheFieldEntityListener which cannot be autowired.
 *
 */
public final class EventBusHolder {

    private static final EventBusHolder SINGLETON = new EventBusHolder();

    @Autowired
    private EventBus eventBus;

    private EventBusHolder() {

    }

    /**
     * @return the cache manager holder singleton instance
     */
    public static EventBusHolder getInstance() {
        return SINGLETON;
    }

    /**
     * @return the eventBus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @param eventBus
     *            the eventBus to set
     */
    public void setEventBus(final EventBus eventBus) {
        this.eventBus = eventBus;
    }

}
