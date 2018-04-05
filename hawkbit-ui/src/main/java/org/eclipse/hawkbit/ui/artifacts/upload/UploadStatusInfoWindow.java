/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent.UploadStatusEventType;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.renderers.ProgressBarRenderer;
import com.vaadin.ui.themes.ValoTheme;

import elemental.json.JsonValue;

/**
 * Shows upload status during upload.
 */
public class UploadStatusInfoWindow extends Window {

    private static final Logger LOG = LoggerFactory.getLogger(UploadStatusInfoWindow.class);

    private static final String ARTIFACT_UPLOAD_EXCEPTION = "Artifact upload exception:";

    private final transient EventBus.UIEventBus eventBus;

    private final ArtifactUploadState artifactUploadState;

    private final VaadinMessageSource i18n;

    private static final String PROGRESS = "Progress";

    private static final String FILE_NAME = "File name";

    private static final String STATUS = "Status";

    private static final String REASON = "Reason";

    private static final String STATUS_INPROGRESS = "InProgress";

    private static final String STATUS_FINISHED = "Finished";

    private static final String STATUS_FAILED = "Failed";

    private static final long serialVersionUID = 1L;

    private final Grid grid;

    private final IndexedContainer uploads;

    private final VerticalLayout mainLayout;

    private Label windowCaption;

    private Button minimizeButton;

    private Button closeButton;

    private Button resizeButton;

    private final UI ui;

    private ConfirmationDialog confirmDialog;

    private final ArtifactManagement artifactManagement;

    private final SoftwareModuleManagement softwareModuleManagement;

    private final UploadLogic uploadLogic;

    UploadStatusInfoWindow(final UIEventBus eventBus, final ArtifactUploadState artifactUploadState,
            final VaadinMessageSource i18n, final ArtifactManagement artifactManagement,
            final SoftwareModuleManagement softwareModuleManagement, final UploadLogic uploadLogic) {
        this.eventBus = eventBus;
        this.artifactUploadState = artifactUploadState;
        this.i18n = i18n;
        this.artifactManagement = artifactManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.uploadLogic = uploadLogic;

        setPopupProperties();
        createStatusPopupHeaderComponents();

        mainLayout = new VerticalLayout();
        mainLayout.setSpacing(Boolean.TRUE);
        mainLayout.setSizeUndefined();
        setPopupSizeInMinMode();

        uploads = getGridContainer();
        grid = createGrid();
        setGridColumnProperties();

        mainLayout.addComponents(getCaptionLayout(), grid);
        mainLayout.setExpandRatio(grid, 1.0F);
        setContent(mainLayout);
        eventBus.subscribe(this);
        ui = UI.getCurrent();

        createConfirmDialog();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final UploadStatusEvent event) {

        event.getFileUploadProgress();
        switch (event.getUploadStatusEventType()) {
        case UPLOAD_STARTED:
            ui.access(() -> onUploadStarted(event));
            break;
        case UPLOAD_IN_PROGRESS:
            ui.access(() -> onUploadInProgress(event));
            break;
        case UPLOAD_FAILED:
            ui.access(() -> onUploadFailure(event));
            break;
        case UPLOAD_SUCCESSFUL:
            onUploadSuccess(event);
            break;
        case UPLOAD_FINISHED:
            ui.access(() -> onUploadFinished(event));
            break;
        default:
            break;
        }
    }

    private void onUploadStarted(final UploadStatusEvent event) {
        final FileUploadProgress fileUploadProgress = event.getFileUploadProgress();
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();
        
        uploadLogic.uploadStarted(fileUploadId, fileUploadProgress);
        updateUploadStatusInfoRowObject(fileUploadId);
        
        if (isFirstFileUpload()) {
            maximizeWindow();
        }
 
        grid.scrollTo(fileUploadId);
    }   

    private void restoreState() {
        final Indexed container = grid.getContainerDataSource();
        container.removeAllItems();
        for (final FileUploadId fileUploadId : artifactUploadState.getAllFilesFromOverallUploadProcessList().keySet()) {
            updateUploadStatusInfoRowObject(fileUploadId);
        }
    }

    private void setPopupProperties() {
        setId(UIComponentIdProvider.UPLOAD_STATUS_POPUP_ID);
        addStyleName(SPUIStyleDefinitions.UPLOAD_INFO);
        setImmediate(true);
        setResizable(false);
        setDraggable(true);
        setClosable(false);
        setModal(true);
    }

    private void setGridColumnProperties() {
        grid.getColumn(STATUS).setRenderer(new StatusRenderer());
        grid.getColumn(PROGRESS).setRenderer(new ProgressBarRenderer());
        grid.setColumnOrder(STATUS, PROGRESS, FILE_NAME, SPUILabelDefinitions.NAME_VERSION, REASON);
        setColumnWidth();
        grid.getColumn(SPUILabelDefinitions.NAME_VERSION).setHeaderCaption(i18n.getMessage("upload.swModuleTable.header"));
        grid.setFrozenColumnCount(5);
    }

