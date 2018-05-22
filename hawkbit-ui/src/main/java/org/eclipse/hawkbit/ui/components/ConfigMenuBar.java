/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Menubar for configuring tags and types (create, update, delete). The menubar
 * is placed in the header of the filter by tags or types tables.
 *
 */
public class ConfigMenuBar extends MenuBar {

    private static final long serialVersionUID = 1L;

    private MenuItem config;

    private final boolean createPermission;

    private final boolean updatePermission;

    private final boolean deletePermission;

    private final Command addButtonCommand;

    private final Command updateButtonCommand;

    private final Command deleteButtonCommand;

    /**
     * Constructor for the menubar
     * 
     * @param createPermission
     *            flag if the logged-in user has permission to create the entity
     *            which should be configured with this menubar
     * @param updatePermission
     *            flag if the logged-in user has permission to update the entity
     *            which should be configured with this menubar
     * @param deletePermission
     *            flag if the logged-in user has permission to delete the entity
     *            which should be configured with this menubar
     * @param addButtonCommand
     *            action which should be executed when clicking on the create
     *            command
     * @param updateButtonCommand
     *            action which should be executed when clicking on the update
     *            command
     * @param deleteButtonCommand
     *            action which should be executed when clicking on the delete
     *            command
     */
    public ConfigMenuBar(final boolean createPermission, final boolean updatePermission, final boolean deletePermission,
            final Command addButtonCommand, final Command updateButtonCommand, final Command deleteButtonCommand) {
        this.createPermission = createPermission;
        this.updatePermission = updatePermission;
        this.deletePermission = deletePermission;
        this.addButtonCommand = addButtonCommand;
        this.updateButtonCommand = updateButtonCommand;
        this.deleteButtonCommand = deleteButtonCommand;

        init();
    }

    private void init() {
        if (!createPermission && !updatePermission && !deletePermission) {
            return;
        }
        setStyleName(ValoTheme.MENUBAR_BORDERLESS);
        addStyleName("menubar-position");
        config = addItem("", FontAwesome.COG, null);
        config.setStyleName("tags");

        addMenuItems();
    }

    private void addMenuItems() {
        if (createPermission) {
            config.addItem("create", FontAwesome.PLUS, addButtonCommand);
        }
        if (updatePermission) {
            config.addItem("update", FontAwesome.EDIT, updateButtonCommand);
        }
        if (deletePermission) {
            config.addItem("delete", FontAwesome.TRASH_O, deleteButtonCommand);
        }
    }

    public MenuItem getConfig() {
        return config;
    }

}
