/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.layout.listener;

import java.util.List;
import java.util.Objects;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload.SelectionChangedEventType;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.springframework.util.CollectionUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Event listener for selection changed
 *
 * @param <T>
 *            Generic type of ProxyIdentifiableEntity
 */
public class SelectionChangedListener<T extends ProxyIdentifiableEntity> extends LayoutViewAwareListener {
    private final List<MasterEntityAwareComponent<T>> masterEntityAwareComponents;

    /**
     * Constructor for SelectionChangedListener
     *
     * @param eventBus
     *            UIEventBus
     * @param layoutViewAware
     *            EventLayoutViewAware
     * @param masterEntityAwareComponents
     *            List of master entity aware components
     */
    public SelectionChangedListener(final UIEventBus eventBus, final EventLayoutViewAware layoutViewAware,
            final List<MasterEntityAwareComponent<T>> masterEntityAwareComponents) {
        super(eventBus, EventTopics.SELECTION_CHANGED, layoutViewAware);

        this.masterEntityAwareComponents = masterEntityAwareComponents;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    private void onSelectionChangedEvent(final SelectionChangedEventPayload<T> eventPayload) {
        if (!getLayoutViewAware().suitableViewLayout(eventPayload)) {
            return;
        }

        if (eventPayload.getSelectionChangedEventType() == SelectionChangedEventType.ENTITY_SELECTED) {
            onSelectionChanged(eventPayload.getEntity());
        } else {
            onSelectionChanged(null);
        }
    }

    private void onSelectionChanged(final T entity) {
        if (CollectionUtils.isEmpty(masterEntityAwareComponents)) {
            return;
        }

        masterEntityAwareComponents.stream().filter(Objects::nonNull)
                .forEach(component -> component.masterEntityChanged(entity));
    }
}
