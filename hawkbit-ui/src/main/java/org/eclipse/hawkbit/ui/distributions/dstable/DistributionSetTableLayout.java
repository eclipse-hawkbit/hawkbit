/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.common.table.AbstractTableLayout;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTableEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTableEvent.DistributionSetComponentEvent;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Software module table layout.
 * 
 *
 * 
 */
@SpringComponent
@ViewScope
public class DistributionSetTableLayout extends AbstractTableLayout {

    private static final long serialVersionUID = 6464291374980641235L;

    /**
     * Details to be autowired before table as details listens to value change
     * of table.
     */
    @Autowired
    private DistributionSetDetails distributionDetails;

    @Autowired
    private DistributionSetTableHeader dsTableHeader;

    @Autowired
    private DistributionSetTable dsTable;

    /**
     * Initialize the filter layout.
     */
    @PostConstruct
    void init() {
        super.init(dsTableHeader, dsTable, distributionDetails);
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
                    dsTable.selectAll();
                    getEventBus().publish(this,
                            new DistributionSetTableEvent(DistributionSetComponentEvent.SELECT_ALL));
                }
            }

            @Override
            public Action[] getActions(final Object target, final Object sender) {
                return new Action[] { ACTION_CTRL_A };
            }
        };
    }
}
