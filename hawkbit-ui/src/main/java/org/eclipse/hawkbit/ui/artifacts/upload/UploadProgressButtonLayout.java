/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.util.concurrent.locks.Lock;

import javax.servlet.MultipartConfigElement;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.ArtifactUploadState;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;

/**
 * Container for upload and progress button.
 */
public class UploadProgressButtonLayout extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    private final ArtifactUploadState artifactUploadState;

    private final transient ArtifactManagement artifactManagement;
    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final transient MultipartConfigElement multipartConfigElement;
    private final Upload upload;
    private final transient Lock uploadLock;

    private final UploadProgressInfoWindow uploadInfoWindow;

    private Button uploadProgressButton;

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
     * @param uploadLock
     *            A common upload lock that enforced sequential upload within an
     *            UI instance
     */
    public UploadProgressButtonLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final ArtifactUploadState artifactUploadState, final MultipartConfigElement multipartConfigElement,
            final ArtifactManagement artifactManagement, final SoftwareModuleManagement softwareManagement,
            final Lock uploadLock) {
        this.artifactUploadState = artifactUploadState;
        this.artifactManagement = artifactManagement;
        this.uploadInfoWindow = new UploadProgressInfoWindow(i18n, artifactUploadState);
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
        this.uploadLock = uploadLock;

        createComponents();
        buildLayout();
    }

    /**
     * Perform specific tasks based on the file upload status
     *
     * @param fileUploadProgress
     *          FileUploadProgress
     */
    public void onUploadChanged(final FileUploadProgress fileUploadProgress) {
        final FileUploadProgress.FileUploadStatus uploadProgressEventType = fileUploadProgress.getFileUploadStatus();

        switch (uploadProgressEventType) {
        case UPLOAD_STARTED:
            UI.getCurrent().access(() -> {
                onStartOfUpload();
                uploadInfoWindow.onUploadStarted(fileUploadProgress);
            });
            break;
        case UPLOAD_FAILED:
        case UPLOAD_SUCCESSFUL:
        case UPLOAD_IN_PROGRESS:
            UI.getCurrent().access(() -> uploadInfoWindow.updateUploadProgressInfoRowObject(fileUploadProgress));
            break;
        case UPLOAD_FINISHED:
            UI.getCurrent().access(() -> {
                onUploadFinished();
                uploadInfoWindow.onUploadFinished();
            });
            break;
        default:
            throw new IllegalArgumentException("Enum " + FileUploadProgress.FileUploadStatus.class.getSimpleName()
                    + " doesn't contain value " + uploadProgressEventType);
        }
    }

    /**
     * Enable the upload view after upload is finished
     *
     * @param masterEntityId
     *          Long
     */
    public void updateMasterEntityFilter(final Long masterEntityId) {
        upload.setEnabled(masterEntityId != null && artifactUploadState.areAllUploadsFinished());
    }

    private void createComponents() {
        uploadProgressButton = SPUIComponentProvider.getButton(UIComponentIdProvider.UPLOAD_STATUS_BUTTON, "", "", "",
                false, null, SPUIButtonStyleNoBorder.class);
        uploadProgressButton.addStyleName(SPUIStyleDefinitions.UPLOAD_PROGRESS_INDICATOR_STYLE);
        uploadProgressButton.addClickListener(event -> showUploadInfoWindow());
    }

    private void buildLayout() {
        final FileTransferHandlerVaadinUpload uploadHandler = new FileTransferHandlerVaadinUpload(
                multipartConfigElement.getMaxFileSize(), softwareModuleManagement, artifactManagement, i18n,
                uploadLock);
        upload.setButtonCaption(i18n.getMessage("upload.file"));
        upload.setReceiver(uploadHandler);
        upload.addSucceededListener(uploadHandler);
        upload.addFailedListener(uploadHandler);
        upload.addFinishedListener(uploadHandler);
        upload.addProgressListener(uploadHandler);
        upload.addStartedListener(uploadHandler);
        upload.setId(UIComponentIdProvider.UPLOAD_BUTTON);

        addComponent(upload);
        setSizeFull();
        setMargin(false);
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

    /**
     * Maximize the file upload view
     */
    public void showUploadInfoWindow() {
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
