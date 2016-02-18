/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Label;

/**
 * Rollout Group Targets List View.
 */
@SpringComponent
@ViewScope
public class RolloutGroupTargetsListView extends AbstractSimpleTableLayout {

    private static final long serialVersionUID = 26089134783467012L;

    @Autowired
    private RolloutGroupTargetsListHeader rolloutGroupTargetsListHeader;

    @Autowired
    private RolloutGroupTargetsListTable rolloutGroupTargetsListTable;
    
    @Autowired
    private RolloutGroupTargetsCountLabelMessage rolloutGroupTargetsCountLabelMessage;
    @Autowired
    private RolloutListGrid3 rolloutListGrid;
    /**
     * Initialization of Rollout group component.
     */
    @PostConstruct
    protected void init() {
        super.init(rolloutGroupTargetsListHeader, rolloutGroupTargetsListTable,rolloutListGrid);
    }

    @Override
    protected boolean hasCountMessage() {
        
        return true;
    }
    
    @Override
    protected Label getCountMessageLabel() {
        
        return rolloutGroupTargetsCountLabelMessage;
    }

}
