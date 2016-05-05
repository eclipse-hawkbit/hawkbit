/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid;

import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * 
 * Abstract grid layout class which builds layout with grid
 * {@link AbstractGrid} and table header
 * {@link AbstractGridHeader}.
 *
 */
public abstract class AbstractGridLayout extends VerticalLayout {

    private static final long serialVersionUID = 8611248179949245460L;

    private AbstractGridHeader tableHeader;
    
    private AbstractGrid grid;


    protected void init(final AbstractGridHeader tableHeader,final AbstractGrid grid) {
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
        rolloutGroupTargetsCountLayout.setStyleName(SPUIStyleDefinitions.FOOTER_LAYOUT);
        rolloutGroupTargetsCountLayout.setWidth("100%");
        return rolloutGroupTargetsCountLayout;

    }

    /**
     * Only in rollout group targets view count message is displayed.
     * 
     * @return true if count message has to be displayed
     */
    protected abstract boolean hasCountMessage();

    /**
     * Get the count message label.
     * 
     * @return count message
     */
    protected abstract Label getCountMessageLabel();
}
