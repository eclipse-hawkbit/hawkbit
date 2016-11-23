/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterLayout;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;

/**
 * Distribution Set Type filter buttons layout.
 * 
 *
 * 
 */

@SpringComponent
@UIScope
public class DSTypeFilterLayout extends AbstractFilterLayout {

    private static final long serialVersionUID = 2689002932344750781L;

    @Autowired
    private transient EventBus.SessionEventBus eventbus;

    @Autowired
    private DSTypeFilterHeader dsTypeFilterHeader;

    @Autowired
    private DSTypeFilterButtons dsTypeFilterButtons;

    @Autowired
    private DSTypeFilterButtonClick dsTypeFilterButtonClick;

    @Autowired
    private ManageDistUIState manageDistUIState;

    /**
     * Initialize the filter layout.
     */
    @PostConstruct
    public void init() {
        super.init(dsTypeFilterHeader, dsTypeFilterButtons, dsTypeFilterButtonClick);
        eventbus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionsUIEvent event) {
        if (event == DistributionsUIEvent.HIDE_DIST_FILTER_BY_TYPE) {
            setVisible(false);
        }
        if (event == DistributionsUIEvent.SHOW_DIST_FILTER_BY_TYPE) {
            setVisible(true);
        }
    }

    @PreDestroy
    void destroy() {
        /*
         * It's good manners to do this, even though vaadin-spring will
         * automatically unsubscribe when this UI is garbage collected.
         */
        eventbus.unsubscribe(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.filterlayout.AbstractFilterLayout#
     * onLoadIsTypeFilterIsClosed()
     */
    @Override
    public Boolean onLoadIsTypeFilterIsClosed() {

        return manageDistUIState.isDistTypeFilterClosed();
    }

}
