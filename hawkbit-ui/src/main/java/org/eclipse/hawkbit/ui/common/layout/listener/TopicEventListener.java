/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.layout.listener;

import java.util.Collection;
import java.util.Collections;

import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Abstract class for event listener
 */
public abstract class TopicEventListener {
    private final UIEventBus eventBus;
    private final Collection<String> topics;
    private boolean subscribed;

    protected TopicEventListener(final UIEventBus eventBus, final String topic) {
        this(eventBus, Collections.singleton(topic));
    }

    protected TopicEventListener(final UIEventBus eventBus, final Collection<String> topics) {
        this.eventBus = eventBus;
        this.topics = topics;

        subscribe();
    }

    /**
     * Subscribe the event
     */
    public void subscribe() {
        topics.forEach(topic -> eventBus.subscribe(this, topic));
        subscribed = true;
    }

    /**
     * Unsubscribe the event
     */
    public void unsubscribe() {
        eventBus.unsubscribe(this);
        subscribed = false;
    }

    /**
     * @return true if event is subscribed else false
     */
    public boolean isSubscribed() {
        return subscribed;
    }

    protected UIEventBus getEventBus() {
        return eventBus;
    }
}
