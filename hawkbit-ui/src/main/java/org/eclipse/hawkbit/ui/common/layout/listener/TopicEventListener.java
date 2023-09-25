/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.layout.listener;

import java.util.Collection;
import java.util.Collections;

import org.vaadin.spring.events.EventBus;

/**
 * Abstract class for event listener
 */
public abstract class TopicEventListener {
    private final EventBus eventBus;
    private final Collection<String> topics;
    private boolean subscribed;

    protected TopicEventListener(final EventBus eventBus, final String topic) {
        this(eventBus, Collections.singleton(topic));
    }

    protected TopicEventListener(final EventBus eventBus, final Collection<String> topics) {
        this.eventBus = eventBus;
        this.topics = topics;
    }

    /**
     * Subscribe the event
     */
    public void subscribe() {
        if (subscribed) {
            return;
        }

        topics.forEach(topic -> eventBus.subscribe(this, topic));
        subscribed = true;
    }

    /**
     * Unsubscribe the event
     */
    public void unsubscribe() {
        if (!subscribed) {
            return;
        }

        eventBus.unsubscribe(this);
        subscribed = false;
    }

    /**
     * @return true if event is subscribed else false
     */
    public boolean isSubscribed() {
        return subscribed;
    }

    protected EventBus getEventBus() {
        return eventBus;
    }
}
