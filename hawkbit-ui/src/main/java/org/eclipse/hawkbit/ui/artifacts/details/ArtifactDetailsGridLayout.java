/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.details;

import java.util.Arrays;
import java.util.List;

import javax.servlet.MultipartConfigElement;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.ui.artifacts.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress;
import org.eclipse.hawkbit.ui.artifacts.upload.UploadDropAreaLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.layout.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.common.layout.listener.GenericEventListener;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectionChangedListener;

/**
 * Display the details of the artifacts for a selected software module.
 */
public class ArtifactDetailsGridLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    private final ArtifactDetailsGridHeader artifactDetailsHeader;
    private final ArtifactDetailsGrid artifactDetailsGrid;
    private final UploadDropAreaLayout uploadDropAreaLayout;

    private final transient SelectionChangedListener<ProxySoftwareModule> selectionChangedListener;
    private final transient GenericEventListener<FileUploadProgress> fileUploadChangedListener;

    /**
     * Constructor for ArtifactDetailsLayout
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param artifactUploadState
     *            ArtifactUploadState
     * @param artifactDetailsGridLayoutUiState
     *            ArtifactDetailsGridLayoutUiState
     * @param artifactManagement
     *            ArtifactManagement
     * @param softwareManagement
     *            SoftwareModuleManagement
     * @param multipartConfigElement
     *            MultipartConfigElement
     */
    public ArtifactDetailsGridLayout(final UIConfiguration uiConfig, final ArtifactUploadState artifactUploadState,
            final ArtifactDetailsGridLayoutUiState artifactDetailsGridLayoutUiState,
            final ArtifactManagement artifactManagement, final SoftwareModuleManagement softwareManagement,
            final MultipartConfigElement multipartConfigElement) {
        this.artifactDetailsHeader = new ArtifactDetailsGridHeader(uiConfig, artifactDetailsGridLayoutUiState);
        this.artifactDetailsGrid = new ArtifactDetailsGrid(uiConfig, artifactManagement);

        if (uiConfig.getPermChecker().hasCreateRepositoryPermission()) {
            this.uploadDropAreaLayout = new UploadDropAreaLayout(uiConfig, artifactUploadState, multipartConfigElement,
                    softwareManagement, artifactManagement);

            buildLayout(artifactDetailsHeader, artifactDetailsGrid, uploadDropAreaLayout);
        } else {
            this.uploadDropAreaLayout = null;

            buildLayout(artifactDetailsHeader, artifactDetailsGrid);
        }

        final EventLayoutViewAware masterLayoutView = new EventLayoutViewAware(EventLayout.SM_LIST, EventView.UPLOAD);
        this.selectionChangedListener = new SelectionChangedListener<>(uiConfig.getEventBus(), masterLayoutView,
                getMasterEntityAwareComponents());
        this.fileUploadChangedListener = new GenericEventListener<>(uiConfig.getEventBus(),
                EventTopics.FILE_UPLOAD_CHANGED, this::onUploadChanged);
    }

    private List<MasterEntityAwareComponent<ProxySoftwareModule>> getMasterEntityAwareComponents() {
        return Arrays.asList(artifactDetailsHeader, artifactDetailsGrid.getMasterEntitySupport(), uploadDropAreaLayout);
    }

    /**
     * Checks progress on file upload
     *
     * @param fileUploadProgress
     *            FileUploadProgress
     */
    public void onUploadChanged(final FileUploadProgress fileUploadProgress) {
        if (uploadDropAreaLayout != null) {
            uploadDropAreaLayout.onUploadChanged(fileUploadProgress);
        }
    }

    /**
     * Maximize the artifact grid
     */
    public void maximize() {
        artifactDetailsGrid.createMaximizedContent();
        hideDetailsLayout();
    }

    /**
     * Minimize the artifact grid
     */
    public void minimize() {
        artifactDetailsGrid.createMinimizedContent();
        showDetailsLayout();
    }

    /**
     * Is called when view is shown to the user
     */
    public void restoreState() {
        artifactDetailsHeader.restoreState();

        if (uploadDropAreaLayout != null) {
            uploadDropAreaLayout.restoreState();
        }
    }

    /**
     * Unsubscribe the even listeners for selection change and fileupload
     */
    public void unsubscribeListener() {
        selectionChangedListener.unsubscribe();
        fileUploadChangedListener.unsubscribe();
    }
}
