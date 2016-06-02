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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent.UploadStatusEventType;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.state.CustomFile;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.util.SPInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.base.Strings;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.StreamVariable;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.WrapperTransferable;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Html5File;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;

/**
 * Upload files layout.
 */
@ViewScope
@SpringComponent
public class UploadLayout extends VerticalLayout {

    private static final long serialVersionUID = -566164756606779220L;

    private static final Logger LOG = LoggerFactory.getLogger(UploadLayout.class);

    @Autowired
    private UploadStatusInfoWindow uploadInfoWindow;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UINotification uiNotification;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ArtifactUploadState artifactUploadState;

    @Autowired
    private transient SPInfo spInfo;

    private final List<String> duplicateFileNamesList = new ArrayList<>();

    private Button processBtn;

    private Button discardBtn;

    private UploadConfirmationwindow currentUploadConfirmationwindow;

    private VerticalLayout dropAreaLayout;

    private UI ui;

    private HorizontalLayout fileUploadLayout;

    private DragAndDropWrapper dropAreaWrapper;

    private Boolean hasDirectory = Boolean.FALSE;

    private Button uploadStatusButton;

    /**
     * Initialize the upload layout.
     */
    @PostConstruct
    void init() {
        createComponents();
        buildLayout();
        restoreState();
        eventBus.subscribe(this);
        ui = UI.getCurrent();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final UploadArtifactUIEvent event) {
        if (event == UploadArtifactUIEvent.DELETED_ALL_SOFWARE) {
            ui.access(() -> updateActionCount());
        } else if (event == UploadArtifactUIEvent.MINIMIZED_STATUS_POPUP) {
            ui.access(() -> showUploadStatusButton());
        } else if (event == UploadArtifactUIEvent.MAXIMIZED_STATUS_POPUP) {
            ui.access(() -> maximizeStatusPopup());
        } else if (event == UploadArtifactUIEvent.ARTIFACT_RESULT_POPUP_CLOSED) {
            ui.access(() -> closeUploadStatusPopup());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final UploadStatusEvent event) {
        if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_STARTED) {
            ui.access(() -> onStartOfUpload());
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_FAILED) {
            ui.access(() -> onUploadFailure(event));
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_FINISHED) {
            ui.access(() -> onUploadCompletion());
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_SUCCESSFUL) {
            ui.access(() -> onUploadSuccess(event));
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_STREAMING_FAILED) {
            ui.access(() -> onUploadStreamingFailure(event));
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_STREAMING_FINISHED) {
            ui.access(() -> onUploadStreamingSuccess());
        }
    }

    @PreDestroy
    void destroy() {
        /*
         * It's good manners to do this, even though vaadin-spring will
         * automatically unsubscribe when this UI is garbage collected.
         */
        eventBus.unsubscribe(this);
    }

    private void createComponents() {
        createUploadStatusButton();
        createProcessButton();
        createDiscardBtn();
    }

    private void buildLayout() {

        final Upload upload = new Upload();
        final UploadHandler uploadHandler = new UploadHandler(null, 0, this, spInfo.getMaxArtifactFileSize(), upload,
                null, null);
        upload.setButtonCaption(i18n.get("upload.file"));
        upload.setImmediate(true);
        upload.setReceiver(uploadHandler);
        upload.addSucceededListener(uploadHandler);
        upload.addFailedListener(uploadHandler);
        upload.addFinishedListener(uploadHandler);
        upload.addProgressListener(uploadHandler);
        upload.addStartedListener(uploadHandler);
        upload.addStyleName(SPUIStyleDefinitions.ACTION_BUTTON);

        fileUploadLayout = new HorizontalLayout();
        fileUploadLayout.setSpacing(true);
        fileUploadLayout.addStyleName(SPUIStyleDefinitions.FOOTER_LAYOUT);
        fileUploadLayout.addComponent(upload);
        fileUploadLayout.setComponentAlignment(upload, Alignment.MIDDLE_LEFT);
        fileUploadLayout.addComponent(processBtn);
        fileUploadLayout.setComponentAlignment(processBtn, Alignment.MIDDLE_RIGHT);
        fileUploadLayout.addComponent(discardBtn);
        fileUploadLayout.setComponentAlignment(discardBtn, Alignment.MIDDLE_RIGHT);
        fileUploadLayout.addComponent(uploadStatusButton);
        fileUploadLayout.setComponentAlignment(uploadStatusButton, Alignment.MIDDLE_RIGHT);
        setMargin(false);

        /* create drag-drop wrapper for drop area */
        dropAreaWrapper = new DragAndDropWrapper(createDropAreaLayout());
        dropAreaWrapper.setDropHandler(new DropAreahandler());
        setSizeFull();
        setSpacing(true);
    }

