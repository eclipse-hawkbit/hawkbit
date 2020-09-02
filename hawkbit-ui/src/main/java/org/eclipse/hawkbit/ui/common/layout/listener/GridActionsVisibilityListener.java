/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.layout.listener;

import org.eclipse.hawkbit.ui.common.event.ActionsVisibilityEventPayload;
import org.eclipse.hawkbit.ui.common.event.ActionsVisibilityEventPayload.ActionsVisibilityType;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Event listener for grid actions visibility
 */
public class GridActionsVisibilityListener extends LayoutViewAwareListener {
    private final Runnable hideAllCallback;
    private final Runnable showEditCallback;
    private final Runnable showDeleteCallback;

    /**
     * Constructor for GridActionsVisibilityListener
     *
     * @param eventBus
     *            GridActionsVisibilityListener
     * @param layoutViewAware
     *            EventLayoutViewAware
     * @param hideAllCallback
     *            Runnable
     * @param showEditCallback
     *            Runnable
     * @param showDeleteCallback
     *            Runnable
     */
    public GridActionsVisibilityListener(final UIEventBus eventBus, final EventLayoutViewAware layoutViewAware,
            final Runnable hideAllCallback, final Runnable showEditCallback, final Runnable showDeleteCallback) {
        super(eventBus, CommandTopics.CHANGE_GRID_ACTIONS_VISIBILITY, layoutViewAware);

        this.hideAllCallback = hideAllCallback;
        this.showEditCallback = showEditCallback;
        this.showDeleteCallback = showDeleteCallback;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    private void onSelectionChangedEvent(final ActionsVisibilityEventPayload eventPayload) {
        if (!getLayoutViewAware().suitableViewLayout(eventPayload)) {
            return;
        }

        final ActionsVisibilityType actionsVisibilityType = eventPayload.getActionsVisibilityType();

        if (actionsVisibilityType == ActionsVisibilityType.HIDE_ALL) {
            hideAllCallback.run();
        } else if (actionsVisibilityType == ActionsVisibilityType.SHOW_EDIT) {
            showEditCallback.run();
        } else if (actionsVisibilityType == ActionsVisibilityType.SHOW_DELETE) {
            showDeleteCallback.run();
        }
    }
}
