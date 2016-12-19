/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

/**
 * Parent class for filter button layout.
 */
public abstract class AbstractFilterLayout extends VerticalLayout {

    private static final long serialVersionUID = 9190616426688385851L;

    private final AbstractFilterHeader filterHeader;

    private final AbstractFilterButtons filterButtons;

    protected AbstractFilterLayout(final AbstractFilterHeader filterHeader, final AbstractFilterButtons filterButtons) {
        this.filterHeader = filterHeader;
        this.filterButtons = filterButtons;
        buildLayout();
    }

    private void buildLayout() {
        setWidth(SPUIDefinitions.FILTER_BY_TYPE_WIDTH, Unit.PIXELS);
        setStyleName("filter-btns-main-layout");
        setHeight(100.0F, Unit.PERCENTAGE);
        setSpacing(false);
        setMargin(false);

        addComponents(filterHeader, filterButtons);

        setComponentAlignment(filterHeader, Alignment.TOP_CENTER);
        setComponentAlignment(filterButtons, Alignment.TOP_CENTER);

        setExpandRatio(filterButtons, 1.0F);
    }

    protected void restoreState() {
        if (onLoadIsTypeFilterIsClosed()) {
            setVisible(false);
        }
    }

    protected AbstractFilterButtons getFilterButtons() {
        return filterButtons;
    }

    protected AbstractFilterHeader getFilterHeader() {
        return filterHeader;
    }

    /**
     * On load, software module type filter is cloaed.
     * 
     * @return true if filter is cleaned before.
     */
    public abstract Boolean onLoadIsTypeFilterIsClosed();
}
