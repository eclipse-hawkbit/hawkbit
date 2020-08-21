/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.layout.listener;

import java.util.Map;

import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventViewAware;
import org.eclipse.hawkbit.ui.common.event.LayoutVisibilityEventPayload;
import org.eclipse.hawkbit.ui.common.event.LayoutVisibilityEventPayload.VisibilityType;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Event listener for layout visibility
 */
public class LayoutVisibilityListener extends ViewAwareListener {
    private final Map<EventLayout, VisibilityHandler> layoutVisibilityHandlers;

    /**
     * Constructor for LayoutVisibilityListener
     *
     * @param eventBus
     *            UIEventBus
     * @param viewAware
     *            EventViewAware
     * @param layoutVisibilityHandlers
     *            layout with the visibility handlers
     */
    public LayoutVisibilityListener(final UIEventBus eventBus, final EventViewAware viewAware,
            final Map<EventLayout, VisibilityHandler> layoutVisibilityHandlers) {
        super(eventBus, CommandTopics.CHANGE_LAYOUT_VISIBILITY, viewAware);

        this.layoutVisibilityHandlers = layoutVisibilityHandlers;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    private void onSelectionChangedEvent(final LayoutVisibilityEventPayload eventPayload) {
        if (!getViewAware().suitableView(eventPayload)
                || !layoutVisibilityHandlers.keySet().contains(eventPayload.getLayout())) {
            return;
        }

        final VisibilityHandler handler = layoutVisibilityHandlers.get(eventPayload.getLayout());
        if (handler == null) {
            return;
        }

        if (VisibilityType.SHOW == eventPayload.getVisibilityType()) {
            handler.show();
        } else {
            handler.hide();
        }
    }

    /**
     * Handler for visibility layout behaviour.
     */
    public static class VisibilityHandler {
        private final Runnable show;
        private final Runnable hide;

        /**
         * Constructor for VisibilityHandler
         *
         * @param show
         *            show callback
         * @param hide
         *            hide callback
         */
        public VisibilityHandler(final Runnable show, final Runnable hide) {
            this.show = show;
            this.hide = hide;
        }

        /**
         * Execute show callback.
         */
        public void show() {
            show.run();
        }

        /**
         * Execute hide callback.
         */
        public void hide() {
            hide.run();
        }
    }
}
