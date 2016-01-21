/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.table;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;

import com.vaadin.data.Container;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Parent class for table.
 * 
 *
 *
 */
public abstract class AbstractTable extends Table {

    private static final long serialVersionUID = 4856562746502217630L;

    /**
     * Initialize the components.
     */
    protected void init() {
        setStyleName("sp-table");
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
        setDefault();
        addValueChangeListener(event -> onValueChange());
        selectRow();
        setPageLength(SPUIDefinitions.PAGE_SIZE);
    }

    private void setDefault() {
        setSelectable(true);
        setMultiSelect(true);
        setDragMode(TableDragMode.MULTIROW);
        setColumnCollapsingAllowed(false);
        setDropHandler(getTableDropHandler());
    }

    private void addNewContainerDS() {
        final Container container = createContainer();
        addContainerProperties(container);
        setContainerDataSource(container);
        final int size = container.size();
        if (size == 0) {
            setData(SPUIDefinitions.NO_DATA);
        }
    }

    protected void selectRow() {
        if (!isMaximized()) {
            if (isFirstRowSelectedOnLoad()) {
                selectFirstRow();
            } else {
                setValue(getItemIdToSelect());
            }
        }
    }

    private void setColumnProperties() {
        final List<TableColumn> columnList = getTableVisibleColumns();
        final List<Object> swColumnIds = new ArrayList<>();
        for (final TableColumn column : columnList) {
            setColumnHeader(column.getColumnPropertyId(), column.getColumnHeader());
            setColumnExpandRatio(column.getColumnPropertyId(), column.getExpandRatio());
            swColumnIds.add(column.getColumnPropertyId());
        }
        setVisibleColumns(swColumnIds.toArray());
    }

    private void selectFirstRow() {
        final Container container = getContainerDataSource();
        final int size = container.size();
        if (size > 0) {
            select(firstItemId());
        }
    }

    protected void applyMaxTableSettings() {
        setColumnProperties();
        setValue(null);
        setSelectable(false);
        setMultiSelect(false);
        setDragMode(TableDragMode.NONE);
        setColumnCollapsingAllowed(true);
    }

    protected void applyMinTableSettings() {
        setDefault();
        setColumnProperties();
        selectRow();
    }

    protected void refreshFilter() {
        addNewContainerDS();
        setColumnProperties();
        selectRow();
    }

    /**
     * Get Id of the table.
     * 
     * @return Id.
     */
    protected abstract String getTableId();

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
     * Add any generated columns if required.
     */
    protected abstract void addCustomGeneratedColumns();

    /**
     * Check if first row should be selected by default on load.
     * 
     * @return true if it should be selected otherwise return false.
     */
    protected abstract boolean isFirstRowSelectedOnLoad();

    /**
     * Get Item Id should be displayed as selected.
     * 
     * @return reference of Item Id of the Row.
     */
    protected abstract Object getItemIdToSelect();

    /**
     * On select of row.
     */
    protected abstract void onValueChange();

    /**
     * Check if the table is maximized or minimized.
     * 
     * @return true if maximized, otherwise false.
     */
    protected abstract boolean isMaximized();

    /**
     * Based on table state (max/min) columns to be shown are returned.
     * 
     * @return List<TableColumn> list of visible columns
     */
    protected abstract List<TableColumn> getTableVisibleColumns();

    /**
     * Get drop handler for the table.
     * 
     * @return reference of {@link DropHandler}
     */
    protected abstract DropHandler getTableDropHandler();

}
