/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.header;

import java.util.Arrays;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.event.ActionsVisibilityEventPayload;
import org.eclipse.hawkbit.ui.common.event.ActionsVisibilityEventPayload.ActionsVisibilityType;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.LayoutVisibilityEventPayload;
import org.eclipse.hawkbit.ui.common.event.LayoutVisibilityEventPayload.VisibilityType;
import org.eclipse.hawkbit.ui.common.grid.header.support.CloseHeaderSupport;
import org.eclipse.hawkbit.ui.common.grid.header.support.CrudMenuHeaderSupport;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Abstract filter header.
 */
public abstract class AbstractFilterHeader extends AbstractGridHeader {
    private static final long serialVersionUID = 1L;

    private final transient CrudMenuHeaderSupport crudMenuHeaderSupport;
    private final transient CloseHeaderSupport closeHeaderSupport;

    /**
     * Constructor for AbstractFilterHeader
     *
     * @param i18n
     *            VaadinMessageSource
     * @param permChecker
     *            SpPermissionChecker
     * @param eventBus
     *            UIEventBus
     */
    protected AbstractFilterHeader(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventBus) {
        super(i18n, permChecker, eventBus);

        this.crudMenuHeaderSupport = new CrudMenuHeaderSupport(i18n, getCrudMenuBarId(),
                permChecker.hasCreateRepositoryPermission(), permChecker.hasUpdateRepositoryPermission(),
                permChecker.hasDeleteRepositoryPermission(), this::addNewItem, this::publishShowEditMode,
                this::publishShowDeleteMode, this::publishHideAllMode);
        this.closeHeaderSupport = new CloseHeaderSupport(i18n, getCloseIconId(), this::hideFilterLayout);
        addHeaderSupports(Arrays.asList(crudMenuHeaderSupport, closeHeaderSupport));
    }

    @Override
    protected Component getHeaderCaption() {
        return SPUIComponentProvider.generateCaptionLabel(i18n, getHeaderCaptionMsgKey());
    }

    protected abstract String getHeaderCaptionMsgKey();

    protected abstract String getCrudMenuBarId();

    private void addNewItem() {
        final Window addWindow = getWindowForAdd();

        addWindow.setCaption(i18n.getMessage("caption.create.new", i18n.getMessage(getAddEntityWindowCaptionMsgKey())));
        UI.getCurrent().addWindow(addWindow);
        addWindow.setVisible(Boolean.TRUE);
    }

    protected abstract Window getWindowForAdd();

    protected abstract String getAddEntityWindowCaptionMsgKey();

    private void publishShowEditMode() {
        eventBus.publish(CommandTopics.CHANGE_GRID_ACTIONS_VISIBILITY, this,
                new ActionsVisibilityEventPayload(ActionsVisibilityType.SHOW_EDIT, getLayout(), getView()));
    }

    private void publishShowDeleteMode() {
        eventBus.publish(CommandTopics.CHANGE_GRID_ACTIONS_VISIBILITY, this,
                new ActionsVisibilityEventPayload(ActionsVisibilityType.SHOW_DELETE, getLayout(), getView()));
    }

    private void publishHideAllMode() {
        eventBus.publish(CommandTopics.CHANGE_GRID_ACTIONS_VISIBILITY, this,
                new ActionsVisibilityEventPayload(ActionsVisibilityType.HIDE_ALL, getLayout(), getView()));
    }

    protected abstract String getCloseIconId();

    private void hideFilterLayout() {
        eventBus.publish(CommandTopics.CHANGE_LAYOUT_VISIBILITY, this,
                new LayoutVisibilityEventPayload(VisibilityType.HIDE, getLayout(), getView()));

        updateHiddenUiState();
    }

    protected abstract EventLayout getLayout();

    protected abstract EventView getView();

    protected abstract void updateHiddenUiState();

    /**
     * Enable crud menu option
     */
    public void enableCrudMenu() {
        crudMenuHeaderSupport.enableCrudMenu();
    }

    /**
     * Disable crud menu option
     */
    public void disableCrudMenu() {
        crudMenuHeaderSupport.disableCrudMenu();
    }
}