    private void restoreState() {
        updateActionCount();

        if (!artifactUploadState.getFileSelected().isEmpty() && artifactUploadState.isUploadCompleted()) {
            processBtn.setEnabled(true);
        }
        if (artifactUploadState.isStatusPopupMinimized()) {
            showUploadStatusButton();
            if (artifactUploadState.isUploadCompleted()) {
                setUploadStatusButtonIconToFinished();
            }
        }
        if (artifactUploadState.isUploadCompleted()) {
            artifactUploadState.getNumberOfFilesActuallyUpload().set(0);
            artifactUploadState.getNumberOfFileUploadsExpected().set(0);
            artifactUploadState.getNumberOfFileUploadsFailed().set(0);
        }
    }

    public DragAndDropWrapper getDropAreaWrapper() {
        return dropAreaWrapper;
    }

    private class DropAreahandler implements DropHandler {

        private static final long serialVersionUID = 1L;

        @Override
        public AcceptCriterion getAcceptCriterion() {
            return AcceptAll.get();
        }

        @Override
        public void drop(final DragAndDropEvent event) {
            if (validate(event)) {
                final Html5File[] files = ((WrapperTransferable) event.getTransferable()).getFiles();
                // selected software module at the time of file drop is
                // considered for upload
                SoftwareModule selectedSw = artifactUploadState.getSelectedBaseSoftwareModule().get();
                // reset the flag
                hasDirectory = Boolean.FALSE;
                for (final Html5File file : files) {
                    processFile(file, selectedSw);
                }
                if (artifactUploadState.getNumberOfFileUploadsExpected().get() > 0) {
                    processBtn.setEnabled(false);
                } else {
                    // If the upload is not started, it signifies all
                    // dropped files as either duplicate or directory.So
                    // display message accordingly
                    displayCompositeMessage();
                }
            }
        }

        private void processFile(final Html5File file, SoftwareModule selectedSw) {
            if (!isDirectory(file)) {
                if (!checkForDuplicate(file.getFileName(), selectedSw)) {
                    artifactUploadState.getNumberOfFileUploadsExpected().incrementAndGet();
                    file.setStreamVariable(createStreamVariable(file, selectedSw));
                }
            } else {
                hasDirectory = Boolean.TRUE;
            }
        }

        private StreamVariable createStreamVariable(final Html5File file, SoftwareModule selectedSw) {
            return new UploadHandler(file.getFileName(), file.getFileSize(), UploadLayout.this,
                    spInfo.getMaxArtifactFileSize(), null, file.getType(), selectedSw);
        }

        private boolean isDirectory(final Html5File file) {
            return Strings.isNullOrEmpty(file.getType()) && file.getFileSize() % 4096 == 0;
        }
    }

    private void displayCompositeMessage() {
        final String duplicateMessage = getDuplicateFileValidationMessage();
        final StringBuilder compositeMessage = new StringBuilder();
        if (!Strings.isNullOrEmpty(duplicateMessage)) {
            compositeMessage.append(duplicateMessage);
        }
        if (hasDirectory) {
            if (compositeMessage.length() > 0) {
                compositeMessage.append("<br>");
            }
            compositeMessage.append(i18n.get("message.no.directory.upload"));
        }
        if (!compositeMessage.toString().isEmpty()) {
            uiNotification.displayValidationError(compositeMessage.toString());
        }
    }

