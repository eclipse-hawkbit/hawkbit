/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.confirmwindow.layout;

import com.vaadin.data.Container;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Confirmation tab of confirmation window.
 */
public class ConfirmationTab extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private Table table;

    private Container container;

    private Object[] visibleColumns;

    private String[] columnHeaders;

    /**
     * Default constructor.
     */
    public ConfirmationTab(final Container container, final Object[] visibleColumns, final String[] columnHeaders) {
        this.container = container;
        this.visibleColumns = visibleColumns;
        this.columnHeaders = columnHeaders;
        createComponents();
        buildLayout();
    }

    private void createComponents() {
        createTable();
    }

    private void buildLayout() {
        setSizeFull();
        setMargin(false);
        setSpacing(false);

        addComponent(table);
        setComponentAlignment(table, Alignment.MIDDLE_CENTER);
    }

    private void createTable() {
        table = new Table();
        table.setSizeFull();
        table.setPageLength(1);
        table.setSortEnabled(false);
        table.setContainerDataSource(container);
        table.setVisibleColumns(visibleColumns);
        table.setColumnHeaders(columnHeaders);
        // Build Style
        final StringBuilder style = new StringBuilder(ValoTheme.TABLE_COMPACT);
        style.append(' ');
        style.append(ValoTheme.TABLE_SMALL);
        style.append(' ');
        // Set style
        table.addStyleName(style.toString());
        table.addStyleName("accordion-tab-table-style");
        table.setImmediate(true);
    }

    /**
     * @return the table
     */
    public Table getTable() {
        return table;
    }

    /**
     * @param table
     *            the table to set
     */
    public void setTable(final Table table) {
        this.table = table;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(final Container container) {
        this.container = container;
    }

    public Object[] getVisibleColumns() {
        return visibleColumns;
    }

    public void setVisibleColumns(final Object[] visibleColumns) {
        this.visibleColumns = visibleColumns;
    }

    public String[] getColumnHeaders() {
        return columnHeaders;
    }

    public void setColumnHeaders(final String[] columnHeaders) {
        this.columnHeaders = columnHeaders;
    }

}
