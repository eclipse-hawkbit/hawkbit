/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * 
 * Abstract table layout class which builds layout with table
 * {@link AbstractSimpleTable} and table header
 * {@link AbstractSimpleTableHeader}.
 *
 */
public abstract class AbstractSimpleTableLayout extends VerticalLayout {

    private static final long serialVersionUID = 8611248179949245460L;

    private AbstractSimpleTableHeader tableHeader;

    
    private AbstractSimpleGrid grid;


    protected void init(final AbstractSimpleTableHeader tableHeader,final AbstractSimpleGrid grid) {
        this.tableHeader = tableHeader;
        this.grid = grid;
        buildLayout();
    }

    private void buildLayout() {
        setSizeFull();
        setSpacing(true);
        setMargin(false);
        setStyleName("group");
        final VerticalLayout tableHeaderLayout = new VerticalLayout();
        tableHeaderLayout.setSizeFull();
        tableHeaderLayout.setSpacing(false);
        tableHeaderLayout.setMargin(false);

        tableHeaderLayout.setStyleName("table-layout");
        tableHeaderLayout.addComponent(tableHeader);

        tableHeaderLayout.setComponentAlignment(tableHeader, Alignment.TOP_CENTER);
//        grid.setSizeFull();
        tableHeaderLayout.addComponent(grid);
        tableHeaderLayout.setComponentAlignment(grid, Alignment.TOP_CENTER);
        tableHeaderLayout.setExpandRatio(grid, 1.0f);
        

        addComponent(tableHeaderLayout);
        setComponentAlignment(tableHeaderLayout, Alignment.TOP_CENTER);
        setExpandRatio(tableHeaderLayout, 1.0f);
        if (hasCountMessage()) {
            final HorizontalLayout rolloutGroupTargetsCountLayout = createCountMessageComponent();
            addComponent(rolloutGroupTargetsCountLayout);
            setComponentAlignment(rolloutGroupTargetsCountLayout, Alignment.BOTTOM_CENTER);
        }
        
    }

    private HorizontalLayout createCountMessageComponent() {
        final HorizontalLayout rolloutGroupTargetsCountLayout = new HorizontalLayout();
        final Label countMessageLabel = getCountMessageLabel();
        countMessageLabel.setId(SPUIComponetIdProvider.ROLLOUT_GROUP_TARGET_LABEL);
        rolloutGroupTargetsCountLayout.addComponent(getCountMessageLabel());
        rolloutGroupTargetsCountLayout.setStyleName("footer-layout");
        rolloutGroupTargetsCountLayout.setWidth("100%");
        return rolloutGroupTargetsCountLayout;

    }

    /**
     * Only in rollout group targets view count message is displayed.
     * 
     * @return
     */
    protected abstract boolean hasCountMessage();

    protected abstract Label getCountMessageLabel();
}
