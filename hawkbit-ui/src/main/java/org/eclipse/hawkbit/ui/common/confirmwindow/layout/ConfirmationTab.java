/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.confirmwindow.layout;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleTiny;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Confirmation tab of confirmation window.
 * 
 *
 *
 */
public class ConfirmationTab extends VerticalLayout {

    private static final long serialVersionUID = -5211351556595775554L;

    private Table table;

    private Button confirmAll;

    private Button discardAll;

    /**
     * Default constructor.
     */
    public ConfirmationTab() {
        createComponents();
        buildLayout();
    }

    private void createComponents() {
        // Create table
        createTable();

        // Create confirm all button
        createConfirmAllButton();

        // Create discard all button
        createDiscardAllButton();

    }

    private void buildLayout() {
        final HorizontalLayout btnLayout = new HorizontalLayout();
        btnLayout.setSpacing(true);

        btnLayout.addStyleName(SPUIStyleDefinitions.SP_ACCORDION_TAB_BTN);
        btnLayout.addComponent(confirmAll);
        btnLayout.setComponentAlignment(confirmAll, Alignment.MIDDLE_CENTER);
        btnLayout.addComponent(discardAll);
        btnLayout.setComponentAlignment(discardAll, Alignment.MIDDLE_CENTER);

        setSizeFull();
        setMargin(false);
        setSpacing(false);

        addComponent(table);
        setComponentAlignment(table, Alignment.MIDDLE_CENTER);
        addComponent(btnLayout);
        setComponentAlignment(btnLayout, Alignment.MIDDLE_CENTER);
    }

    private void createDiscardAllButton() {
        discardAll = SPUIComponentProvider.getButton(null, SPUILabelDefinitions.DISCARD_ALL, "", null, false, null,
                SPUIButtonStyleTiny.class);
        discardAll.setIcon(FontAwesome.REPLY);
    }

    private void createConfirmAllButton() {
        confirmAll = SPUIComponentProvider.getButton(null, "", "", ValoTheme.BUTTON_PRIMARY, false, null,
                SPUIButtonStyleTiny.class);
        confirmAll.setIcon(FontAwesome.REPLY);
    }

    private void createTable() {
        table = new Table();
        table.setSizeFull();
        table.setPageLength(SPUIDefinitions.ACCORDION_TAB_DETAILS_PAGE_LENGTH);
        // Build Style
        final StringBuilder style = new StringBuilder(ValoTheme.TABLE_COMPACT);
        style.append(' ');
        style.append(ValoTheme.TABLE_SMALL);
        style.append(' ');
        style.append(ValoTheme.TABLE_NO_VERTICAL_LINES);
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

    /**
     * @return the confirmAll
     */
    public Button getConfirmAll() {
        return confirmAll;
    }

    /**
     * @param confirmAll
     *            the confirmAll to set
     */
    public void setConfirmAll(final Button confirmAll) {
        this.confirmAll = confirmAll;
    }

    /**
     * @return the discardAll
     */
    public Button getDiscardAll() {
        return discardAll;
    }

    /**
     * @param discardAll
     *            the discardAll to set
     */
    public void setDiscardAll(final Button discardAll) {
        this.discardAll = discardAll;
    }

}
