/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.common.grid.AbstractGridLayout;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Label;

/**
 * 
 * Rollout list view.
 *
 */
@SpringComponent
@UIScope
public class RolloutListView extends AbstractGridLayout {

    private static final long serialVersionUID = -2703552177439393208L;

    @Autowired
    private RolloutListHeader rolloutListHeader;

    @Autowired
    private RolloutListGrid rolloutListGrid;

    @PostConstruct
    void init() {
        super.init(rolloutListHeader, rolloutListGrid);
    }

    @Override
    protected boolean hasCountMessage() {
        return false;
    }

    @Override
    protected Label getCountMessageLabel() {

        return null;
    }

}
