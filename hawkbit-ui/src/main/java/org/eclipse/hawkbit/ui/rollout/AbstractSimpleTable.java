/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;

import com.vaadin.data.Container;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract table class.
 *
 */
public abstract class AbstractSimpleTable extends Table {

    private static final long serialVersionUID = 4856562746502217630L;

    /**
     * Initialize the components.
     */
    protected void init() {
        addStyleName("sp-table rollout-table");
        setSizeFull();
        setImmediate(true);
        setHeight(100.0f, Unit.PERCENTAGE);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);
        setSortEnabled(false);
        setId(getTableId());
        addCustomGeneratedColumns();
        addNewContainerDS();
        setColumnProperties();
        addValueChangeListener(event -> onValueChange());
        setPageLength(SPUIDefinitions.PAGE_SIZE);
        setSelectable(false);
        setColumnCollapsingAllowed(true);
        setCollapsiblecolumns();
    }

    private void setColumnProperties() {
        final List<TableColumn> columnList = getTableVisibleColumns();
        final List<Object> columnIds = new ArrayList<>();
        for (final TableColumn column : columnList) {
            setColumnHeader(column.getColumnPropertyId(), column.getColumnHeader());
            setColumnExpandRatio(column.getColumnPropertyId(), column.getExpandRatio());
            columnIds.add(column.getColumnPropertyId());
        }
        setVisibleColumns(columnIds.toArray());
    }

    private void addNewContainerDS() {
        final Container container = createContainer();
        addContainerProperties(container);
        setContainerDataSource(container);
        int size = 0;
        if (container != null) {
            size = container.size();
        }
        // final int size = container.size();
        if (size == 0) {
            setData(SPUIDefinitions.NO_DATA);
        }
    }

    /**
     * Based on table state (max/min) columns to be shown are returned.
     * 
     * @return List<TableColumn> list of visible columns
     */
    protected abstract List<TableColumn> getTableVisibleColumns();

    /**
     * Create container of the data to be displayed by the table.
     */
    protected abstract Container createContainer();

    /**
     * Add container properties to the container passed in the reference.
     * 
     * @param container
     *            reference of {@link Container}
     */
    protected abstract void addContainerProperties(Container container);

    /**
     * Get Id of the table.
     * 
     * @return Id.
     */
    protected abstract String getTableId();

    /**
     * On select of row.
     */
    protected abstract void onValueChange();

    /**
     * Add any generated columns if required.
     */
    protected abstract void addCustomGeneratedColumns();

    /**
     * Set list of columns collapsible.
     */
    protected abstract void setCollapsiblecolumns();
}
