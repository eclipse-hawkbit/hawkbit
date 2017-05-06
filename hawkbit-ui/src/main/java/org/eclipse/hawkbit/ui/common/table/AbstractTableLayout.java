/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.table;

import org.eclipse.hawkbit.ui.common.detailslayout.AbstractTableDetailsLayout;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.utils.ShortCutModifierUtils;

import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Parent class for table layout.
 * 
 *
 * @param <T>
 *            type of the concrete table
 */
public abstract class AbstractTableLayout<T extends AbstractTable<?>> extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private AbstractTableHeader tableHeader;

    private T table;

    private AbstractTableDetailsLayout<?> detailsLayout;

    protected void init(final AbstractTableHeader tableHeader, final T table,
            final AbstractTableDetailsLayout<?> detailsLayout) {
        this.tableHeader = tableHeader;
        this.table = table;
        this.detailsLayout = detailsLayout;
        buildLayout();

        table.selectRow();
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
        if (isShortCutKeysRequired()) {
            final Panel tablePanel = new Panel();
            tablePanel.setStyleName("table-panel");
            tablePanel.setHeight(100.0F, Unit.PERCENTAGE);
            tablePanel.setContent(table);
            tablePanel.addActionHandler(getShortCutKeysHandler());
            tablePanel.addStyleName(ValoTheme.PANEL_BORDERLESS);
            tableHeaderLayout.addComponent(tablePanel);
            tableHeaderLayout.setComponentAlignment(tablePanel, Alignment.TOP_CENTER);
            tableHeaderLayout.setExpandRatio(tablePanel, 1.0F);
        } else {
            tableHeaderLayout.addComponent(table);
            tableHeaderLayout.setComponentAlignment(table, Alignment.TOP_CENTER);
            tableHeaderLayout.setExpandRatio(table, 1.0F);
        }

        addComponent(tableHeaderLayout);
        addComponent(detailsLayout);
        setComponentAlignment(tableHeaderLayout, Alignment.TOP_CENTER);
        setComponentAlignment(detailsLayout, Alignment.TOP_CENTER);
        setExpandRatio(tableHeaderLayout, 1.0F);
    }

    /**
     * If any short cut keys required on the table.
     * 
     * @return true if required else false. Default is 'true'.
     */
    protected boolean isShortCutKeysRequired() {
        return true;
    }

    /**
     * Get the action handler for the short cut keys.
     * 
     * @return reference of {@link Handler} to handler the short cut keys.
     *         Default is null.
     */
    protected Handler getShortCutKeysHandler() {
        return new TableShortCutHandler();
    }

    protected void publishEvent() {
        // can be override by subclasses
    }

    public void setShowFilterButtonVisible(final boolean visible) {
        tableHeader.setFilterButtonsIconVisible(visible);
    }

    public RefreshableContainer getTable() {
        return table;
    }

    private class TableShortCutHandler implements Handler {

        private static final String SELECT_ALL_TEXT = "Select All";
        private final ShortcutAction selectAllAction = new ShortcutAction(SELECT_ALL_TEXT, ShortcutAction.KeyCode.A,
                new int[] { ShortCutModifierUtils.getCtrlOrMetaModifier() });

        private static final long serialVersionUID = 1L;

        @Override
        public void handleAction(final Action action, final Object sender, final Object target) {
            if (!selectAllAction.equals(action)) {
                return;
            }
            table.selectAll();
            publishEvent();
        }

        @Override
        public Action[] getActions(final Object target, final Object sender) {
            return new Action[] { selectAllAction };
        }
    }

}
