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

import java.util.function.Consumer;

import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Event listener for generic type
 *
 * @param <T>
 *            Generic type
 */
public class GenericEventListener<T> extends TopicEventListener {
    private final Consumer<T> eventCallback;

    /**
     * Constructor for GenericEventListener
     *
     * @param eventBus
     *            UIEventBus
     * @param topic
     *            Topic
     * @param eventCallback
     *            Event callback
     */
    public GenericEventListener(final UIEventBus eventBus, final String topic, final Consumer<T> eventCallback) {
        super(eventBus, topic);

        this.eventCallback = eventCallback;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    private void onEvent(final T eventPayload) {
        eventCallback.accept(eventPayload);
    }
}
