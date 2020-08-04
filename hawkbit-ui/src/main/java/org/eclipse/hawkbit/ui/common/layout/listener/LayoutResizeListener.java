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
import org.eclipse.hawkbit.ui.common.event.LayoutResizeEventPayload;
import org.eclipse.hawkbit.ui.common.event.LayoutResizeEventPayload.ResizeType;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Event listener for layout resize
 */
public class LayoutResizeListener extends ViewAwareListener {
    private final Map<EventLayout, ResizeHandler> layoutResizeHandlers;

    /**
     * Constructor for LayoutResizeListener
     *
     * @param eventBus
     *            UIEventBus
     * @param viewAware
     *            EventViewAware
     * @param layoutResizeHandlers
     *            layout with the resize handlers
     */
    public LayoutResizeListener(final UIEventBus eventBus, final EventViewAware viewAware,
            final Map<EventLayout, ResizeHandler> layoutResizeHandlers) {
        super(eventBus, CommandTopics.RESIZE_LAYOUT, viewAware);

        this.layoutResizeHandlers = layoutResizeHandlers;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    private void onSelectionChangedEvent(final LayoutResizeEventPayload eventPayload) {
        if (!getViewAware().suitableView(eventPayload)
                || !layoutResizeHandlers.keySet().contains(eventPayload.getLayout())) {
            return;
        }

        final ResizeHandler handler = layoutResizeHandlers.get(eventPayload.getLayout());
        if (handler == null) {
            return;
        }

        if (ResizeType.MAXIMIZE == eventPayload.getResizeType()) {
            handler.maximize();
        } else {
            handler.minimize();
        }
    }

    /**
     * Handler for maximize/minimize layout behaviour.
     */
    public static class ResizeHandler {
        private final Runnable maximize;
        private final Runnable minimize;

        /**
         * Constructor for ResizeHandler
         *
         * @param maximize
         *            maximize callback
         * @param minimize
         *            minimize callback
         */
        public ResizeHandler(final Runnable maximize, final Runnable minimize) {
            this.maximize = maximize;
            this.minimize = minimize;
        }

        /**
         * Execute maximize callback.
         */
        public void maximize() {
            maximize.run();
        }

        /**
         * Execute minimize callback.
         */
        public void minimize() {
            minimize.run();
        }
    }
}
