/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterLayout;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class DistributionTagLayout extends AbstractFilterLayout {

    /**
    * 
    */
    private static final long serialVersionUID = 4363033587261057567L;

    @Autowired
    private transient EventBus.SessionEventBus eventbus;

    @Autowired
    private DistributionTagHeader distributionTagHeader;

    @Autowired
    private DistributionTagButtons distributionTagButtons;

    @Autowired
    private DistributionTagButtonClick distributionTagButtonClick;

    @Autowired
    private ManagementUIState managementUIState;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.filterlayout.AbstractFilterLayout#
     * onLoadIsTypeFilterIsClosed()
     */

    /**
     * Initialize the filter layout.
     */
    @PostConstruct
    public void init() {
        super.init(distributionTagHeader, distributionTagButtons, distributionTagButtonClick);
        eventbus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.HIDE_DISTRIBUTION_TAG_LAYOUT) {
            managementUIState.setDistTagFilterClosed(true);
            setVisible(false);
        }
        if (event == ManagementUIEvent.SHOW_DISTRIBUTION_TAG_LAYOUT) {
            managementUIState.setDistTagFilterClosed(false);
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

    @Override
    public Boolean onLoadIsTypeFilterIsClosed() {
        return managementUIState.isDistTagFilterClosed();
    }

}
