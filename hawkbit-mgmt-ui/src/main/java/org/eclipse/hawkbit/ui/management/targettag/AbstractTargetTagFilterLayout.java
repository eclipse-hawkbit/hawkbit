/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

/**
 * Parent class for custom target filter button layout.
 * 
 *
 * 
 */
public abstract class AbstractTargetTagFilterLayout extends VerticalLayout {

    private static final long serialVersionUID = 9190616426688385851L;

    private AbstractFilterHeader filterHeader;

    private MultipleTargetFilter multipleFilterTabs;

    /**
     * Initialize the artifact details layout.
     */
    protected void init(final AbstractFilterHeader filterHeader, final MultipleTargetFilter multipleFilterTabs) {
        this.filterHeader = filterHeader;
        this.multipleFilterTabs = multipleFilterTabs;
        buildLayout();
        restoreState();
    }

    private void buildLayout() {
        setWidth(SPUIDefinitions.FILTER_BY_TYPE_WIDTH, Unit.PIXELS);
        setStyleName("filter-btns-main-layout");
        setHeight(100.0F, Unit.PERCENTAGE);
        setSpacing(false);
        setMargin(false);

        addComponents(filterHeader, multipleFilterTabs);
        setComponentAlignment(filterHeader, Alignment.TOP_CENTER);
        setComponentAlignment(multipleFilterTabs, Alignment.TOP_CENTER);

        setExpandRatio(multipleFilterTabs, 1.0F);
    }

    private void restoreState() {
        if (onLoadIsTypeFilterIsClosed()) {
            setVisible(false);
        }
    }

    /**
     * On load, software module type filter is cloaed.
     * 
     * @return true if filter is cleaned before.
     */
    public abstract Boolean onLoadIsTypeFilterIsClosed();
}