    private VerticalLayout createDropAreaLayout() {
        dropAreaLayout = new VerticalLayout();
        final Label dropHereLabel = new Label("Drop files to upload");
        dropHereLabel.setWidth(null);

        final Label dropIcon = new Label(FontAwesome.ARROW_DOWN.getHtml(), ContentMode.HTML);
        dropIcon.addStyleName("drop-icon");
        dropIcon.setWidth(null);

        dropAreaLayout.addComponent(dropIcon);
        dropAreaLayout.setComponentAlignment(dropIcon, Alignment.BOTTOM_CENTER);
        dropAreaLayout.addComponent(dropHereLabel);
        dropAreaLayout.setComponentAlignment(dropHereLabel, Alignment.TOP_CENTER);
        dropAreaLayout.setSizeFull();
        dropAreaLayout.setStyleName("upload-drop-area-layout-info");
        dropAreaLayout.setSpacing(false);
        return dropAreaLayout;
    }

    private void createProcessButton() {
        processBtn = SPUIComponentProvider.getButton(SPUIComponentIdProvider.UPLOAD_PROCESS_BUTTON,
                SPUILabelDefinitions.PROCESS, SPUILabelDefinitions.PROCESS, null, false, null,
                SPUIButtonStyleSmall.class);
        processBtn.setIcon(FontAwesome.BELL);
        processBtn.addStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
        processBtn.addClickListener(event -> displayConfirmWindow(event));
        processBtn.setHtmlContentAllowed(true);
        processBtn.setEnabled(false);
    }

    private void createDiscardBtn() {
        discardBtn = SPUIComponentProvider.getButton(SPUIComponentIdProvider.UPLOAD_DISCARD_BUTTON,
                SPUILabelDefinitions.DISCARD, SPUILabelDefinitions.DISCARD, null, false, null,
                SPUIButtonStyleSmall.class);
        discardBtn.setIcon(FontAwesome.TRASH_O);
        discardBtn.addStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
        discardBtn.addClickListener(event -> discardUploadData(event));
    }

    boolean checkForDuplicate(final String filename, final SoftwareModule selectedSw) {
        final Boolean isDuplicate = checkIfFileIsDuplicate(filename, selectedSw);
        if (isDuplicate) {
            getDuplicateFileNamesList().add(filename);
        }
        return isDuplicate;
    }

    /**
     * Save uploaded file details.
     *
     * @param stream
     *            read from uploaded file
     * @param name
     *            file name
     * @param size
     *            file size
     * @param mimeType
     *            the mimeType of the file
     * @param selectedSw
     * @throws IOException
     *             in case of upload errors
     */
    OutputStream saveUploadedFileDetails(final String name, final long size, final String mimeType,
            SoftwareModule selectedSw) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("spUiArtifactUpload", null);

            final OutputStream out = new FileOutputStream(tempFile);

            final String currentBaseSoftwareModuleKey = HawkbitCommonUtil.getFormattedNameVersion(selectedSw.getName(),
                    selectedSw.getVersion());

            final CustomFile customFile = new CustomFile(name, size, tempFile.getAbsolutePath(), selectedSw.getName(),
                    selectedSw.getVersion(), mimeType);

            artifactUploadState.getFileSelected().add(customFile);
            processBtn.setEnabled(false);