    private Grid createGrid() {
        final Grid statusGrid = new Grid(uploads);
        statusGrid.addStyleName(SPUIStyleDefinitions.UPLOAD_STATUS_GRID);
        statusGrid.setSelectionMode(SelectionMode.NONE);
        statusGrid.setHeaderVisible(true);
        statusGrid.setImmediate(true);
        statusGrid.setSizeFull();
        return statusGrid;
    }

    private IndexedContainer getGridContainer() {
        final IndexedContainer uploadContainer = new IndexedContainer();
        uploadContainer.addContainerProperty(STATUS, String.class, "Active");
        uploadContainer.addContainerProperty(FILE_NAME, String.class, null);
        uploadContainer.addContainerProperty(PROGRESS, Double.class, 0D);
        uploadContainer.addContainerProperty(REASON, String.class, "");
        uploadContainer.addContainerProperty(SPUILabelDefinitions.NAME_VERSION, String.class, "");
        return uploadContainer;
    }

    private HorizontalLayout getCaptionLayout() {
        final HorizontalLayout captionLayout = new HorizontalLayout();
        captionLayout.setSizeFull();
        captionLayout.setHeight("36px");
        captionLayout.addComponents(windowCaption, minimizeButton, resizeButton, closeButton);
        captionLayout.setExpandRatio(windowCaption, 1.0F);
        captionLayout.addStyleName("v-window-header");
        return captionLayout;
    }

    private void createStatusPopupHeaderComponents() {
        minimizeButton = getMinimizeButton();
        windowCaption = new Label("Upload status");
        closeButton = getCloseButton();
        resizeButton = getResizeButton();
    }

    private void setColumnWidth() {
        grid.getColumn(STATUS).setWidth(60);
        grid.getColumn(PROGRESS).setWidth(150);
        grid.getColumn(FILE_NAME).setWidth(200);
        grid.getColumn(REASON).setWidth(290);
        grid.getColumn(SPUILabelDefinitions.NAME_VERSION).setWidth(200);
    }

    private void resetColumnWidth() {
        grid.getColumn(STATUS).setWidthUndefined();
        grid.getColumn(PROGRESS).setWidthUndefined();
        grid.getColumn(FILE_NAME).setWidthUndefined();
        grid.getColumn(REASON).setWidthUndefined();
        grid.getColumn(SPUILabelDefinitions.NAME_VERSION).setWidthUndefined();
    }

    private static class StatusRenderer extends HtmlRenderer {

        private static final long serialVersionUID = -5365795450234970943L;

        @Override
        public JsonValue encode(final String value) {
            if (value == null) {
                return super.encode(getNullRepresentation());
            }

            final String result;
            switch (value) {
            case STATUS_FINISHED:
                result = "<div class=\"statusIconGreen\">" + FontAwesome.CHECK_CIRCLE.getHtml() + "</div>";
                break;
            case STATUS_FAILED:
                result = "<div class=\"statusIconRed\">" + FontAwesome.EXCLAMATION_CIRCLE.getHtml() + "</div>";
                break;
            case STATUS_INPROGRESS:
                result = "<div class=\"statusIconActive\"></div>";
                break;
            default:
                throw new IllegalArgumentException("Argument " + value + " wasn't expected.");
            }

            return super.encode(result);
        }
    }

    private boolean isFirstFileUpload() {
        return artifactUploadState.getAllFilesFromOverallUploadProcessList().size() == 1;
    }

    private void openWindow() {
        UI.getCurrent().addWindow(this);
        center();
    }

    protected void maximizeWindow() {
        openWindow();
        restoreState();
        artifactUploadState.setStatusPopupMinimized(false);
    }

    private void minimizeWindow() {
        artifactUploadState.setStatusPopupMinimized(true);
        closeWindow();

        if (uploadLogic.isUploadComplete()) {
            cleanupStates();
        }
    }

    private void onUploadInProgress(final UploadStatusEvent event) {
        final FileUploadProgress fileUploadProgress = event.getFileUploadProgress();
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();

        uploadLogic.uploadInProgress(fileUploadId, fileUploadProgress);
        updateUploadStatusInfoRowObject(fileUploadId);
    }

    /**
     * Called for every successful file upload.
     */
    private void onUploadSuccess(final UploadStatusEvent event) {
        final FileUploadProgress fileUploadProgress = event.getFileUploadProgress();
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();

        uploadLogic.uploadSucceeded(fileUploadId, fileUploadProgress);
        updateUploadStatusInfoRowObject(fileUploadId);
    }


    /**
     * Called for every failed upload.
     */
    private void onUploadFailure(final UploadStatusEvent event) {
        final FileUploadProgress fileUploadProgress = event.getFileUploadProgress();
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();

        uploadLogic.uploadFailed(fileUploadId, fileUploadProgress);
        updateUploadStatusInfoRowObject(fileUploadId);
    }

    /**
     * Called for every finished (succeeded or failed) upload.
     */
    private void onUploadFinished(final UploadStatusEvent event) {

        // check if we are finished
        if (uploadLogic.isUploadComplete()) {

            // processArtifactUpload();

            if (artifactUploadState.isStatusPopupMinimized()) {
                if (artifactUploadState.getFilesInFailedState().isEmpty()) {
                    cleanupStates();
                } else {
                    maximizeWindow();
                }
            }
        }
    }

