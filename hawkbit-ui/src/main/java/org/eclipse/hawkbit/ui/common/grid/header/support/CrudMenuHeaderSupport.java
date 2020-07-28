/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.header.support;

import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Component;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Header support for CRUD menu
 */
public class CrudMenuHeaderSupport implements HeaderSupport {
    private final VaadinMessageSource i18n;

    private final String crudMenuBarId;
    private final boolean hasCreatePermission;
    private final boolean hasUpdatePermission;
    private final boolean hasDeletePermission;
    private final Runnable addCallback;
    private final Runnable editCallback;
    private final Runnable deleteCallback;
    private final Runnable closeCallback;

    private final MenuBar crudMenuBar;
    private final MenuItem crudMenuItem;

    private Mode currentMode;

    /**
     * Constructor for CrudMenuHeaderSupport
     *
     * @param i18n
     *            VaadinMessageSource
     * @param crudMenuBarId
     *            Crud menu bar id
     * @param hasCreatePermission
     *            boolean value based on create permission
     * @param hasUpdatePermission
     *            boolean value based on update permission
     * @param hasDeletePermission
     *            boolean value based on delete permission
     * @param addCallback
     *            Runnable
     * @param editCallback
     *            Runnable
     * @param deleteCallback
     *            Runnable
     * @param closeCallback
     *            Runnable
     */
    public CrudMenuHeaderSupport(final VaadinMessageSource i18n, final String crudMenuBarId,
            final boolean hasCreatePermission, final boolean hasUpdatePermission, final boolean hasDeletePermission,
            final Runnable addCallback, final Runnable editCallback, final Runnable deleteCallback,
            final Runnable closeCallback) {
        this.i18n = i18n;

        this.crudMenuBarId = crudMenuBarId;
        this.hasCreatePermission = hasCreatePermission;
        this.hasUpdatePermission = hasUpdatePermission;
        this.hasDeletePermission = hasDeletePermission;
        this.addCallback = addCallback;
        this.editCallback = editCallback;
        this.deleteCallback = deleteCallback;
        this.closeCallback = closeCallback;

        this.crudMenuBar = createCrudMenuBar();
        this.crudMenuItem = crudMenuBar.addItem("");

        addCrudMenuItemCommands();
        activateSelectMode();
    }

    private MenuBar createCrudMenuBar() {
        final MenuBar menuBar = new MenuBar();

        menuBar.setId(crudMenuBarId);
        menuBar.setStyleName(ValoTheme.MENUBAR_BORDERLESS);
        menuBar.addStyleName("crud-menubar");

        return menuBar;
    }

    private void addCrudMenuItemCommands() {
        if (hasCreatePermission) {
            crudMenuItem.addItem(i18n.getMessage(UIMessageIdProvider.CAPTION_CONFIG_CREATE), VaadinIcons.PLUS,
                    menuItem -> addCallback.run());
        }
        if (hasUpdatePermission) {
            crudMenuItem.addItem(i18n.getMessage(UIMessageIdProvider.CAPTION_CONFIG_EDIT), VaadinIcons.EDIT,
                    menuItem -> {
                        activateModifyMode(Mode.EDIT);
                        editCallback.run();
                    });
        }
        if (hasDeletePermission) {
            crudMenuItem.addItem(i18n.getMessage(UIMessageIdProvider.CAPTION_CONFIG_DELETE), VaadinIcons.TRASH,
                    menuItem -> {
                        activateModifyMode(Mode.DELETE);
                        deleteCallback.run();
                    });
        }

        // in case of missing CUD permissions we disable the menu button
        if (CollectionUtils.isEmpty(crudMenuItem.getChildren())) {
            crudMenuItem.setEnabled(false);
        }
    }

    private void activateSelectMode() {
        crudMenuItem.setIcon(VaadinIcons.COG);
        crudMenuItem.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_CONFIGURE));
        crudMenuItem.setCommand(null);

        currentMode = Mode.SELECT;
        crudMenuBar.removeStyleNames(Mode.EDIT.getStyle(), Mode.DELETE.getStyle());
    }

    private void activateModifyMode(final Mode mode) {
        crudMenuItem.setIcon(VaadinIcons.CLOSE_CIRCLE);
        crudMenuItem.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_CONFIGURE_CLOSE));
        crudMenuItem.setCommand(menuItem -> resetModifyMode());

        currentMode = mode;
        crudMenuBar.addStyleName(mode.getStyle());
    }

    private void resetModifyMode() {
        if (isModifyModeActive()) {
            activateSelectMode();
            closeCallback.run();
        }
    }

    private boolean isModifyModeActive() {
        return Mode.EDIT == currentMode || Mode.DELETE == currentMode;
    }

    @Override
    public Component getHeaderComponent() {
        return crudMenuBar;
    }

    /**
     * Enable crud menu option
     */
    public void enableCrudMenu() {
        crudMenuBar.setEnabled(true);
    }

    /**
     * Disable crud menu option
     */
    public void disableCrudMenu() {
        resetModifyMode();
        crudMenuBar.setEnabled(false);
    }

    private enum Mode {
        SELECT(""), EDIT("mode-edit"), DELETE("mode-delete");

        private String style;

        Mode(final String style) {
            this.style = style;
        }

        private String getStyle() {
            return style;
        }
    }
}
