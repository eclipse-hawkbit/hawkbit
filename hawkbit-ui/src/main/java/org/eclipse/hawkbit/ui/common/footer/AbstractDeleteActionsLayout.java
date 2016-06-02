/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.footer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

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
 */
public abstract class AbstractDeleteActionsLayout extends VerticalLayout implements DropHandler {

    private static final long serialVersionUID = -6047975388519155509L;

    @Autowired
    protected I18N i18n;

    @Autowired
    protected SpPermissionChecker permChecker;

    @Autowired
    protected transient EventBus.SessionEventBus eventBus;

    @Autowired
    protected transient UINotification notification;

    private DragAndDropWrapper deleteWrapper;

    private Button noActionBtn;

    private Window unsavedActionsWindow;

    private Button bulkUploadStatusButton;

    /**
     * Initialize.
     */
    @PostConstruct
    protected void init() {
        if (hasCountMessage() || hasDeletePermission() || hasUpdatePermission() || hasBulkUploadPermission()) {
            createComponents();
            buildLayout();
            reload();
        }
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    private void reload() {
        restoreActionCount();
        restoreBulkUploadStatusCount();
    }

    private void createComponents() {
        if (hasDeletePermission()) {
            deleteWrapper = createDeleteWrapperLayout();
        }
        if (hasDeletePermission() || hasUpdatePermission()) {
            noActionBtn = createActionsButton();
        }
        if (hasBulkUploadPermission()) {
            bulkUploadStatusButton = createBulkUploadStatusButton();
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
        if (null != deleteWrapper) {
            hLayout.addComponent(deleteWrapper);
            hLayout.setComponentAlignment(deleteWrapper, Alignment.BOTTOM_LEFT);
        }
        if (null != noActionBtn) {
            hLayout.addComponent(noActionBtn);
            hLayout.setComponentAlignment(noActionBtn, Alignment.BOTTOM_LEFT);
        }
        if (null != bulkUploadStatusButton) {
            hLayout.addComponent(bulkUploadStatusButton);
            hLayout.setComponentAlignment(bulkUploadStatusButton, Alignment.BOTTOM_LEFT);
        }
        if (dropHintLayout.getComponentCount() > 0) {
            addComponent(dropHintLayout);
            setComponentAlignment(dropHintLayout, Alignment.BOTTOM_CENTER);
        }
        if (hLayout.getComponentCount() > 0) {
            addComponent(hLayout);
            setComponentAlignment(hLayout, Alignment.BOTTOM_CENTER);
        }
        setStyleName(SPUIStyleDefinitions.FOOTER_LAYOUT);
        setWidth("100%");
    }

    private DragAndDropWrapper createDeleteWrapperLayout() {
        final Button dropToDelete = new Button("Drop here to delete");
        dropToDelete.setCaptionAsHtml(true);
        dropToDelete.setIcon(FontAwesome.TRASH_O);
        dropToDelete.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        dropToDelete.addStyleName("drop-to-delete-button");
        dropToDelete.addStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
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
        final Button button = SPUIComponentProvider.getButton(SPUIComponentIdProvider.PENDING_ACTION_BUTTON,
                getNoActionsButtonLabel(), "", "", false, FontAwesome.BELL, SPUIButtonStyleSmall.class);
        button.setStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
        button.addStyleName("del-action-button");

        button.addClickListener(event -> actionButtonClicked());
        button.setHtmlContentAllowed(true);
        return button;
    }

    private Button createBulkUploadStatusButton() {
        final Button button = SPUIComponentProvider.getButton(SPUIComponentIdProvider.BULK_UPLOAD_STATUS_BUTTON, "", "",
                "", false, null, SPUIButtonStyleSmall.class);
        button.setStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
        button.addStyleName(SPUIStyleDefinitions.UPLOAD_PROGRESS_INDICATOR_STYLE);
        button.setWidth("100px");
        button.setHtmlContentAllowed(true);
        button.addClickListener(event -> onClickBulkUploadNotificationButton());
        button.setVisible(false);
        return button;
    }

    private void onClickBulkUploadNotificationButton() {
        hideBulkUploadStatusButton();
        showBulkUploadWindow();
    }

    protected void setUploadStatusButtonCaption(final Long count) {
        if (bulkUploadStatusButton == null) {
            return;
        }
        bulkUploadStatusButton.setCaption("<div class='unread'>" + count + "</div>");
    }

    protected void enableBulkUploadStatusButton() {
        if (bulkUploadStatusButton == null) {
            return;
        }
        bulkUploadStatusButton.setVisible(true);
    }

    protected void updateUploadBtnIconToComplete() {
        if (bulkUploadStatusButton == null) {
            return;
        }
        bulkUploadStatusButton.removeStyleName(SPUIStyleDefinitions.UPLOAD_PROGRESS_INDICATOR_STYLE);
        bulkUploadStatusButton.setIcon(FontAwesome.UPLOAD);
    }

    protected void updateUploadBtnIconToProgressIndicator() {
        if (bulkUploadStatusButton == null) {
            return;
        }
        bulkUploadStatusButton.addStyleName(SPUIStyleDefinitions.UPLOAD_PROGRESS_INDICATOR_STYLE);
        bulkUploadStatusButton.setIcon(null);
    }

    protected void actionButtonClicked() {
        if (!hasUnsavedActions()) {
            return;
        }
        unsavedActionsWindow = SPUIComponentProvider.getWindow(getUnsavedActionsWindowCaption(),
                SPUIComponentIdProvider.SAVE_ACTIONS_POPUP, SPUIDefinitions.CONFIRMATION_WINDOW);
        unsavedActionsWindow.addCloseListener(event -> unsavedActionsWindowClosed());
        unsavedActionsWindow.setContent(getUnsavedActionsWindowContent());
        unsavedActionsWindow.setId(SPUIComponentIdProvider.CONFIRMATION_POPUP_ID);
        UI.getCurrent().addWindow(unsavedActionsWindow);
    }

    /**
     * It will close the unsaved actions window.
     */
    protected void closeUnsavedActionsWindow() {
        UI.getCurrent().removeWindow(unsavedActionsWindow);
    }

    @Override
    public AcceptCriterion getAcceptCriterion() {
        return getDeleteLayoutAcceptCriteria();
    }

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
        if (noActionBtn != null) {
            if (newCount > 0) {
                noActionBtn.setCaption(getActionsButtonLabel() + "<div class='unread'>" + newCount + "</div>");
            } else {
                noActionBtn.setCaption(getNoActionsButtonLabel());
            }
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

    protected void hideBulkUploadStatusButton() {
        if (null != bulkUploadStatusButton) {
            bulkUploadStatusButton.setCaption(null);
            bulkUploadStatusButton.setVisible(false);
        }
    }

    /**
     * 
     * @return true if the count label is displayed false is not displayed
     */
    protected boolean hasCountMessage() {
        return false;
    }

    /**
     * 
     * @return the count message label
     */
    protected Label getCountMessageLabel() {
        return null;
    }

    /**
     * @return true if bulk upload is allowed and has required create
     *         permissions.
     */
    protected boolean hasBulkUploadPermission() {
        // can be overriden
        return false;
    }

    protected void showBulkUploadWindow() {
        // can be overriden
    }

    /**
     * restore the upload status count.
     */
    protected void restoreBulkUploadStatusCount() {
        // can be overriden
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
    protected String getNoActionsButtonLabel() {
        return i18n.get("button.no.actions");
    }

    /**
     * Get the pending actions button label.
     * 
     * @return the actions label.
     */
    protected String getActionsButtonLabel() {
        return i18n.get("button.actions");
    }

    /**
     * Get caption of unsaved actions window.
     * 
     * @return caption of the window.
     */
    protected String getUnsavedActionsWindowCaption() {
        return i18n.get("caption.save.window");
    }

    /**
     * reload the count value.
     */
    protected abstract void restoreActionCount();

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

}
