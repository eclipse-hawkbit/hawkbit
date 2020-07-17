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
 * Event view on payload event
 */
public class EventViewAware {
    private final EventView view;

    /**
     * Constructor for EventViewAware
     *
     * @param view
     *          EventView
     */
    public EventViewAware(final EventView view) {
        this.view = view;
    }

    /**
     * Constructor for EventViewAware
     *
     * @param viewAware
     *          EventViewAware
     */
    public EventViewAware(final EventViewAware viewAware) {
        this.view = viewAware.getView();
    }

    /**
     * Verifies if event view is suitable
     *
     * @param view
     *          EventView
     *
     * @return <code>true</code> if the view is not null, otherwise
     *         <code>false</code>
     */
    public boolean suitableView(final EventView view) {
        return this.view != null && view != null && this.view == view;
    }

    /**
     * Verifies if event view aware is suitable
     *
     * @param viewAware
     *          EventViewAware
     *
     * @return <code>true</code> if the view is suitable, otherwise
     *         <code>false</code>
     */
    public boolean suitableView(final EventViewAware viewAware) {
        return suitableView(viewAware.getView());
    }

    /**
     * @return Event view
     */
    public EventView getView() {
        return view;
    }
}
