/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.footer;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Parent class for footer layout.
 * 
 *
 *
 * 
 */
public abstract class AbstractDeleteActionsLayout extends VerticalLayout implements DropHandler {

    private static final long serialVersionUID = -6047975388519155509L;

    private DragAndDropWrapper deleteWrapper;

    private Button noActionBtn;

    private Window unsavedActionsWindow;

    /**
     * Initialize.
     */
    protected void init() {
        if (hasDeletePermission() || hasUpdatePermission()) {
            createComponents();
            buildLayout();
            reload();
        }
    }

    private void reload() {
        reloadActionCount();
    }

    private void createComponents() {
        if (hasDeletePermission()) {
            deleteWrapper = createDeleteWrapperLayout();
        }
        if (hasUpdatePermission()) {
            noActionBtn = createActionsButton();
        }
    }

    private void buildLayout() {
        final HorizontalLayout dropHintLayout = new HorizontalLayout();
        if (hasCountMessage()) {
            dropHintLayout.addComponent(getCountMessageLabel());
        }
        final HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setSpacing(true);
        hLayout.setSizeUndefined();
        if (hasDeletePermission()) {
            hLayout.addComponent(deleteWrapper);
            hLayout.setComponentAlignment(deleteWrapper, Alignment.BOTTOM_LEFT);
        }
        if (hasUpdatePermission()) {
            hLayout.addComponent(noActionBtn);
            hLayout.setComponentAlignment(noActionBtn, Alignment.BOTTOM_LEFT);
        }
        addComponent(dropHintLayout);
        addComponent(hLayout);
        setComponentAlignment(dropHintLayout, Alignment.BOTTOM_CENTER);
        setComponentAlignment(hLayout, Alignment.BOTTOM_CENTER);
        setStyleName("footer-layout");
        setWidth("100%");

    }

    private DragAndDropWrapper createDeleteWrapperLayout() {
        final Button dropToDelete = new Button("Drop here to delete");
        dropToDelete.setCaptionAsHtml(true);
        dropToDelete.setIcon(FontAwesome.TRASH_O);
        dropToDelete.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        dropToDelete.addStyleName("drop-to-delete-button");
        dropToDelete.addStyleName("action-button");
        dropToDelete.addStyleName("del-action-button");
        dropToDelete.addStyleName("delete-icon");

        final DragAndDropWrapper wrapper = new DragAndDropWrapper(dropToDelete);
        wrapper.setStyleName(ValoTheme.BUTTON_PRIMARY);
        wrapper.setId(getDeleteAreaId());
        wrapper.setDropHandler(this);
        wrapper.addStyleName("delete-button-border");
        wrapper.addStyleName(SPUIStyleDefinitions.SHOW_DELETE_DROP_HINT);
        return wrapper;
    }

    private Button createActionsButton() {
        final Button button = SPUIComponentProvider.getButton(SPUIComponetIdProvider.PENDING_ACTION_BUTTON,
                getNoActionsButtonLabel(), "", "", false, null, SPUIButtonStyleSmall.class);
        button.setIcon(FontAwesome.BELL);
        button.setStyleName("action-button");
        button.addStyleName("del-action-button");

        button.addClickListener(event -> actionButtonClicked());
        button.setHtmlContentAllowed(true);
        return button;
    }

    protected void actionButtonClicked() {
        if (hasUnsavedActions()) {
            unsavedActionsWindow = SPUIComponentProvider.getWindow(getUnsavedActionsWindowCaption(),
                    SPUIComponetIdProvider.SAVE_ACTIONS_POPUP, SPUIDefinitions.CONFIRMATION_WINDOW);
            unsavedActionsWindow.addCloseListener(event -> unsavedActionsWindowClosed());
            unsavedActionsWindow.setContent(getUnsavedActionsWindowContent());
            UI.getCurrent().addWindow(unsavedActionsWindow);
        }
    }

    /**
     * It will close the unsaved actions window.
     */
    protected void closeUnsavedActionsWindow() {
        UI.getCurrent().removeWindow(unsavedActionsWindow);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.event.dd.DropHandler#getAcceptCriterion()
     */
    @Override
    public AcceptCriterion getAcceptCriterion() {
        return getDeleteLayoutAcceptCriteria();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.event.dd.DropHandler#drop(com.vaadin.event.dd.
     * DragAndDropEvent)
     */
    @Override
    public void drop(final DragAndDropEvent event) {
        processDroppedComponent(event);
    }

    /**
     * Update the pending actions count.
     * 
     * @param newCount
     *            new count value.
     */
    protected void updateActionsCount(final int newCount) {
        if (newCount > 0) {
            noActionBtn.setCaption(getActionsButtonLabel() + "<div class='unread'>" + newCount + "</div>");
        } else {
            noActionBtn.setCaption(getNoActionsButtonLabel());
        }
    }

    /**
     * Hide the drop hints.
     */
    protected void hideDropHints() {
        if (hasDeletePermission()) {
            Page.getCurrent().getJavaScript().execute(HawkbitCommonUtil.hideDeleteDropHintScript());
        }
    }

    /**
     * show the drop hints.
     */
    protected void showDropHints() {
        if (hasDeletePermission()) {
            Page.getCurrent().getJavaScript().execute(HawkbitCommonUtil.dispDeleteDropHintScript());
        }
    }

    /**
     * Check user has delete permission.
     * 
     * @return true if user has permission, otherwise return false.
     */
    protected abstract boolean hasDeletePermission();

    /**
     * Check if user has update permission.
     * 
     * @return true if user has permission, otherwise return false.
     */
    protected abstract boolean hasUpdatePermission();

    /**
     * Get label for delete drop area.
     * 
     * @return label of delete drop area.
     */
    protected abstract String getDeleteAreaLabel();

    /**
     * Get Id of the delete drop area.
     * 
     * @return Id of the delete drop area.
     */
    protected abstract String getDeleteAreaId();

    /**
     * Get the accept criteria for the delete layout drop.
     * 
     * @return reference of {@link AcceptCriteria}
     */
    protected abstract AcceptCriterion getDeleteLayoutAcceptCriteria();

    /**
     * Process the dropped component.
     * 
     * @param event
     *            reference of {@link DragAndDropEvent}
     */
    protected abstract void processDroppedComponent(DragAndDropEvent event);

    /**
     * Get the no actions button label.
     * 
     * @return the no actions label.
     */
    protected abstract String getNoActionsButtonLabel();

    /**
     * Get the pending actions button label.
     * 
     * @return the actions label.
     */
    protected abstract String getActionsButtonLabel();

    /**
     * reload the count value.
     */
    protected abstract void reloadActionCount();

    /**
     * Get caption of unsaved actions window.
     * 
     * @return caption of the window.
     */
    protected abstract String getUnsavedActionsWindowCaption();

    /**
     * This method will be called when unsaved actions window is closed.
     */
    protected abstract void unsavedActionsWindowClosed();

    /**
     * Get the content to be displayed in unsaved actions window.
     * 
     * @return reference of the component.
     */
    protected abstract Component getUnsavedActionsWindowContent();

    /**
     * Check if any unsaved actions done by the user.
     * 
     * @return 'true' if any unsaved actions available, otherwise retrun
     *         'false'.
     */
    protected abstract boolean hasUnsavedActions();

    /**
     * Only in deployment view count message is displayed.
     * 
     * @return
     */
    protected abstract boolean hasCountMessage();

    protected abstract Label getCountMessageLabel();

}
