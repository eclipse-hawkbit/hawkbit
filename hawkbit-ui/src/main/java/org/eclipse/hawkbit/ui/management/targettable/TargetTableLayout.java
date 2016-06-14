/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.common.table.AbstractTableLayout;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Target table layout.
 */
@SpringComponent
@ViewScope
public class TargetTableLayout extends AbstractTableLayout {

    private static final long serialVersionUID = 2248703121998709112L;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private TargetDetails targetDetails;

    @Autowired
    private TargetTableHeader targetTableHeader;

    @Autowired
    private TargetTable targetTable;

    /**
     * Initialize the filter layout.
     */
    @PostConstruct
    void init() {

        super.init(targetTableHeader, targetTable, targetDetails);
    }

    @Override
    protected void publishEvent() {

        eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.SELECT_ALL));
    }

}
