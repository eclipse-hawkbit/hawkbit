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
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.context.ApplicationEventPublisher;

/**
 * A singleton bean which holds the event publisher and service origin Id in
 * order to publish remote application events. It can be used in beans not
 * instantiated by spring e.g. JPA entities which cannot be auto-wired.
 */
public final class EventPublisherHolder {

    private static final EventPublisherHolder SINGLETON = new EventPublisherHolder();

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private ServiceMatcher serviceMatcher;

    @Autowired
    private BusProperties bus;

    private EventPublisherHolder() {
    }

    /**
     * @return the event publisher holder singleton instance
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

    /**
     * @return the service origin Id coming either from {@link ServiceMatcher}
     *         when available or {@link BusProperties} otherwise.
     */
    public String getApplicationId() {
        return serviceMatcher != null ? serviceMatcher.getServiceId() : bus.getId();
    }
}
