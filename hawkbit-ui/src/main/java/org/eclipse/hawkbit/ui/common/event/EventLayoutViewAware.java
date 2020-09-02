/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

/**
 * Event layout view on payload event
 */
public class EventLayoutViewAware extends EventViewAware {
    private final EventLayout layout;

    /**
     * Constructor for EventLayoutViewAware
     *
     * @param layout
     *            EventLayout
     * @param view
     *            EventView
     */
    public EventLayoutViewAware(final EventLayout layout, final EventView view) {
        super(view);

        this.layout = layout;
    }

    /**
     * Constructor for EventLayoutViewAware
     *
     * @param layoutViewAware
     *            EventLayoutViewAware
     */
    public EventLayoutViewAware(final EventLayoutViewAware layoutViewAware) {
        super(layoutViewAware);

        this.layout = layoutViewAware.getLayout();
    }

    /**
     * Verifies if event layout is suitable
     *
     * @param layout
     *            EventLayout
     *
     * @return <code>true</code> if the event layout matches current layout,
     *         otherwise <code>false</code>
     */
    public boolean suitableLayout(final EventLayout layout) {
        return this.layout != null && layout != null && this.layout == layout;
    }

    /**
     * Verifies if event layout view aware is suitable
     *
     * @param layoutAware
     *            EventLayoutViewAware
     *
     * @return <code>true</code> if the event layout matches current layout,
     *         otherwise <code>false</code>
     */
    public boolean suitableLayout(final EventLayoutViewAware layoutAware) {
        return suitableLayout(layoutAware.getLayout());
    }

    /**
     * Verifies if event view and layout are suitable
     *
     * @param layout
     *            EventLayout
     * @param view
     *            EventView
     *
     * @return <code>true</code> if the suitableView and suitableLayout exist,
     *         otherwise <code>false</code>
     */
    public boolean suitableViewLayout(final EventLayout layout, final EventView view) {
        return suitableView(view) && suitableLayout(layout);
    }

    /**
     * Verifies if layout aware view and layout are suitable
     *
     * @param layoutAware
     *            EventLayoutViewAware
     *
     * @return <code>true</code> if the suitableViewLayout exist, otherwise
     *         <code>false</code>
     */
    public boolean suitableViewLayout(final EventLayoutViewAware layoutAware) {
        return suitableViewLayout(layoutAware.getLayout(), layoutAware.getView());
    }

    /**
     * @return Event layout
     */
    public EventLayout getLayout() {
        return layout;
    }
}
