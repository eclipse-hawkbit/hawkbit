/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.layout.listener;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload;
import org.eclipse.hawkbit.ui.common.grid.support.SelectionSupport;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Event listener for select grid entity
 *
 * @param <T>
 *            Generic type of ProxyIdentifiableEntity
 */
public class SelectGridEntityListener<T extends ProxyIdentifiableEntity> extends LayoutViewAwareListener {
    private final SelectionSupport<T> selectionSupport;

    /**
     * Constructor for SelectGridEntityListener
     *
     * @param eventBus
     *            UIEventBus
     * @param layoutViewAware
     *            EventLayoutViewAware
     * @param selectionSupport
     *            Generic type selection support
     */
    public SelectGridEntityListener(final UIEventBus eventBus, final EventLayoutViewAware layoutViewAware,
            final SelectionSupport<T> selectionSupport) {
        super(eventBus, CommandTopics.SELECT_GRID_ENTITY, layoutViewAware);

        this.selectionSupport = selectionSupport;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    private void onSelectGridEntityEvent(final SelectionChangedEventPayload<T> eventPayload) {
        if (getLayoutViewAware().suitableViewLayout(eventPayload)) {
            selectionSupport.deselectAll();
            selectionSupport.select(eventPayload.getEntity());
        }
    }
}
