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
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent.UploadStatusEventType;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
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
    void onEvent(final UploadStatusEvent event) {
        final UploadStatusEventType uploadProgressEventType = event.getUploadStatusEventType();
        switch (uploadProgressEventType) {
        case UPLOAD_STARTED:
            ui.access(this::onStartOfUpload);
            break;

        case UPLOAD_FAILED:
        case UPLOAD_SUCCESSFUL:
        case UPLOAD_FINISHED:
        case UPLOAD_ABORTED_BY_USER:
            ui.access(this::onUploadFinished);
            break;
        case UPLOAD_IN_PROGRESS:
            break;
        default:
            throw new IllegalArgumentException("Enum " + UploadStatusEventType.class.getSimpleName()
                    + " doesn't contain value " + uploadProgressEventType);
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
        final FileTransferHandlerVaadinUpload uploadHandler = new FileTransferHandlerVaadinUpload(uploadLogic,
                multipartConfigElement.getMaxFileSize(), upload, softwareModuleManagement);
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

    // private void displayDuplicateValidationMessage() {
    // // check if streaming of all dropped files are completed
    // if (artifactUploadState.getNumberOfFilesActuallyUploading().intValue() ==
    // artifactUploadState
    // .getNumberOfFileUploadsExpected().intValue()) {
    // displayCompositeMessage();
    // uploadLogic.clearDuplicateFileNamesList();
    // }
    // }

    VaadinMessageSource getI18n() {
        return i18n;
    }

    private void onStartOfUpload() {
        showUploadStatusButton();
        updateUploadStatusButtonCount();
    }

    /**
     * Called for every finished (succeeded or failed) upload.
     */
    private void onUploadFinished() {
        updateUploadStatusButtonCount();
        if (uploadLogic.isUploadComplete()) {
            // displayDuplicateValidationMessage();
            hideUploadStatusButton();
        }
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