    private void cleanupStates() {
        uploads.removeAllItems();
        uploadLogic.clearUploadDetails();
    }

    private void setPopupSizeInMinMode() {
        mainLayout.setWidth(900, Unit.PIXELS);
        mainLayout.setHeight(510, Unit.PIXELS);
    }

    private Button getMinimizeButton() {
        final Button minimizeBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.UPLOAD_STATUS_POPUP_MINIMIZE_BUTTON_ID, "", "", "", true, FontAwesome.MINUS,
                SPUIButtonStyleSmallNoBorder.class);
        minimizeBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        minimizeBtn.addClickListener(event -> minimizeWindow());
        minimizeBtn.setEnabled(true);
        return minimizeBtn;
    }

    private Button getResizeButton() {
        final Button resizeBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.UPLOAD_STATUS_POPUP_RESIZE_BUTTON_ID, "", "", "", true, FontAwesome.EXPAND,
                SPUIButtonStyleSmallNoBorder.class);
        resizeBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        resizeBtn.addClickListener(this::resizeWindow);
        return resizeBtn;
    }

    private void resizeWindow(final ClickEvent event) {
        if (FontAwesome.EXPAND.equals(event.getButton().getIcon())) {
            event.getButton().setIcon(FontAwesome.COMPRESS);
            setWindowMode(WindowMode.MAXIMIZED);
            resetColumnWidth();
            grid.getColumn(STATUS).setExpandRatio(0);
            grid.getColumn(PROGRESS).setExpandRatio(1);
            grid.getColumn(FILE_NAME).setExpandRatio(2);
            grid.getColumn(REASON).setExpandRatio(3);
            grid.getColumn(SPUILabelDefinitions.NAME_VERSION).setExpandRatio(4);
            mainLayout.setSizeFull();
        } else {
            event.getButton().setIcon(FontAwesome.EXPAND);
            setWindowMode(WindowMode.NORMAL);
            setColumnWidth();
            setPopupSizeInMinMode();
        }
    }

    private Button getCloseButton() {
        final Button closeBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.UPLOAD_STATUS_POPUP_CLOSE_BUTTON_ID, "", "", "", true, FontAwesome.TIMES,
                SPUIButtonStyleSmallNoBorder.class);
        closeBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        closeBtn.addClickListener(event -> onClose());
        return closeBtn;
    }

    private void onClose() {
        if (uploadLogic.isUploadComplete()) {
            cleanupStates();
            closeWindow();
        } else {
            confirmAbortAction();
        }
    }

    private void closeWindow() {
        setWindowMode(WindowMode.NORMAL);
        setColumnWidth();
        setPopupSizeInMinMode();
        resizeButton.setIcon(FontAwesome.EXPAND);
        this.close();
    }

    private void confirmAbortAction() {
        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    private void createConfirmDialog() {
        confirmDialog = new ConfirmationDialog(i18n.getMessage("caption.confirm.abort.action"),
                i18n.getMessage("message.abort.upload"), i18n.getMessage("button.ok"), i18n.getMessage("button.cancel"), ok -> {
                    if (ok) {
                        eventBus.publish(this, UploadStatusEventType.UPLOAD_ABORTED_BY_USER);
                        cleanupStates();
                        closeWindow();
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private void updateUploadStatusInfoRowObject(final FileUploadId fileUploadId) {
        Item item = uploads.getItem(fileUploadId);
        if (item == null) {
            item = grid.getContainerDataSource().addItem(fileUploadId);
            item.getItemProperty(FILE_NAME).setValue(fileUploadId.getFilename());
            item.getItemProperty(SPUILabelDefinitions.NAME_VERSION)
                    .setValue(HawkbitCommonUtil.getFormattedNameVersion(fileUploadId.getSoftwareModule().getName(),
                            fileUploadId.getSoftwareModule().getVersion()));
        }

        final String status;
        if (artifactUploadState.getFilesInFailedState().contains(fileUploadId)) {
            status = STATUS_FAILED;
        } else if (artifactUploadState.getFilesInSucceededState().contains(fileUploadId)) {
            status = STATUS_FINISHED;
        } else {
            status = STATUS_INPROGRESS;
        }
        item.getItemProperty(STATUS).setValue(status);

        final String failureReason = artifactUploadState.getFileUploadProgress(fileUploadId).getFailureReason();
        if (StringUtils.isNotBlank(failureReason)) {
            item.getItemProperty(REASON).setValue(failureReason);
        }

        final FileUploadProgress fileUploadProgress = artifactUploadState.getFileUploadProgress(fileUploadId);
        final long bytesRead = fileUploadProgress.getBytesRead();
        final long fileSize = fileUploadProgress.getContentLength();
        if (bytesRead > 0 && fileSize > 0) {
            item.getItemProperty(PROGRESS).setValue((double) bytesRead / (double) fileSize);
        }
    }
}