            if (!artifactUploadState.getBaseSwModuleList().keySet().contains(currentBaseSoftwareModuleKey)) {
                artifactUploadState.getBaseSwModuleList().put(currentBaseSoftwareModuleKey, selectedSw);
            }
            return out;
        } catch (final FileNotFoundException e) {
            LOG.error("Upload failed {}", e);
            throw new ArtifactUploadFailedException(i18n.get("message.file.not.found"));
        } catch (final IOException e) {
            LOG.error("Upload failed {}", e);
            throw new ArtifactUploadFailedException(i18n.get("message.upload.failed"));
        }

    }

    Boolean validate(final DragAndDropEvent event) {
        // check if drop is valid.If valid ,check if software module is
        // selected.
        if (!isFilesDropped(event)) {
            uiNotification.displayValidationError(i18n.get("message.action.not.allowed"));
            return false;
        }
        return checkIfSoftwareModuleIsSelected();
    }

    private boolean isFilesDropped(final DragAndDropEvent event) {
        if (event.getTransferable() instanceof WrapperTransferable) {
            final Html5File[] files = ((WrapperTransferable) event.getTransferable()).getFiles();
            // other components can also be wrapped in WrapperTransferable , so
            // additional check on files
            if (files == null) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    Boolean checkIfSoftwareModuleIsSelected() {
        if (!isSoftwareModuleSelected()) {
            uiNotification.displayValidationError(i18n.get("message.error.noSwModuleSelected"));
            return false;
        }
        return true;
    }

    Boolean isSoftwareModuleSelected() {
        if (!artifactUploadState.getSelectedBaseSwModuleId().isPresent()) {
            return false;
        }
        return true;
    }

    /**
     * Check if file selected is duplicate.i,e already selected for upload for
     * same software module.
     *
     * @param name
     *            file name
     * @return Boolean
     */
    public Boolean checkIfFileIsDuplicate(final String name, final SoftwareModule selectedSoftwareModule) {
        Boolean isDuplicate = false;
        final String currentBaseSoftwareModuleKey = HawkbitCommonUtil.getFormattedNameVersion(
                selectedSoftwareModule.getName(), selectedSoftwareModule.getVersion());

        for (final CustomFile customFile : artifactUploadState.getFileSelected()) {
            final String fileSoftwareModuleKey = HawkbitCommonUtil.getFormattedNameVersion(
                    customFile.getBaseSoftwareModuleName(), customFile.getBaseSoftwareModuleVersion());
            if (customFile.getFileName().equals(name) && currentBaseSoftwareModuleKey.equals(fileSoftwareModuleKey)) {
                isDuplicate = true;
                break;
            }
        }
        return isDuplicate;
    }

    void decreaseNumberOfFileUploadsExpected() {
        artifactUploadState.getNumberOfFileUploadsExpected().decrementAndGet();
    }

    List<String> getDuplicateFileNamesList() {
        return duplicateFileNamesList;
    }

    /**
     * Update pending action count.
     */
    void updateActionCount() {
        if (!artifactUploadState.getFileSelected().isEmpty()) {
            processBtn.setCaption(SPUILabelDefinitions.PROCESS + "<div class='unread'>"
                    + artifactUploadState.getFileSelected().size() + "</div>");
        } else {
            processBtn.setCaption(SPUILabelDefinitions.PROCESS);
        }
    }

    void displayDuplicateValidationMessage() {
        // check if streaming of all dropped files are completed
        if (artifactUploadState.getNumberOfFilesActuallyUpload().intValue() == artifactUploadState
                .getNumberOfFileUploadsExpected().intValue()) {
            displayCompositeMessage();
            duplicateFileNamesList.clear();
        }
    }

    private String getDuplicateFileValidationMessage() {
        final StringBuilder message = new StringBuilder();
        if (!duplicateFileNamesList.isEmpty()) {
            final String fileNames = StringUtils.collectionToCommaDelimitedString(duplicateFileNamesList);
            if (duplicateFileNamesList.size() == 1) {
                message.append(i18n.get("message.no.duplicateFile") + fileNames);

            } else if (duplicateFileNamesList.size() > 1) {
                message.append(i18n.get("message.no.duplicateFiles"));
            }
        }
        return message.toString();
    }

    public void showDuplicateMessage() {
        uiNotification.displayValidationError(getDuplicateFileValidationMessage());
    }

    void increaseNumberOfFileUploadsExpected() {
        artifactUploadState.getNumberOfFileUploadsExpected().incrementAndGet();
    }

    void updateFileSize(final String name, final long size, SoftwareModule selectedSoftwareModule) {
        final String currentBaseSoftwareModuleKey = HawkbitCommonUtil.getFormattedNameVersion(
                selectedSoftwareModule.getName(), selectedSoftwareModule.getVersion());

        for (final CustomFile customFile : artifactUploadState.getFileSelected()) {
            final String fileSoftwareModuleKey = HawkbitCommonUtil.getFormattedNameVersion(
                    customFile.getBaseSoftwareModuleName(), customFile.getBaseSoftwareModuleVersion());
            if (customFile.getFileName().equals(name) && currentBaseSoftwareModuleKey.equals(fileSoftwareModuleKey)) {
                customFile.setFileSize(size);
                break;
            }
        }
    }

    void increaseNumberOfFilesActuallyUpload() {
        artifactUploadState.getNumberOfFilesActuallyUpload().incrementAndGet();
    }

    void increaseNumberOfFileUploadsFailed() {
        artifactUploadState.getNumberOfFileUploadsFailed().incrementAndGet();
    }

    /**
     * Enable process button once upload is completed.
     */
    boolean enableProcessBtn() {
        if (artifactUploadState.getNumberOfFilesActuallyUpload().intValue() >= artifactUploadState
                .getNumberOfFileUploadsExpected().intValue() && !getFileSelected().isEmpty()) {
            processBtn.setEnabled(true);
            artifactUploadState.getNumberOfFilesActuallyUpload().set(0);
            artifactUploadState.getNumberOfFileUploadsExpected().set(0);
            return true;
        }
        return false;
    }

    Set<CustomFile> getFileSelected() {
        return artifactUploadState.getFileSelected();
    }

    private void discardUploadData(final Button.ClickEvent event) {
        if (event.getButton().equals(discardBtn)) {
            if (artifactUploadState.getFileSelected().isEmpty()) {
                uiNotification.displayValidationError(i18n.get("message.error.noFileSelected"));
            } else {
                clearUploadedFileDetails();
            }
        }
    }

    protected void clearUploadedFileDetails() {
        clearFileList();
        closeUploadStatusPopup();
    }

    private void closeUploadStatusPopup() {
        uploadInfoWindow.clearWindow();
        hideUploadStatusButton();
        artifactUploadState.setStatusPopupMinimized(false);
    }

    /**
     * Clear details.
     */
    void clearFileList() {
        // delete file system zombies
        artifactUploadState.getFileSelected().forEach(customFile -> {
            final File file = new File(customFile.getFilePath());
            file.delete();
        });

        artifactUploadState.getFileSelected().clear();
        artifactUploadState.getBaseSwModuleList().clear();
        processBtn.setCaption(SPUILabelDefinitions.PROCESS);
        /* disable when there is no files to upload. */
        processBtn.setEnabled(false);
        artifactUploadState.getNumberOfFilesActuallyUpload().set(0);
        artifactUploadState.getNumberOfFileUploadsExpected().set(0);
        artifactUploadState.getNumberOfFileUploadsFailed().set(0);
        duplicateFileNamesList.clear();
    }

    private void setConfirmationPopupHeightWidth(final float newWidth, final float newHeight) {
        if (currentUploadConfirmationwindow != null) {
            currentUploadConfirmationwindow.getUploadArtifactDetails().setWidth(
                    HawkbitCommonUtil.getArtifactUploadPopupWidth(newWidth,
                            SPUIDefinitions.MIN_UPLOAD_CONFIRMATION_POPUP_WIDTH), Unit.PIXELS);
            currentUploadConfirmationwindow.getUploadDetailsTable().setHeight(
                    HawkbitCommonUtil.getArtifactUploadPopupHeight(newHeight,
                            SPUIDefinitions.MIN_UPLOAD_CONFIRMATION_POPUP_HEIGHT), Unit.PIXELS);
        }
    }

    /**
     * Set artifact upload result pop up size changes.
     *
     * @param newWidth
     *            new width of result pop up
     * @param newHeight
     *            new height of result pop up
     */
    void setResultPopupHeightWidth(final float newWidth, final float newHeight) {
        if (currentUploadConfirmationwindow != null
                && currentUploadConfirmationwindow.getCurrentUploadResultWindow() != null) {
            final UploadResultWindow uploadResultWindow = currentUploadConfirmationwindow
                    .getCurrentUploadResultWindow();
            uploadResultWindow.getUploadResultsWindow().setWidth(
                    HawkbitCommonUtil.getArtifactUploadPopupWidth(newWidth,
                            SPUIDefinitions.MIN_UPLOAD_CONFIRMATION_POPUP_WIDTH), Unit.PIXELS);
            uploadResultWindow.getUploadResultTable().setHeight(
                    HawkbitCommonUtil.getArtifactUploadPopupHeight(newHeight,
                            SPUIDefinitions.MIN_UPLOAD_CONFIRMATION_POPUP_HEIGHT), Unit.PIXELS);
        }
    }

    private void displayConfirmWindow(final Button.ClickEvent event) {
        if (event.getComponent().getId().equals(SPUIComponentIdProvider.UPLOAD_PROCESS_BUTTON)) {
            if (artifactUploadState.getFileSelected().isEmpty()) {
                uiNotification.displayValidationError(i18n.get("message.error.noFileSelected"));
            } else {
                currentUploadConfirmationwindow = new UploadConfirmationwindow(this, artifactUploadState);
                UI.getCurrent().addWindow(currentUploadConfirmationwindow.getUploadConfrimationWindow());
                setConfirmationPopupHeightWidth(Page.getCurrent().getBrowserWindowWidth(), Page.getCurrent()
                        .getBrowserWindowHeight());
            }
        }
    }

    /**
     * @return
     */
    I18N getI18n() {
        return i18n;
    }

    /**
     * @return
     */
    SPInfo getSPInfo() {
        return spInfo;
    }

    void setCurrentUploadConfirmationwindow(final UploadConfirmationwindow currentUploadConfirmationwindow) {
        this.currentUploadConfirmationwindow = currentUploadConfirmationwindow;
    }

    /**
     * @return
     */
    VerticalLayout getDropAreaLayout() {
        return dropAreaLayout;
    }

    private void onStartOfUpload() {
        setUploadStatusButtonIconToInProgress();
        if (artifactUploadState.isStatusPopupMinimized()) {
            updateStatusButtonCount();
        }
    }

    private void onUploadStreamingSuccess() {
        increaseNumberOfFilesActuallyUpload();
        updateUploadCounts();
        enableProcessBtn();
        if (isUploadComplete()) {
            uploadInfoWindow.uploadSessionFinished();
            setUploadStatusButtonIconToFinished();
        }
        // display the duplicate message after streaming all files
        displayDuplicateValidationMessage();
    }

    private void onUploadStreamingFailure(UploadStatusEvent event) {
        /**
         * If upload interrupted because of duplicate file,do not remove the
         * file already in upload list
         **/
        if (getDuplicateFileNamesList().isEmpty()
                || !getDuplicateFileNamesList().contains(event.getUploadStatus().getFileName())) {
            final SoftwareModule sw = event.getUploadStatus().getSoftwareModule();
            if (sw != null) {
                getFileSelected().remove(
                        new CustomFile(event.getUploadStatus().getFileName(), sw.getName(), sw.getVersion()));
            }
            // failed reason to be updated only if there is error other than
            // duplicate file error
            uploadInfoWindow.uploadFailed(event.getUploadStatus().getFileName(), event.getUploadStatus()
                    .getFailureReason(), event.getUploadStatus().getSoftwareModule());
            increaseNumberOfFileUploadsFailed();
        }
        decreaseNumberOfFileUploadsExpected();
        updateUploadCounts();
        enableProcessBtn();
        // check if we are finished
        if (isUploadComplete()) {
            uploadInfoWindow.uploadSessionFinished();
            setUploadStatusButtonIconToFinished();
        }
        displayDuplicateValidationMessage();
    }

    private void onUploadSuccess(UploadStatusEvent event) {
        updateFileSize(event.getUploadStatus().getFileName(), event.getUploadStatus().getContentLength(), event
                .getUploadStatus().getSoftwareModule());
        // recorded that we now one more uploaded
        increaseNumberOfFilesActuallyUpload();
    }

    private void onUploadCompletion() {
        // check if we are finished
        if (isUploadComplete()) {
            uploadInfoWindow.uploadSessionFinished();
            setUploadStatusButtonIconToFinished();
            displayDuplicateValidationMessage();
        }
        updateUploadCounts();
        enableProcessBtn();
    }

    private boolean isUploadComplete() {
        int uploadedCount = artifactUploadState.getNumberOfFilesActuallyUpload().intValue();
        int expectedUploadsCount = artifactUploadState.getNumberOfFileUploadsExpected().intValue();
        return uploadedCount == expectedUploadsCount;
    }

    private void onUploadFailure(final UploadStatusEvent event) {
        /**
         * If upload interrupted because of duplicate file,do not remove the
         * file already in upload list
         **/
        if (getDuplicateFileNamesList().isEmpty()
                || !getDuplicateFileNamesList().contains(event.getUploadStatus().getFileName())) {
            final SoftwareModule sw = event.getUploadStatus().getSoftwareModule();
            if (sw != null) {
                getFileSelected().remove(
                        new CustomFile(event.getUploadStatus().getFileName(), sw.getName(), sw.getVersion()));
            }
            // failed reason to be updated only if there is error other than
            // duplicate file error
            uploadInfoWindow.uploadFailed(event.getUploadStatus().getFileName(), event.getUploadStatus()
                    .getFailureReason(), event.getUploadStatus().getSoftwareModule());
            increaseNumberOfFileUploadsFailed();
            decreaseNumberOfFileUploadsExpected();
        }
    }

    /**
     * set upload status and confirmation window.
     * 
     * @param newWidth
     *            browser width
     * @param newHeight
     *            browser height
     */
    public void setUploadPopupSize(final float newWidth, final float newHeight) {
        setConfirmationPopupHeightWidth(newWidth, newHeight);
        setResultPopupHeightWidth(newWidth, newHeight);
    }

    /**
     * @param selectedBaseSoftwareModule
     */
    public void refreshArtifactDetailsLayout(final SoftwareModule selectedBaseSoftwareModule) {
        eventBus.publish(this, new SoftwareModuleEvent(SoftwareModuleEventType.ARTIFACTS_CHANGED,
                selectedBaseSoftwareModule));
    }

    /**
     * @return the fileUploadLayout
     */
    public HorizontalLayout getFileUploadLayout() {
        return fileUploadLayout;
    }

    public UINotification getUINotification() {
        return uiNotification;
    }

    public void setHasDirectory(final Boolean hasDirectory) {
        this.hasDirectory = hasDirectory;
    }

    private void createUploadStatusButton() {
        uploadStatusButton = SPUIComponentProvider.getButton(SPUIComponetIdProvider.UPLOAD_STATUS_BUTTON, "", "", "",
                false, null, SPUIButtonStyleSmall.class);
        uploadStatusButton.setStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
        uploadStatusButton.addStyleName(SPUIStyleDefinitions.UPLOAD_PROGRESS_INDICATOR_STYLE);
        uploadStatusButton.setWidth("100px");
        uploadStatusButton.setHtmlContentAllowed(true);
        uploadStatusButton.addClickListener(event -> onClickOfUploadStatusButton());
        uploadStatusButton.setVisible(false);
    }

    void updateStatusButtonCount() {
        int uploadsPending = artifactUploadState.getNumberOfFileUploadsExpected().get()
                - artifactUploadState.getNumberOfFilesActuallyUpload().get();
        int uploadsFailed = artifactUploadState.getNumberOfFileUploadsFailed().get();
        StringBuilder builder = new StringBuilder("");
        if (uploadsFailed != 0) {
            if (uploadsPending != 0) {
                builder.append("<div class='error-count error-count-color'>" + uploadsFailed + "</div>");
            } else {
                builder.append("<div class='unread error-count-color'>" + uploadsFailed + "</div>");
            }
        }
        if (uploadsPending != 0) {
            builder.append("<div class='unread'>" + uploadsPending + "</div>");
        }
        uploadStatusButton.setCaption(builder.toString());
    }

    private void onClickOfUploadStatusButton() {
        artifactUploadState.setStatusPopupMinimized(false);
        eventBus.publish(this, UploadArtifactUIEvent.MAXIMIZED_STATUS_POPUP);
    }

    private void showUploadStatusButton() {
        if (uploadStatusButton == null) {
            return;
        }
        uploadStatusButton.setVisible(true);
        updateStatusButtonCount();
    }

    protected void hideUploadStatusButton() {
        if (uploadStatusButton == null) {
            return;
        }
        uploadStatusButton.setVisible(false);
    }

    private void maximizeStatusPopup() {
        hideUploadStatusButton();
        uploadInfoWindow.maximizeStatusPopup();
    }

    private void setUploadStatusButtonIconToFinished() {
        if (uploadStatusButton == null) {
            return;
        }
        uploadStatusButton.removeStyleName(SPUIStyleDefinitions.UPLOAD_PROGRESS_INDICATOR_STYLE);
        uploadStatusButton.setIcon(FontAwesome.UPLOAD);
    }

    private void setUploadStatusButtonIconToInProgress() {
        if (uploadStatusButton == null) {
            return;
        }
        uploadStatusButton.addStyleName(SPUIStyleDefinitions.UPLOAD_PROGRESS_INDICATOR_STYLE);
        uploadStatusButton.setIcon(null);
    }

    protected void updateUploadCounts() {
        updateActionCount();
        updateStatusButtonCount();
    }
}
