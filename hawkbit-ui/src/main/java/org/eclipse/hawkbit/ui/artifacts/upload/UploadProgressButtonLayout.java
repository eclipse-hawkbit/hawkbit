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
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Container for upload and progress button.
 */
public class UploadProgressButtonLayout extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final UploadProgressInfoWindow uploadInfoWindow;

    private final VaadinMessageSource i18n;

    private final transient MultipartConfigElement multipartConfigElement;

    private final UI ui;

    private Button uploadProgressButton;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final UploadFixed upload;

    private final transient ArtifactManagement artifactManagement;

    private final ArtifactUploadState artifactUploadState;

    /**
     * Creates a new {@link UploadProgressButtonLayout} instance.
     * 
     * @param i18n
     *            the {@link VaadinMessageSource}
     * @param eventBus
     *            the {@link UIEventBus} for listening to ui events
     * @param artifactUploadState
     *            the {@link ArtifactUploadState} for state information
     * @param multipartConfigElement
     *            the {@link MultipartConfigElement}
     * @param softwareManagement
     *            the {@link SoftwareModuleManagement} for retrieving the
     *            {@link SoftwareModule}
     * @param artifactManagement
     *            the {@link ArtifactManagement} for storing the uploaded
     *            artifacts
     */
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
        this.upload = new UploadFixed();

        createComponents();
        buildLayout();
        restoreState();
        ui = UI.getCurrent();

        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final FileUploadProgress fileUploadProgress) {
        final FileUploadProgress.FileUploadStatus uploadProgressEventType = fileUploadProgress.getFileUploadStatus();
        switch (uploadProgressEventType) {
        case UPLOAD_STARTED:
            ui.access(this::onStartOfUpload);
            break;
        case UPLOAD_FAILED:
        case UPLOAD_SUCCESSFUL:
        case UPLOAD_IN_PROGRESS:
            break;
        case UPLOAD_FINISHED:
            ui.access(this::onUploadFinished);
            break;
        default:
            throw new IllegalArgumentException("Enum " + FileUploadProgress.FileUploadStatus.class.getSimpleName()
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
                } else if (artifactUploadState.areAllUploadsFinished()) {
                    upload.setEnabled(true);
                }
            });
        }
    }

    private void createComponents() {
        uploadProgressButton = SPUIComponentProvider.getButton(UIComponentIdProvider.UPLOAD_STATUS_BUTTON, "", "", "",
                false, null, SPUIButtonStyleNoBorder.class);
        uploadProgressButton.addStyleName(SPUIStyleDefinitions.UPLOAD_PROGRESS_INDICATOR_STYLE);
        uploadProgressButton.setIcon(null);
        uploadProgressButton.setHtmlContentAllowed(true);
        uploadProgressButton.addClickListener(event -> onClickOfUploadProgressButton());
    }

    private void buildLayout() {
        final FileTransferHandlerVaadinUpload uploadHandler = new FileTransferHandlerVaadinUpload(
                multipartConfigElement.getMaxFileSize(), softwareModuleManagement, artifactManagement, i18n);
        upload.setButtonCaption(i18n.getMessage("upload.file"));
        upload.setImmediate(true);
        upload.setReceiver(uploadHandler);
        upload.addSucceededListener(uploadHandler);
        upload.addFailedListener(uploadHandler);
        upload.addFinishedListener(uploadHandler);
        upload.addProgressListener(uploadHandler);
        upload.addStartedListener(uploadHandler);
        upload.setId(UIComponentIdProvider.UPLOAD_BUTTON);

        addComponent(upload);
        setSpacing(true);
    }

    /**
     * Is called when view is shown to the user
     */
    public void restoreState() {
        if (artifactUploadState.areAllUploadsFinished()) {
            artifactUploadState.clearUploadTempData();
            hideUploadProgressButton();
            upload.setEnabled(true);
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
            upload.setEnabled(true);
            artifactUploadState.clearUploadTempData();
        }
    }

    private void onClickOfUploadProgressButton() {
        uploadInfoWindow.maximizeWindow();
    }

    private void showUploadProgressButton() {
        removeComponent(upload);
        addComponent(uploadProgressButton);
    }

    private void hideUploadProgressButton() {
        removeComponent(uploadProgressButton);
        addComponent(upload);
    }
}
