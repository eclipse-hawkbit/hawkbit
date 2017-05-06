/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadFileStatus;
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

    private final transient EventBus.UIEventBus eventBus;

    private final ArtifactUploadState artifactUploadState;

    private final VaadinMessageSource i18n;

    private static final String PROGRESS = "Progress";

    private static final String FILE_NAME = "File name";

    private static final String STATUS = "Status";

    private static final String REASON = "Reason";

    private static final long serialVersionUID = 1L;

    private final Grid grid;

    private final IndexedContainer uploads;

    private volatile boolean errorOccured;

    private volatile boolean uploadAborted;

    private Button minimizeButton;

    private final VerticalLayout mainLayout;

    private Label windowCaption;

    private Button closeButton;

    private Button resizeButton;

    private final UI ui;

    private ConfirmationDialog confirmDialog;

    UploadStatusInfoWindow(final UIEventBus eventBus, final ArtifactUploadState artifactUploadState, final VaadinMessageSource i18n) {
        this.eventBus = eventBus;
        this.artifactUploadState = artifactUploadState;
        this.i18n = i18n;

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

        final UploadFileStatus uploadStatus = event.getUploadStatus();
        switch (event.getUploadProgressEventType()) {
        case UPLOAD_IN_PROGRESS:
            ui.access(() -> updateProgress(uploadStatus.getFileName(), uploadStatus.getBytesRead(),
                    uploadStatus.getContentLength(), uploadStatus.getSoftwareModule()));
            break;
        case UPLOAD_STARTED:
            ui.access(() -> onStartOfUpload(event));
            break;
        case UPLOAD_STREAMING_FAILED:
            ui.access(() -> uploadFailed(uploadStatus.getFileName(), uploadStatus.getFailureReason(),
                    uploadStatus.getSoftwareModule()));
            break;
        case UPLOAD_SUCCESSFUL:
            // fall through here
        case UPLOAD_STREAMING_FINISHED:
            ui.access(() -> uploadSucceeded(uploadStatus.getFileName(), uploadStatus.getSoftwareModule()));
            break;
        case RECEIVE_UPLOAD:
            uploadRecevied(uploadStatus.getFileName(), uploadStatus.getSoftwareModule());
            break;
        default:
            break;
        }
    }

    private void onStartOfUpload(final UploadStatusEvent event) {
        uploadSessionStarted();
        uploadStarted(event.getUploadStatus().getFileName(), event.getUploadStatus().getSoftwareModule());
    }

    private void restoreState() {
        final Indexed container = grid.getContainerDataSource();
        if (container.getItemIds().isEmpty()) {
            container.removeAllItems();
            for (final UploadStatusObject statusObject : artifactUploadState.getUploadedFileStatusList()) {
                final Item item = container
                        .addItem(getItemid(statusObject.getFilename(), statusObject.getSelectedSoftwareModule()));
                item.getItemProperty(REASON).setValue(statusObject.getReason() != null ? statusObject.getReason() : "");
                if (statusObject.getStatus() != null) {
                    item.getItemProperty(STATUS).setValue(statusObject.getStatus());
                }
                if (statusObject.getProgress() != null) {
                    item.getItemProperty(PROGRESS).setValue(statusObject.getProgress());
                }
                item.getItemProperty(FILE_NAME).setValue(statusObject.getFilename());
                final SoftwareModule sw = statusObject.getSelectedSoftwareModule();
                item.getItemProperty(SPUILabelDefinitions.NAME_VERSION)
                        .setValue(HawkbitCommonUtil.getFormattedNameVersion(sw.getName(), sw.getVersion()));
            }
            if (artifactUploadState.isUploadCompleted()) {
                minimizeButton.setEnabled(false);
            }
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
            String result;
            switch (value) {
            case "Finished":
                result = "<div class=\"statusIconGreen\">" + FontAwesome.CHECK_CIRCLE.getHtml() + "</div>";
                break;
            case "Failed":
                result = "<div class=\"statusIconRed\">" + FontAwesome.EXCLAMATION_CIRCLE.getHtml() + "</div>";
                break;
            default:
                result = "<div class=\"statusIconActive\"></div>";
            }

            return super.encode(result);
        }
    }

    /**
     * Automatically close if not error has occured.
     */
    void uploadSessionFinished() {
        uploadAborted = false;
        if (!errorOccured && !artifactUploadState.isStatusPopupMinimized()) {
            clearWindow();
        }
        artifactUploadState.setUploadCompleted(true);
        minimizeButton.setEnabled(false);
        closeButton.setEnabled(true);
        confirmDialog.getWindow().close();
        UI.getCurrent().removeWindow(confirmDialog.getWindow());
    }

    void uploadSessionStarted() {
        if (artifactUploadState.getNumberOfFilesActuallyUpload().intValue() == 0
                && artifactUploadState.getNumberOfFileUploadsFailed().intValue() == 0
                && !artifactUploadState.isStatusPopupMinimized()) {
            openWindow();
        }
        if (!uploadAborted) {
            minimizeButton.setEnabled(true);
            closeButton.setEnabled(true);
            artifactUploadState.setUploadCompleted(false);
        }
    }

    void openWindow() {
        UI.getCurrent().addWindow(this);
        center();
    }

    void maximizeStatusPopup() {
        openWindow();
        restoreState();
    }

    @SuppressWarnings("unchecked")
    private void uploadRecevied(final String filename, final SoftwareModule softwareModule) {
        final Item item = uploads.addItem(getItemid(filename, softwareModule));
        if (item != null) {
            item.getItemProperty(FILE_NAME).setValue(filename);
            item.getItemProperty(SPUILabelDefinitions.NAME_VERSION).setValue(
                    HawkbitCommonUtil.getFormattedNameVersion(softwareModule.getName(), softwareModule.getVersion()));
            final UploadStatusObject uploadStatus = new UploadStatusObject(filename, softwareModule);
            uploadStatus.setStatus("Active");
            artifactUploadState.getUploadedFileStatusList().add(uploadStatus);
        }
    }

    void uploadStarted(final String filename, final SoftwareModule softwareModule) {
        grid.scrollTo(getItemid(filename, softwareModule));
    }

    void updateProgress(final String filename, final long readBytes, final long contentLength,
            final SoftwareModule softwareModule) {
        final Item item = uploads.getItem(getItemid(filename, softwareModule));
        final double progress = (double) readBytes / (double) contentLength;
        if (item != null) {
            item.getItemProperty(PROGRESS).setValue(progress);
        }
        final List<UploadStatusObject> uploadStatusObjectList = artifactUploadState.getUploadedFileStatusList().stream()
                .filter(e -> e.getFilename().equals(filename)).collect(Collectors.toList());
        if (!uploadStatusObjectList.isEmpty()) {
            final UploadStatusObject uploadStatusObject = uploadStatusObjectList.get(0);
            uploadStatusObject.setProgress(progress);
        }
    }

    /**
     * Called when each file upload is success.
     *
     * @param filename
     *            of the uploaded file.
     * @param softwareModule
     *            selected software module
     */
    public void uploadSucceeded(final String filename, final SoftwareModule softwareModule) {
        final Item item = uploads.getItem(getItemid(filename, softwareModule));
        final String status = "Finished";
        if (item != null) {
            item.getItemProperty(STATUS).setValue(status);
        }
        final List<UploadStatusObject> uploadStatusObjectList = artifactUploadState.getUploadedFileStatusList().stream()
                .filter(e -> e.getFilename().equals(filename)).collect(Collectors.toList());
        if (!uploadStatusObjectList.isEmpty()) {
            final UploadStatusObject uploadStatusObject = uploadStatusObjectList.get(0);
            uploadStatusObject.setStatus(status);
            uploadStatusObject.setProgress(1d);
        }
    }

    void uploadFailed(final String filename, final String errorReason, final SoftwareModule softwareModule) {
        errorOccured = true;
        final String status = "Failed";
        final Item item = uploads.getItem(getItemid(filename, softwareModule));
        if (item != null) {
            item.getItemProperty(REASON).setValue(errorReason);
            item.getItemProperty(STATUS).setValue(status);
        }
        final List<UploadStatusObject> uploadStatusObjectList = artifactUploadState.getUploadedFileStatusList().stream()
                .filter(e -> e.getFilename().equals(filename)).collect(Collectors.toList());
        if (!uploadStatusObjectList.isEmpty()) {
            final UploadStatusObject uploadStatusObject = uploadStatusObjectList.get(0);
            uploadStatusObject.setStatus(status);
            uploadStatusObject.setReason(errorReason);
        }
    }

    protected void clearWindow() {
        errorOccured = false;
        uploads.removeAllItems();
        setWindowMode(WindowMode.NORMAL);
        setColumnWidth();
        setPopupSizeInMinMode();
        resizeButton.setIcon(FontAwesome.EXPAND);
        this.close();
        artifactUploadState.getUploadedFileStatusList().clear();
        artifactUploadState.getNumberOfFileUploadsFailed().set(0);
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
        resizeBtn.addClickListener(event -> resizeWindow(event));
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

    private void minimizeWindow() {
        this.close();
        artifactUploadState.setStatusPopupMinimized(true);
        eventBus.publish(this, UploadArtifactUIEvent.MINIMIZED_STATUS_POPUP);
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
        if (!artifactUploadState.isUploadCompleted()) {
            confirmAbortAction();
        } else {
            clearWindow();
        }
    }

    private void confirmAbortAction() {
        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    private void createConfirmDialog() {
        confirmDialog = new ConfirmationDialog(i18n.getMessage("caption.confirm.abort.action"),
                i18n.getMessage("message.abort.upload"), i18n.getMessage("button.ok"), i18n.getMessage("button.cancel"), ok -> {
                    if (ok) {
                        eventBus.publish(this, UploadStatusEventType.ABORT_UPLOAD);
                        uploadAborted = true;
                        errorOccured = true;
                        minimizeButton.setEnabled(false);
                        closeButton.setEnabled(false);
                    }
                });
    }

    private String getItemid(final String filename, final SoftwareModule softwareModule) {
        return new StringBuilder(filename).append(
                HawkbitCommonUtil.getFormattedNameVersion(softwareModule.getName(), softwareModule.getVersion()))
                .toString();
    }
}
