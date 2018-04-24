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
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent.UploadStatusEventType;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
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
public class UploadProgressButtonLayout extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final UploadProgressInfoWindow uploadInfoWindow;

    private final VaadinMessageSource i18n;

    private final transient MultipartConfigElement multipartConfigElement;

    private final UI ui;

    private HorizontalLayout fileUploadButtonLayout;

    private Button uploadProgressButton;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final Upload upload;

    private FileTransferHandlerVaadinUpload uploadHandler;

    private final ArtifactManagement artifactManagement;

    private final ArtifactUploadState artifactUploadState;

    public UploadProgressButtonLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final ArtifactUploadState artifactUploadState, final MultipartConfigElement multipartConfigElement,
            final ArtifactManagement artifactManagement, final SoftwareModuleManagement softwareManagement) {
        this.artifactUploadState = artifactUploadState;
        this.artifactManagement = artifactManagement;
        this.uploadInfoWindow = new UploadProgressInfoWindow(eventBus, artifactUploadState, i18n);
        this.uploadInfoWindow.addCloseListener(event -> {
            // ensure that the progress button is hidden when the progress
            // window is closed and no more uploads running
            if (artifactUploadState.areAllUploadsFinished()) {
                hideUploadProgressButton();
            }
        });
        this.i18n = i18n;
        this.multipartConfigElement = multipartConfigElement;
        this.softwareModuleManagement = softwareManagement;
        this.upload = new Upload();

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
            break;
        case UPLOAD_SUCCESSFUL:
            break;
        case UPLOAD_FINISHED:
            ui.access(this::onUploadFinished);
            break;
        case UPLOAD_IN_PROGRESS:
            break;
        default:
            throw new IllegalArgumentException("Enum " + UploadStatusEventType.class.getSimpleName()
                    + " doesn't contain value " + uploadProgressEventType);
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SoftwareModuleEvent event) {
        final BaseEntityEventType eventType = event.getEventType();
        if (eventType == BaseEntityEventType.SELECTED_ENTITY) {
            ui.access(() -> {
                if (artifactUploadState.isNoSoftwareModuleSelected()
                        || artifactUploadState.isMoreThanOneSoftwareModulesSelected()) {
                    upload.setEnabled(false);
                } else {
                    upload.setEnabled(true);
                }
            });
        }
    }

    private void createComponents() {
        uploadProgressButton = SPUIComponentProvider.getButton(UIComponentIdProvider.UPLOAD_STATUS_BUTTON, "", "", "",
                false, null, SPUIButtonStyleSmall.class);
        uploadProgressButton.setStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
        uploadProgressButton.addStyleName(SPUIStyleDefinitions.UPLOAD_PROGRESS_INDICATOR_STYLE);
        uploadProgressButton.setIcon(null);
        uploadProgressButton.setWidth("100px");
        uploadProgressButton.setHtmlContentAllowed(true);
        uploadProgressButton.addClickListener(event -> onClickOfUploadProgressButton());
        uploadProgressButton.setVisible(false);
    }

    private void buildLayout() {

        uploadHandler = new FileTransferHandlerVaadinUpload(multipartConfigElement.getMaxFileSize(),
                softwareModuleManagement, artifactManagement, i18n);
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
        fileUploadButtonLayout.addComponent(uploadProgressButton);
        fileUploadButtonLayout.setComponentAlignment(uploadProgressButton, Alignment.MIDDLE_RIGHT);
        setMargin(false);

        setSizeFull();
        setSpacing(true);
    }

    private void restoreState() {
        if (artifactUploadState.areAllUploadsFinished()) {
            artifactUploadState.clearUploadDetails();
            hideUploadProgressButton();
        } else if (artifactUploadState.isAtLeastOneUploadInProgress()) {
            showUploadProgressButton();
        }
    }

    private void onStartOfUpload() {
        showUploadProgressButton();
    }

    /**
     * Called for every finished (succeeded or failed) upload.
     */
    private void onUploadFinished() {
        if (artifactUploadState.areAllUploadsFinished()) {
            hideUploadProgressButton();
        }
    }

    public HorizontalLayout getFileUploadButtonLayout() {
        return fileUploadButtonLayout;
    }

    private void onClickOfUploadProgressButton() {
        uploadInfoWindow.maximizeWindow();
    }

    private void showUploadProgressButton() {
        uploadProgressButton.setVisible(true);
    }

    private void hideUploadProgressButton() {
        uploadProgressButton.setVisible(false);
    }
}
