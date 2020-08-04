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

import org.eclipse.hawkbit.ui.common.event.EntityDraggingEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityDraggingEventPayload.DraggingEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.springframework.util.CollectionUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.ui.Component;

/**
 * Event listener for entity dragging
 */
public class EntityDraggingListener extends TopicEventListener {
    private static final String DROP_HINT_STYLE = "show-drop-hint";
    private final Collection<String> draggingSourceIds;
    private final Component dropComponent;

    /**
     * Constructor for EntityDraggingListener
     *
     * @param eventBus
     *            UIEventBus
     * @param draggingSourceIds
     *            List of draggins source id
     * @param dropComponent
     *            Component
     */
    public EntityDraggingListener(final UIEventBus eventBus, final Collection<String> draggingSourceIds,
            final Component dropComponent) {
        super(eventBus, EventTopics.ENTITY_DRAGGING_CHANGED);

        this.draggingSourceIds = draggingSourceIds;
        this.dropComponent = dropComponent;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    private void onEntityDraggingEvent(final EntityDraggingEventPayload eventPayload) {
        if (CollectionUtils.isEmpty(draggingSourceIds) || !draggingSourceIds.contains(eventPayload.getSourceGridId())) {
            return;
        }

        if (eventPayload.getDraggingEventType() == DraggingEventType.STARTED) {
            dropComponent.addStyleName(DROP_HINT_STYLE);
        } else {
            dropComponent.removeStyleName(DROP_HINT_STYLE);
        }
    }
}
