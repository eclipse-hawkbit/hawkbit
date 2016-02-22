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
 * Groups List View.
 *
 */
@SpringComponent
@ViewScope
public class RolloutGroupsListView extends AbstractSimpleTableLayout {

    private static final long serialVersionUID = 7252345838154270259L;

    @Autowired
    private RolloutGroupsListHeader rolloutGroupListHeader;

    @Autowired
    private RolloutGroupListGrid rolloutListGrid;

    @PostConstruct
    protected void init() {
        super.init(rolloutGroupListHeader, rolloutListGrid);
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
