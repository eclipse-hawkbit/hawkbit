/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.MultipartConfigElement;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Html5File;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.dnd.FileDropHandler;
import com.vaadin.ui.dnd.FileDropTarget;
import com.vaadin.ui.dnd.event.FileDropEvent;

/**
 * Container for drag and drop area in the upload view.
 */
public class UploadDropAreaLayout extends CustomComponent implements MasterEntityAwareComponent<ProxySoftwareModule> {
    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;
    private final UINotification uiNotification;

    private final ArtifactUploadState artifactUploadState;

    private final transient ArtifactManagement artifactManagement;
    private final transient SoftwareModuleManagement softwareManagement;

    private final transient MultipartConfigElement multipartConfigElement;
    private final transient Lock uploadLock = new ReentrantLock();

    private final UploadProgressButtonLayout uploadButtonLayout;
    private VerticalLayout dropAreaLayout;

    /**
     * Creates a new {@link UploadDropAreaLayout} instance.
     *
     * @param uiDependencies
     *            the {@link CommonUiDependencies}
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
    public UploadDropAreaLayout(final CommonUiDependencies uiDependencies, final ArtifactUploadState artifactUploadState,
            final MultipartConfigElement multipartConfigElement, final SoftwareModuleManagement softwareManagement,
            final ArtifactManagement artifactManagement) {
        this.i18n = uiDependencies.getI18n();
        this.uiNotification = uiDependencies.getUiNotification();
        this.artifactUploadState = artifactUploadState;
        this.multipartConfigElement = multipartConfigElement;
        this.softwareManagement = softwareManagement;
        this.artifactManagement = artifactManagement;

        this.uploadButtonLayout = new UploadProgressButtonLayout(i18n, uiDependencies.getEventBus(), artifactUploadState,
                multipartConfigElement, artifactManagement, softwareManagement, uploadLock);

        buildLayout();
    }

    private void buildLayout() {
        dropAreaLayout = new VerticalLayout();
        dropAreaLayout.setId(UIComponentIdProvider.UPLOAD_ARTIFACT_FILE_DROP_LAYOUT);
        dropAreaLayout.setMargin(false);
        dropAreaLayout.setSpacing(false);
        dropAreaLayout.addStyleName("upload-drop-area-layout-info");
        dropAreaLayout.setEnabled(false);
        dropAreaLayout.setHeightUndefined();

        final Label dropIcon = new Label(VaadinIcons.ARROW_DOWN.getHtml(), ContentMode.HTML);
        dropIcon.addStyleName("drop-icon");
        dropIcon.setWidth(null);
        dropAreaLayout.addComponent(dropIcon);

        final Label dropHereLabel = new Label(i18n.getMessage(UIMessageIdProvider.LABEL_DROP_AREA_UPLOAD));
        dropHereLabel.setWidth(null);
        dropAreaLayout.addComponent(dropHereLabel);

        uploadButtonLayout.setWidth(null);
        uploadButtonLayout.addStyleName("upload-button");
        dropAreaLayout.addComponent(uploadButtonLayout);

        new FileDropTarget<>(dropAreaLayout, new UploadFileDropHandler());

        setCompositionRoot(dropAreaLayout);
    }

    /**
     * Update the upload view on file drop
     *
     * @param masterEntity
     *            ProxySoftwareModule
     */
    @Override
    public void masterEntityChanged(final ProxySoftwareModule masterEntity) {
        final Long masterEntityId = masterEntity != null ? masterEntity.getId() : null;

        dropAreaLayout.setEnabled(masterEntityId != null);
        uploadButtonLayout.updateMasterEntityFilter(masterEntityId);
    }

    /**
     * Checks progress on file upload
     *
     * @param fileUploadProgress
     *            FileUploadProgress
     */
    public void onUploadChanged(final FileUploadProgress fileUploadProgress) {
        uploadButtonLayout.onUploadChanged(fileUploadProgress);
    }

    /**
     * Is called when view is shown to the user
     */
    public void restoreState() {
        uploadButtonLayout.restoreState();
    }

    /**
     * Get file drop area layout
     *
     * @return VerticalLayout
     */
    public VerticalLayout getDropAreaLayout() {
        return dropAreaLayout;
    }

    /**
     * Get File upload button layout
     *
     * @return UploadProgressButtonLayout
     */
    public UploadProgressButtonLayout getUploadButtonLayout() {
        return uploadButtonLayout;
    }

    private class UploadFileDropHandler implements FileDropHandler<VerticalLayout> {

        private static final long serialVersionUID = 1L;

        /**
         * Validates the file drop events and triggers the upload
         *
         * @param event
         *            FileDropEvent
         */
        @Override
        public void drop(final FileDropEvent<VerticalLayout> event) {
            if (validate(event)) {
                // selected software module at the time of file drop is
                // considered for upload
                final Long lastSelectedSmId = artifactUploadState.getSmGridLayoutUiState().getSelectedEntityId();
                if (lastSelectedSmId != null) {
                    uploadFilesForSoftwareModule(event.getFiles(), lastSelectedSmId);
                }
            }
        }

        private void uploadFilesForSoftwareModule(final Collection<Html5File> files, final Long softwareModuleId) {
            final SoftwareModule softwareModule = softwareManagement.get(softwareModuleId).orElse(null);

            boolean duplicateFound = false;

            for (final Html5File file : files) {
                if (artifactUploadState.isFileInUploadState(file.getFileName(), softwareModule)) {
                    duplicateFound = true;
                } else {
                    file.setStreamVariable(new FileTransferHandlerStreamVariable(file.getFileName(), file.getFileSize(),
                            multipartConfigElement.getMaxFileSize(), file.getType(), softwareModule, artifactManagement,
                            i18n, uploadLock));
                }
            }
            if (duplicateFound) {
                uiNotification.displayValidationError(i18n.getMessage("message.no.duplicateFiles"));
            }
        }

        private boolean validate(final FileDropEvent<VerticalLayout> event) {
            // check if drop is valid.If valid ,check if software module is
            // selected.
            if (!isFilesDropped(event)) {
                uiNotification.displayValidationError(i18n.getMessage("message.drop.onlyFiles"));
                return false;
            }
            return validateSoftwareModuleSelection();
        }

        private boolean isFilesDropped(final FileDropEvent<VerticalLayout> event) {
            return event.getFiles() != null;
        }

        private boolean validateSoftwareModuleSelection() {
            final Long lastSelectedSmId = artifactUploadState.getSmGridLayoutUiState().getSelectedEntityId();

            if (lastSelectedSmId == null) {
                uiNotification.displayValidationError(i18n.getMessage("message.error.noSwModuleSelected"));
                return false;
            }

            return true;
        }
    }
}
