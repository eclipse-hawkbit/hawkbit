/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.common.table.AbstractTableLayout;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Software module table layout. (Upload Management)
 */
@SpringComponent
@ViewScope
public class SoftwareModuleTableLayout extends AbstractTableLayout {

    private static final long serialVersionUID = 6464291374980641235L;

    @Autowired
    private SoftwareModuleDetails softwareModuleDetails;

    @Autowired
    private SoftwareModuleTableHeader smTableHeader;

    @Autowired
    private SoftwareModuleTable smTable;

    /**
     * Initialize the filter layout.
     */
    @PostConstruct
    void init() {
        super.init(smTableHeader, smTable, softwareModuleDetails);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableLayout#
     * isShortCutKeysRequired()
     */
    @Override
    protected boolean isShortCutKeysRequired() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableLayout#
     * getShortCutKeysHandler()
     */
    @Override
    protected Handler getShortCutKeysHandler() {
        return new Handler() {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(final Action action, final Object sender, final Object target) {
                if (ACTION_CTRL_A.equals(action)) {
                    smTable.selectAll();
                    getEventBus().publish(this, new SoftwareModuleEvent(SoftwareModuleEventType.SELECT_ALL, null));
                }
            }

            @Override
            public Action[] getActions(final Object target, final Object sender) {
                return new Action[] { ACTION_CTRL_A };
            }
        };
    }
}
