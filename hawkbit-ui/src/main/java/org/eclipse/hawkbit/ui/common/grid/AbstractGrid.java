/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Indexed;
import com.vaadin.ui.Grid;

/**
 * Abstract table class.
 *
 */
public abstract class AbstractGrid extends Grid implements RefreshableContainer {

    private static final long serialVersionUID = 4856562746502217630L;

    @Autowired
    protected I18N i18n;

    @Autowired
    protected transient EventBus.SessionEventBus eventBus;

    /**
     * Initialize the components.
     */
    @PostConstruct
    protected void init() {
        setSizeFull();
        setImmediate(true);
        setId(getGridId());
        setSelectionMode(SelectionMode.NONE);
        setColumnReorderingAllowed(true);
        addNewContainerDS();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
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
        final LazyQueryContainer gridContainer = (LazyQueryContainer) getContainerDataSource();
        gridContainer.refresh();
    }

    public void addNewContainerDS() {
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
