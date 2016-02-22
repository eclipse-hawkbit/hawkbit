/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Indexed;
import com.vaadin.ui.Grid;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract table class.
 *
 */
public abstract class AbstractSimpleGrid extends Grid {

    private static final long serialVersionUID = 4856562746502217630L;

    /**
     * Initialize the components.
     */
    protected void init() {
        setSizeFull();
        setImmediate(true);
        setId(getTableId());
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);

        addNewContainerDS();
        setSelectionMode(SelectionMode.NONE);
        setColumnReorderingAllowed(true);
    }

    private void addNewContainerDS() {
        final Container container = createContainer();
        setContainerDataSource((Indexed) container);
        addContainerProperties();
        setColumnExpandRatio();
        setColumnProperties();
        setColumnHeaderNames();
        addColumnRenderes();
        // Allow column hiding
        for (Column c : getColumns()) {
            c.setHidable(true);
        }
        setHiddenColumns();

        int size = 0;
        if (container != null) {
            size = container.size();
        }
        if (size == 0) {
            setData(SPUIDefinitions.NO_DATA);
        }
        
    }

    protected abstract Container createContainer();

    protected abstract void addContainerProperties();

    protected abstract void setColumnExpandRatio();

    protected abstract void setColumnHeaderNames();

    protected abstract String getTableId();

    protected abstract void setColumnProperties();

    protected abstract void addColumnRenderes();

    protected abstract void setHiddenColumns();
}
