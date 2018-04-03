/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
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

    private final Map<FileUploadId, UploadStatusInfo> uploadStatusInfos = new HashMap<>();

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
            // fall through here
            ui.access(() -> onUploadSuccess(event));
            break;
        case UPLOAD_FINISHED:
            ui.access(() -> onUploadFinished());
            break;
        default:
            break;
        }
    }

    @SuppressWarnings("unchecked")
    private void onUploadStarted(final UploadStatusEvent event) {
        // TODO rollouts: ungleich null????
        final FileUploadProgress fileUploadProgress = event.getFileUploadProgress();
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();
        final String fileName = fileUploadId.getFilename();
        final SoftwareModule softwareModule = fileUploadId.getSoftwareModule();
        
        artifactUploadState.addFileToUploadState(fileUploadId, fileUploadProgress);
        uploadLogic.updateUploadProcessCountersDueToUploadAdded();

        // final String itemId =
        // getItemid(event.getFileUploadProgress().getFileName(),
        // event.getFileUploadProgress().getSoftwareModule());

        final Item item = uploads.addItem(fileUploadId);
        if (item != null) {
            item.getItemProperty(FILE_NAME).setValue(fileName);
            item.getItemProperty(SPUILabelDefinitions.NAME_VERSION).setValue(
                    HawkbitCommonUtil.getFormattedNameVersion(softwareModule.getName(), softwareModule.getVersion()));

            // final UploadStatusObject uploadStatus = new
            // UploadStatusObject(fileName, softwareModule);
            // uploadStatus.setStatus("Active");
            final UploadStatusInfo uploadStatusInfo = new UploadStatusInfo(fileUploadId);
            uploadStatusInfo.setStatus("Active");
            uploadStatusInfo.setProgress(fileUploadProgress.getProgress());
            uploadStatusInfos.put(fileUploadId, uploadStatusInfo);

            // TODO rollouts: ist der uploadStatus schon in der liste???
            // artifactUploadState.addFileUploadStatus(uploadStatus);

            // final Optional<UploadStatusObject> uploadStatus3 =
            // artifactUploadState.getFileUploadStatusList()
            // .stream().filter(e ->
            // e.getFilename().equals(fileName)).findFirst();
            // uploadStatus3.setStatus("Active");
        }
        
        startUploadSession();
 
        grid.scrollTo(fileUploadId);
    }   

    private void restoreState() {
        final Indexed container = grid.getContainerDataSource();
        if (container.getItemIds().isEmpty()) {
            container.removeAllItems();
            for (final Map.Entry<FileUploadId, UploadStatusInfo> entry : uploadStatusInfos.entrySet()) {
                final FileUploadId fileUploadId = entry.getKey();
                final UploadStatusInfo uploadStatusInfo = entry.getValue();
                updateUploadStatusInfoRowObject(fileUploadId, uploadStatusInfo.getStatus(),
                        uploadStatusInfo.getReason(), uploadStatusInfo.getProgress());
            }
            if (uploadLogic.isUploadComplete()) {
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

    private void startUploadSession() {
        if (artifactUploadState.getNumberOfFilesActuallyUploading().intValue() == 0
                && artifactUploadState.getNumberOfFileUploadsFailed().intValue() == 0
                && !artifactUploadState.isStatusPopupMinimized()) {
            openWindow();
        }
        if (!uploadAborted) {
            minimizeButton.setEnabled(true);
            closeButton.setEnabled(true);
        }
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
        this.close();
        artifactUploadState.setStatusPopupMinimized(true);
    }

    private void onUploadInProgress(final UploadStatusEvent event) {
        final FileUploadProgress fileUploadProgress = event.getFileUploadProgress();
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();

        final long readBytes = fileUploadProgress.getBytesRead();
        final long contentLength = fileUploadProgress.getContentLength();

        final Item item = uploads.getItem(fileUploadId);
        final double progress = (double) readBytes / (double) contentLength;
        if (item != null) {
            item.getItemProperty(PROGRESS).setValue(progress);
        }

        final UploadStatusInfo uploadStatusInfo = uploadStatusInfos.get(fileUploadId);
        if (uploadStatusInfo != null) {
            uploadStatusInfo.setProgress(progress);
        }
    }

    /**
     * Called for every successful file upload.
     */
    private void onUploadSuccess(final UploadStatusEvent event) {
        final FileUploadProgress fileUploadProgress = event.getFileUploadProgress();
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();

        uploadLogic.updateUploadProcessCountersDueToUploadSucceeded();

        final UploadStatusInfo uploadStatusInfo = uploadStatusInfos.get(fileUploadId);
        if (uploadStatusInfo != null) {
            uploadStatusInfo.setStatus("Finished");
            uploadStatusInfo.setProgress(1d);
        }

        // uploadLogic.updateFileSize(fileUploadId.getFilename(),
        // fileUploadProgress.getContentLength(),
        // fileUploadId.getSoftwareModule(),
        // artifactUploadState.getFileSelected());
        // recorded that we now one more uploaded
        // artifactUploadState.incrementNumberOfFilesActuallyUploading();
        // uploadLogic.resetUploadState();
        // uploadSucceeded(event.getFileUploadProgress().getFileName(),
        // event.getFileUploadProgress().getSoftwareModule());

        processArtifactUpload();
    }
    //
    // private void uploadSucceeded(final String filename, final SoftwareModule
    // softwareModule) {
    // final Item item = uploads.getItem(getItemid(filename, softwareModule));
    // final String status = "Finished";
    // if (item != null) {
    // item.getItemProperty(STATUS).setValue(status);
    // }
    // final List<UploadStatusObject> uploadStatusObjectList =
    // artifactUploadState.getFileUploadStatusList().stream()
    // .filter(e ->
    // e.getFilename().equals(filename)).collect(Collectors.toList());
    // if (!uploadStatusObjectList.isEmpty()) {
    // final UploadStatusObject uploadStatusObject =
    // uploadStatusObjectList.get(0);
    // uploadStatusObject.setStatus(status);
    // uploadStatusObject.setProgress(1d);
    // }
    //
    // // TODO rollouts: uploadconfirmationwindow
    // processArtifactUpload();
    // }


    /**
     * Called for every failed upload.
     */
    private void onUploadFailure(final UploadStatusEvent event) {
        /**
         * If upload interrupted because of duplicate file, do not remove the
         * file already in upload list
         **/
        final FileUploadProgress fileUploadProgress = event.getFileUploadProgress();
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();

        // TODO rollouts: i18n
        updateUploadStatusInfoRowObject(fileUploadId, "Failed", fileUploadProgress.getFailureReason(), 1d);
        uploadLogic.updateUploadProcessCountersDueToUploadFailed();

        // if
        // (!uploadLogic.containsFileName(event.getFileUploadProgress().getFileName()))
        // {
        // final SoftwareModule sw =
        // event.getFileUploadProgress().getSoftwareModule();
        // if (sw != null) {
        // artifactUploadState.removeSelectedFile(
        // new CustomFile(event.getFileUploadProgress().getFileName(),
        // sw.getName(), sw.getVersion()));
        // }
        // // failed reason to be updated only if there is error other than
        // // duplicate file error
        // uploadFailed(event.getFileUploadProgress().getFileName(),
        // event.getFileUploadProgress().getFailureReason(),
        // event.getFileUploadProgress().getSoftwareModule());
        // artifactUploadState.incrementNumberOfFileUploadsFailed();
        // artifactUploadState.decrementNumberOfFileUploadsExpected();
        // uploadLogic.resetUploadState();
        // }
    }

    // private void uploadFailed(final String filename, final String
    // errorReason, final SoftwareModule softwareModule) {
    // errorOccured = true;
    // final String status = "Failed";
    //
    // final List<UploadStatusObject> uploadStatusObjectList =
    // artifactUploadState.getFileUploadStatusList().stream()
    // .filter(e ->
    // e.getFilename().equals(filename)).collect(Collectors.toList());
    // if (!uploadStatusObjectList.isEmpty()) {
    // final UploadStatusObject uploadStatusObject =
    // uploadStatusObjectList.get(0);
    // uploadStatusObject.setStatus(status);
    // uploadStatusObject.setReason(errorReason);
    // }
    // }

    /**
     * Called for every finished (succeeded or failed) upload.
     */
    private void onUploadFinished() {
        // check if we are finished
        if (uploadLogic.isUploadComplete()) {
            uploadAborted = false;
            if (artifactUploadState.isStatusPopupMinimized()) {
                maximizeWindow();
            }
            minimizeButton.setEnabled(false);
            closeButton.setEnabled(true);
            uploadLogic.clearUploadDetailsIfAllUploadsFinished();
        }
        // uploadLogic.resetUploadCounters();
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
            closeWindow();
        } else {
            confirmAbortAction();
        }
    }

    private void closeWindow() {
        errorOccured = false;
        uploads.removeAllItems();
        setWindowMode(WindowMode.NORMAL);
        setColumnWidth();
        setPopupSizeInMinMode();
        resizeButton.setIcon(FontAwesome.EXPAND);
        uploadLogic.clearUploadDetailsIfAllUploadsFinished();
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
                        uploadAborted = true;
                        errorOccured = true;
                        minimizeButton.setEnabled(false);
                        closeButton.setEnabled(false);
                    }
                });
    }

    // private String getItemid(final String filename, final SoftwareModule
    // softwareModule) {
    // return new StringBuilder(filename).append(
    // HawkbitCommonUtil.getFormattedNameVersion(softwareModule.getName(),
    // softwareModule.getVersion()))
    // .toString();
    // }

    // Exception squid:S3655 - Optional access is checked in
    // checkIfArtifactDetailsDispalyed subroutine
    @SuppressWarnings("squid:S3655")
    private void processArtifactUpload() {

        
        // TODO separate from per file topics and overall finished upload
        // process

        final Set<Long> softwareModuleIdsWithNewArtifacts = new HashSet<>();
        for (final Map.Entry<FileUploadId, FileUploadProgress> entry : artifactUploadState.getFilesInUploadState()
                .entrySet()) {

            final FileUploadId fileUploadId = entry.getKey();
            final FileUploadProgress fileUploadProgress = entry.getValue();

            // TODO rollouts: check if file/upload is still valid
            // TODO bulk process or single succeeded file???

            final SoftwareModule softwareModule = fileUploadId.getSoftwareModule();
            final File newFile = new File(fileUploadProgress.getFilePath());

            final String filename = fileUploadId.getFilename();
            softwareModule.getVersion();
            LOG.info("Transfering tempfile {} to repository", fileUploadProgress.getFilePath());
            try (FileInputStream fis = new FileInputStream(newFile)) {

                artifactManagement.create(fis, softwareModule.getId(), filename, null, null, true,
                        fileUploadId.getMimeType());
                // TODO rollouts: i18n
                updateUploadStatusInfoRowObject(fileUploadId, SPUILabelDefinitions.SUCCESS, "", 1d);
                softwareModuleIdsWithNewArtifacts.add(softwareModule.getId());
            } catch (final ArtifactUploadFailedException | InvalidSHA1HashException | InvalidMD5HashException
                    | FileNotFoundException e) {
                // TODO rollouts: i18n
                updateUploadStatusInfoRowObject(fileUploadId, SPUILabelDefinitions.FAILED, e.getMessage(), 1d);
                LOG.error(ARTIFACT_UPLOAD_EXCEPTION, e);

            } catch (final IOException ex) {
                LOG.error(ARTIFACT_UPLOAD_EXCEPTION, ex);
            } finally {
                if (newFile.exists() && !newFile.delete()) {
                    LOG.error("Could not delete temporary file: {}", newFile);
                }
            }
        }

        final Long selectedSoftwareModulId = artifactUploadState.getSelectedBaseSwModuleId().get();
        if (isArtifactAddedToTheSelectedSoftwareModule(selectedSoftwareModulId, softwareModuleIdsWithNewArtifacts)) {
            refreshArtifactDetailsLayout(selectedSoftwareModulId);
        }
        uploadLogic.clearUploadDetailsIfAllUploadsFinished();

        // uploadLayout.setResultPopupHeightWidth(Page.getCurrent().getBrowserWindowWidth(),
        // Page.getCurrent().getBrowserWindowHeight());
        //
    }

    boolean isArtifactAddedToTheSelectedSoftwareModule(final Long selectedSoftwareModulId,
            final Set<Long> softwareModuleIdsWithNewArtifacts) {
        return softwareModuleIdsWithNewArtifacts.contains(selectedSoftwareModulId);
    }

    private void refreshArtifactDetailsLayout(final Long softwareModuleId) {
        final SoftwareModule softwareModule = softwareModuleManagement.get(softwareModuleId).orElse(null);
        eventBus.publish(this, new SoftwareModuleEvent(SoftwareModuleEventType.ARTIFACTS_CHANGED, softwareModule));
    }

    private void updateUploadStatusInfoRowObject(final FileUploadId fileUploadId, final String status,
            final String failureReason, final Double progress) {
        UploadStatusInfo uploadStatusInfo = uploadStatusInfos.get(fileUploadId);
        if (uploadStatusInfo == null) {
            uploadStatusInfo = new UploadStatusInfo(fileUploadId);
            uploadStatusInfos.put(fileUploadId, uploadStatusInfo);
        }
        Item item = uploads.getItem(fileUploadId);
        if (item == null) {
            item = grid.getContainerDataSource().addItem(fileUploadId);
            item.getItemProperty(FILE_NAME).setValue(fileUploadId.getFilename());
            item.getItemProperty(SPUILabelDefinitions.NAME_VERSION)
                    .setValue(HawkbitCommonUtil.getFormattedNameVersion(fileUploadId.getSoftwareModule().getName(),
                            fileUploadId.getSoftwareModule().getVersion()));
        }

        if (!StringUtils.isEmpty(status)) {
            uploadStatusInfo.setStatus(status);
            item.getItemProperty(STATUS).setValue(status);
        }
        if (!StringUtils.isEmpty(failureReason)) {
            uploadStatusInfo.setReason(failureReason);
            item.getItemProperty(REASON).setValue(failureReason);
        }
        if (progress != null) {
            uploadStatusInfo.setProgress(progress);
            item.getItemProperty(PROGRESS).setValue(uploadStatusInfo.getProgress());
        }
    }
}
