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

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdateEvent;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.push.TargetTagCreatedEventContainer;
import org.eclipse.hawkbit.ui.push.TargetTagDeletedEventContainer;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.spring.events.EventBus.UIEventBus;
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

    protected final transient TagManagement tagManagement;

    protected AbstractTargetTagToken(final SpPermissionChecker checker, final I18N i18n,
            final UINotification uinotification, final UIEventBus eventBus, final ManagementUIState managementUIState,
            final TagManagement tagManagement) {
        super(checker, i18n, uinotification, eventBus, managementUIState);
        this.tagManagement = tagManagement;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    public void onEventTargetTagCreated(final TargetTagCreatedEventContainer container) {
        container.getEvents().stream().map(event -> event.getEntity())
                .forEach(tag -> setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour()));
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    public void onTargetTagDeletedEvent(final TargetTagDeletedEventContainer container) {
        container.getEvents().stream().map(event -> getTagIdByTagName(event.getEntityId()))
                .forEach(this::removeTagFromCombo);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onTargetTagUpdateEvent(final List<TargetTagUpdateEvent> events) {
        events.stream().map(TargetTagUpdateEvent::getEntity).forEach(entity -> {
            final Item item = container.getItem(entity.getId());
            if (item != null) {
                updateItem(entity.getName(), entity.getColour(), item);
            }
        });
    }

}
