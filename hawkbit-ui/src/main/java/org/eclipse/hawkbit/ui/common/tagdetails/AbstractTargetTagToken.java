/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.ui.push.events.TargetTagCreatedEventHolder;
import org.eclipse.hawkbit.ui.push.events.TargetTagDeletedEventHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

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
    void onEventTargetTagCreated(final TargetTagCreatedEventHolder holder) {
        holder.getEvents().stream().map(event -> event.getEntity())
                .forEach(tag -> setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour()));
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onTargetDeletedEvent(final TargetTagDeletedEventHolder holder) {
        holder.getEvents().stream().map(event -> getTagIdByTagName(event.getEntity().getName()))
                .forEach(deletedTagId -> removeTagFromCombo(deletedTagId));
    }

}
