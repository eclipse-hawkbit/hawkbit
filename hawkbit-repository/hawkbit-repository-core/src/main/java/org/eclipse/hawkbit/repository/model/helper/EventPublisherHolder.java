/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

/**
 * A singleton bean which holds the event publisher to have to the cache manager
 * in beans not instantiated by spring e.g. JPA entities or
 * CacheFieldEntityListener which cannot be autowired.
 *
 */
public final class EventPublisherHolder {

    private static final EventPublisherHolder SINGLETON = new EventPublisherHolder();

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationContext applicationContext;

    private EventPublisherHolder() {

    }

    /**
     * @return the cache manager holder singleton instance
     */
    public static EventPublisherHolder getInstance() {
        return SINGLETON;
    }

    /**
     * @return the eventPublisher
     */
    public ApplicationEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    public String getApplicationId() {
        return applicationContext.getId();
    }

}
