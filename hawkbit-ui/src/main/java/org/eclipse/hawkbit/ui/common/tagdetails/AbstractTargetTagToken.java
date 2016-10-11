/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.util.List;

import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.eventbus.event.TargetTagUpdateEvent;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.ui.push.events.TargetTagCreatedEventContainer;
import org.eclipse.hawkbit.ui.push.events.TargetTagDeletedEventContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Item;

/**
 * /** Abstract class for target tag token layout.
 * 
 * @param <T>
 *            the entity type
 */
public abstract class AbstractTargetTagToken<T extends BaseEntity> extends AbstractTagToken<T> {

    private static final long serialVersionUID = 7772876588903171201L;

    @Autowired
    protected transient TagManagement tagManagement;

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEventTargetTagCreated(final TargetTagCreatedEventContainer holder) {
        holder.getEvents().stream().map(event -> event.getEntity())
                .forEach(tag -> setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour()));
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onTargetTagDeletedEvent(final TargetTagDeletedEventContainer holder) {
        holder.getEvents().stream().map(event -> getTagIdByTagName(event.getEntity().getName()))
                .forEach(this::removeTagFromCombo);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onTargetTagUpdateEvent(final List<TargetTagUpdateEvent> events) {
        events.stream().map(event -> event.getEntity()).forEach(entity -> {
            final Item item = container.getItem(entity.getId());
            if (item != null) {
                updateItem(entity.getName(), entity.getColour(), item);
            }
        });
    }

}
