/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Indexed;
import com.vaadin.ui.Grid;

/**
 * Abstract table class.
 *
 */
public abstract class AbstractGrid extends Grid implements RefreshableContainer {

    private static final long serialVersionUID = 4856562746502217630L;

    protected final VaadinMessageSource i18n;

    protected final transient EventBus.UIEventBus eventBus;

    protected final SpPermissionChecker permissionChecker;

    protected AbstractGrid(final VaadinMessageSource i18n, final UIEventBus eventBus, final SpPermissionChecker permissionChecker) {
        this.i18n = i18n;
        this.eventBus = eventBus;
        this.permissionChecker = permissionChecker;
        setSizeFull();
        setImmediate(true);
        setId(getGridId());
        setSelectionMode(SelectionMode.NONE);
        setColumnReorderingAllowed(true);
        addNewContainerDS();
        eventBus.subscribe(this);
    }

    /**
     * Refresh the container.
     */
    @Override
    public void refreshContainer() {
        final Container container = getContainerDataSource();
        if (!(container instanceof LazyQueryContainer)) {
            return;
        }
        ((LazyQueryContainer) container).refresh();
    }

    private void addNewContainerDS() {
        final Container container = createContainer();
        setContainerDataSource((Indexed) container);
        addContainerProperties();
        setColumnExpandRatio();
        setColumnProperties();
        setColumnHeaderNames();
        addColumnRenderes();

        final CellDescriptionGenerator cellDescriptionGenerator = getDescriptionGenerator();
        if (getDescriptionGenerator() != null) {
            setCellDescriptionGenerator(cellDescriptionGenerator);
        }

        // Allow column hiding
        for (final Column c : getColumns()) {
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

    protected abstract String getGridId();

    protected abstract void setColumnProperties();

    protected abstract void addColumnRenderes();

    protected abstract void setHiddenColumns();

    protected abstract CellDescriptionGenerator getDescriptionGenerator();
}
