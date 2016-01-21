/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Target Tag filter layout.
 * 
 *
 *
 * 
 */

@SpringComponent
@ViewScope
public class TargetTagFilterLayout extends AbstractTargetTagFilterLayout {

    private static final long serialVersionUID = 2153612878428575009L;

    @Autowired
    private transient EventBus.SessionEventBus eventbus;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private TargetTagFilterHeader filterByHeader;

    @Autowired
    private MultipleTargetFilter multipleTargetFilter;

    /**
     * Initialize the filter layout.
     */
    @PostConstruct
    public void init() {
        super.init(filterByHeader, multipleTargetFilter);
        eventbus.subscribe(this);

    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.HIDE_TARGET_TAG_LAYOUT) {
            setVisible(false);
        }
        if (event == ManagementUIEvent.SHOW_TARGET_TAG_LAYOUT) {
            setVisible(true);
        }
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

        return managementUIState.isTargetTagFilterClosed();
    }
}
