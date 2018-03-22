/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import javax.servlet.MultipartConfigElement;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent.UploadStatusEventType;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;

/**
 * Upload files layout.
 */
public class UploadAndStatusButtonLayout extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private static final String HTML_DIV = "</div>";

    private static final Logger LOG = LoggerFactory.getLogger(UploadAndStatusButtonLayout.class);

    private final UploadStatusInfoWindow uploadInfoWindow;

    private final VaadinMessageSource i18n;

    private final UINotification uiNotification;

    private final transient EventBus.UIEventBus eventBus;

    private final ArtifactUploadState artifactUploadState;

    private final transient MultipartConfigElement multipartConfigElement;

    private UploadConfirmationWindow currentUploadConfirmationwindow;

    private final UI ui;

    private HorizontalLayout fileUploadButtonLayout;

    private Button uploadStatusButton;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final transient ArtifactManagement artifactManagement;

    private final UploadLogic uploadLogic;

    private final UploadMessageBuilder uploadMessageBuilder;

    public UploadAndStatusButtonLayout(final VaadinMessageSource i18n, final UINotification uiNotification, final UIEventBus eventBus,
            final ArtifactUploadState artifactUploadState, final MultipartConfigElement multipartConfigElement,
            final ArtifactManagement artifactManagement, final SoftwareModuleManagement softwareManagement,
            final UploadLogic uploadLogic) {
        this.uploadLogic = uploadLogic;
        this.uploadInfoWindow = new UploadStatusInfoWindow(eventBus, artifactUploadState, i18n, artifactManagement,
                softwareManagement, uploadLogic);
        this.i18n = i18n;
        this.uiNotification = uiNotification;
        this.eventBus = eventBus;
        this.artifactUploadState = artifactUploadState;
        this.multipartConfigElement = multipartConfigElement;
        this.artifactManagement = artifactManagement;
        this.softwareModuleManagement = softwareManagement;
        this.uploadMessageBuilder = new UploadMessageBuilder(uploadLogic, i18n);

        createComponents();
        buildLayout();
        restoreState();
        eventBus.subscribe(this);
        ui = UI.getCurrent();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final UploadArtifactUIEvent event) {
        // if (event == UploadArtifactUIEvent.MINIMIZED_STATUS_POPUP) {
        // ui.access(this::minimizeStatusPopup);
        // } else if (event == UploadArtifactUIEvent.MAXIMIZED_STATUS_POPUP) {
        // ui.access(this::maximizeStatusPopup);
        // } else if (event ==
        // UploadArtifactUIEvent.ARTIFACT_RESULT_POPUP_CLOSED) {
        // ui.access(this::closeUploadStatusPopup);
        // }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final UploadStatusEvent event) {
        if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_STARTED) {
            ui.access(this::onStartOfUpload);
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_FINISHED) {
            ui.access(this::onUploadFinished);
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_STREAMING_FAILED) {
            ui.access(() -> onUploadStreamingFailure(event));
        } else if (event.getUploadProgressEventType() == UploadStatusEventType.UPLOAD_STREAMING_FINISHED) {
            ui.access(this::onUploadStreamingSuccess);
        }
    }

    private void createComponents() {
        uploadStatusButton = SPUIComponentProvider.getButton(UIComponentIdProvider.UPLOAD_STATUS_BUTTON, "", "", "",
                false, null, SPUIButtonStyleSmall.class);
        uploadStatusButton.setStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
        uploadStatusButton.addStyleName(SPUIStyleDefinitions.UPLOAD_PROGRESS_INDICATOR_STYLE);
        uploadStatusButton.setIcon(null);
        uploadStatusButton.setWidth("100px");
        uploadStatusButton.setHtmlContentAllowed(true);
        uploadStatusButton.addClickListener(event -> onClickOfUploadStatusButton());
        uploadStatusButton.setVisible(false);
    }

    private void buildLayout() {

        final Upload upload = new Upload();
        final FileTransferHandler uploadHandler = new FileTransferHandler(null, 0, uploadLogic,
                multipartConfigElement.getMaxFileSize(),
                upload, null, null, softwareModuleManagement);
        upload.setButtonCaption(i18n.getMessage("upload.file"));
        upload.setImmediate(true);
        upload.setReceiver(uploadHandler);
        upload.addSucceededListener(uploadHandler);
        upload.addFailedListener(uploadHandler);
        upload.addFinishedListener(uploadHandler);
        upload.addProgressListener(uploadHandler);
        upload.addStartedListener(uploadHandler);
        upload.addStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
        upload.addStyleName("no-border");        

        fileUploadButtonLayout = new HorizontalLayout();
        fileUploadButtonLayout.setSpacing(true);
        fileUploadButtonLayout.addStyleName(SPUIStyleDefinitions.FOOTER_LAYOUT);
        fileUploadButtonLayout.addComponent(upload);
        fileUploadButtonLayout.setComponentAlignment(upload, Alignment.MIDDLE_LEFT);
        fileUploadButtonLayout.addComponent(uploadStatusButton);
        fileUploadButtonLayout.setComponentAlignment(uploadStatusButton, Alignment.MIDDLE_RIGHT);
        setMargin(false);

        setSizeFull();
        setSpacing(true);
    }

    private void restoreState() {

        if (uploadLogic.isUploadComplete()) {
            artifactUploadState.clearNumberOfFilesActuallyUploading();
            artifactUploadState.clearNumberOfFileUploadsExpected();
            artifactUploadState.clearNumberOfFileUploadsFailed();
            hideUploadStatusButton();
        } else if (uploadLogic.isUploadRunning()) {
            showUploadStatusButton();
            updateUploadStatusButtonCount();
        }

    }

    private void displayCompositeMessage() {
        uploadMessageBuilder.buildCompositeMessage()
                .ifPresent(value -> uiNotification.displayValidationError(value));
    }

    // private void createProcessButton() {
    // processBtn =
    // SPUIComponentProvider.getButton(UIComponentIdProvider.UPLOAD_PROCESS_BUTTON,
    // SPUILabelDefinitions.PROCESS, SPUILabelDefinitions.PROCESS, null, false,
    // null,
    // SPUIButtonStyleSmall.class);
    // processBtn.setIcon(FontAwesome.BELL);
    // processBtn.addStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
    // processBtn.addClickListener(this::displayConfirmWindow);
    // processBtn.setHtmlContentAllowed(true);
    // processBtn.setEnabled(false);
    // }

    // private void createDiscardBtn() {
    // discardBtn =
    // SPUIComponentProvider.getButton(UIComponentIdProvider.UPLOAD_DISCARD_BUTTON,
    // SPUILabelDefinitions.DISCARD, SPUILabelDefinitions.DISCARD, null, false,
    // null,
    // SPUIButtonStyleSmall.class);
    // discardBtn.setIcon(FontAwesome.TRASH_O);
    // discardBtn.addStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
    // discardBtn.addClickListener(this::discardUploadData);
    // }


    // /**
    // * Update pending action count.
    // */
    // private void updateActionCount() {
    // if (!artifactUploadState.getFileSelected().isEmpty()) {
    // processBtn.setCaption(SPUILabelDefinitions.PROCESS + "<div
    // class='unread'>"
    // + artifactUploadState.getFileSelected().size() + HTML_DIV);
    // } else {
    // processBtn.setCaption(SPUILabelDefinitions.PROCESS);
    // }
    // }

    private void displayDuplicateValidationMessage() {
        // check if streaming of all dropped files are completed
        if (artifactUploadState.getNumberOfFilesActuallyUploading().intValue() == artifactUploadState
                .getNumberOfFileUploadsExpected().intValue()) {
            displayCompositeMessage();
            uploadLogic.clearDuplicateFileNamesList();
        }
    }

    // private void discardUploadData(final Button.ClickEvent event) {
    // if (event.getButton().equals(discardBtn)) {
    // if (artifactUploadState.getFileSelected().isEmpty()) {
    // uiNotification.displayValidationError(i18n.getMessage("message.error.noFileSelected"));
    // } else {
    // clearUploadedFileDetails();
    // }
    // }
    // }

    // protected void clearUploadedFileDetails() {
    // uploadLogic.clearUploadDetailsIfAllUploadsFinished();
    // closeUploadStatusPopup();
    // }

    private void closeUploadStatusPopup() {
        uploadInfoWindow.close();
        artifactUploadState.setStatusPopupMinimized(false);
    }



    // private void setConfirmationPopupHeightWidth(final float newWidth, final
    // float newHeight) {
    // if (currentUploadConfirmationwindow != null) {
    // currentUploadConfirmationwindow.getUploadArtifactDetails().setWidth(HawkbitCommonUtil
    // .getArtifactUploadPopupWidth(newWidth,
    // SPUIDefinitions.MIN_UPLOAD_CONFIRMATION_POPUP_WIDTH),
    // Unit.PIXELS);
    // currentUploadConfirmationwindow.getUploadDetailsTable().setHeight(HawkbitCommonUtil
    // .getArtifactUploadPopupHeight(newHeight,
    // SPUIDefinitions.MIN_UPLOAD_CONFIRMATION_POPUP_HEIGHT),
    // Unit.PIXELS);
    // }
    // }

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
            uploadResultWindow.getUploadResultsWindow().setWidth(HawkbitCommonUtil.getArtifactUploadPopupWidth(newWidth,
                    SPUIDefinitions.MIN_UPLOAD_CONFIRMATION_POPUP_WIDTH), Unit.PIXELS);
            uploadResultWindow.getUploadResultTable().setHeight(HawkbitCommonUtil.getArtifactUploadPopupHeight(
                    newHeight, SPUIDefinitions.MIN_UPLOAD_CONFIRMATION_POPUP_HEIGHT), Unit.PIXELS);
        }
    }

    VaadinMessageSource getI18n() {
        return i18n;
    }

    void setCurrentUploadConfirmationwindow(final UploadConfirmationWindow currentUploadConfirmationwindow) {
        this.currentUploadConfirmationwindow = currentUploadConfirmationwindow;
    }

    private void onStartOfUpload() {
        showUploadStatusButton();
        updateUploadStatusButtonCount();
    }

    private void onUploadStreamingSuccess() {
        updateUploadStatusButtonCount();
    }

    private void onUploadStreamingFailure(final UploadStatusEvent event) {
        updateUploadStatusButtonCount();
    }

    /**
     * Called for every finished (succeeded or failed) upload.
     */
    private void onUploadFinished() {
        updateUploadStatusButtonCount();
        if (uploadLogic.isUploadComplete()) {
            displayDuplicateValidationMessage();
            hideUploadStatusButton();
        }
    }

    // TODO rollouts: remove
    void refreshArtifactDetailsLayout(final Long selectedBaseSoftwareModuleId) {
        final SoftwareModule softwareModule = softwareModuleManagement.get(selectedBaseSoftwareModuleId)
                .orElse(null);
        eventBus.publish(this, new SoftwareModuleEvent(SoftwareModuleEventType.ARTIFACTS_CHANGED, softwareModule));
    }

    public HorizontalLayout getFileUploadButtonLayout() {
        return fileUploadButtonLayout;
    }

    public UINotification getUINotification() {
        return uiNotification;
    }

    private void updateUploadStatusButtonCount() {
        final int uploadsPending = artifactUploadState.getNumberOfFileUploadsExpected().get()
                - artifactUploadState.getNumberOfFilesActuallyUploading().get();
        final int uploadsFailed = artifactUploadState.getNumberOfFileUploadsFailed().get();
        final StringBuilder builder = new StringBuilder("");
        if (uploadsFailed != 0) {
            if (uploadsPending != 0) {
                builder.append("<div class='error-count error-count-color'>" + uploadsFailed + HTML_DIV);
            } else {
                builder.append("<div class='unread error-count-color'>" + uploadsFailed + HTML_DIV);
            }
        }
        if (uploadsPending != 0) {
            builder.append("<div class='unread'>" + uploadsPending + HTML_DIV);
        }
        uploadStatusButton.setCaption(builder.toString());
    }

    private void onClickOfUploadStatusButton() {
        uploadInfoWindow.maximizeWindow();
    }

    private void showUploadStatusButton() {
        if (uploadStatusButton == null) {
            return;
        }
        uploadStatusButton.setVisible(true);
        updateUploadStatusButtonCount();
    }

    private void hideUploadStatusButton() {
        if (uploadStatusButton == null) {
            return;
        }
        uploadStatusButton.setVisible(false);
    }
}
