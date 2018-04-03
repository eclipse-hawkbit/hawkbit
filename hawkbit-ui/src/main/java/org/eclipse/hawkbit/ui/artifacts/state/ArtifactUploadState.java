/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.state;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadId;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress;
import org.eclipse.hawkbit.ui.common.ManagementEntityState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * User status of Artifact upload.
 */
@VaadinSessionScope
@SpringComponent
public class ArtifactUploadState implements ManagementEntityState, Serializable {

    private static final long serialVersionUID = 1L;

    private final SoftwareModuleFilters softwareModuleFilters;

    private final Map<Long, String> deleteSofwareModules = new HashMap<>();

    private transient Optional<Long> selectedBaseSwModuleId = Optional.empty();

    private final Map<String, SoftwareModule> baseSwModuleList = new HashMap<>();

    private Set<Long> selectedSoftwareModules = Collections.emptySet();

    private boolean swTypeFilterClosed;

    private boolean swModuleTableMaximized;

    private boolean artifactDetailsMaximized;

    private final Set<String> selectedDeleteSWModuleTypes = new HashSet<>();

    private boolean noDataAvilableSoftwareModule;

    private boolean statusPopupMinimized;

    private final AtomicInteger numberOfFileUploadsExpected = new AtomicInteger();

    private final AtomicInteger numberOfFilesActuallyUploading = new AtomicInteger();

    private final AtomicInteger numberOfFileUploadsFailed = new AtomicInteger();

    /**
     * Map that holds all files that were selected for upload. They remain in
     * the list even if upload fails or was abortedby the user.
     */
    private final Map<FileUploadId, FileUploadProgress> filesInUploadState = new HashMap<>();

    @Autowired
    ArtifactUploadState(final SoftwareModuleFilters softwareModuleFilters) {
        this.softwareModuleFilters = softwareModuleFilters;
    }

    public void clearNumberOfFileUploadsFailed() {
        numberOfFileUploadsFailed.set(0);
    }

    public void incrementNumberOfFileUploadsFailed() {
        numberOfFileUploadsFailed.incrementAndGet();
    }

    public void decrementNumberOfFileUploadsFailed() {
        numberOfFileUploadsFailed.decrementAndGet();
    }

    public AtomicInteger getNumberOfFileUploadsFailed() {
        return numberOfFileUploadsFailed;
    }

    public void clearNumberOfFilesActuallyUploading() {
        numberOfFilesActuallyUploading.set(0);
    }

    public void incrementNumberOfFilesActuallyUploading() {
        numberOfFilesActuallyUploading.incrementAndGet();
    }

    public void decrementNumberOfFilesActuallyUploading() {
        numberOfFilesActuallyUploading.decrementAndGet();
    }

    public AtomicInteger getNumberOfFilesActuallyUploading() {
        return numberOfFilesActuallyUploading;
    }

    public void clearNumberOfFileUploadsExpected() {
        numberOfFileUploadsExpected.set(0);
    }

    public void incrementNumberOfFileUploadsExpected() {
        numberOfFileUploadsExpected.incrementAndGet();
    }

    public void decrementNumberOfFileUploadsExpected() {
        numberOfFileUploadsExpected.decrementAndGet();
    }

    public AtomicInteger getNumberOfFileUploadsExpected() {
        return numberOfFileUploadsExpected;
    }

    public void setStatusPopupMinimized(final boolean statusPopupMinimized) {
        this.statusPopupMinimized = statusPopupMinimized;
    }

    public boolean isStatusPopupMinimized() {
        return statusPopupMinimized;
    }

    public SoftwareModuleFilters getSoftwareModuleFilters() {
        return softwareModuleFilters;
    }

    public Map<Long, String> getDeleteSofwareModules() {
        return deleteSofwareModules;
    }

    public Optional<Long> getSelectedBaseSwModuleId() {
        return selectedBaseSwModuleId;
    }

    public void clearBaseSwModuleList() {
        baseSwModuleList.clear();
    }

    public String getKeyForSoftwareModule(final SoftwareModule softwareModule) {
        return HawkbitCommonUtil.getFormattedNameVersion(softwareModule.getName(), softwareModule.getVersion());
    }


    public void addBaseSoftwareModule(final SoftwareModule softwareModule) {
        final String currentBaseSoftwareModuleKey = HawkbitCommonUtil.getFormattedNameVersion(softwareModule.getName(),
                softwareModule.getVersion());
        if (!baseSwModuleList.keySet().contains(currentBaseSoftwareModuleKey)) {
            baseSwModuleList.put(currentBaseSoftwareModuleKey, softwareModule);
        }
    }

    public Map<String, SoftwareModule> getBaseSwModuleList() {
        return baseSwModuleList;
    }

    public Set<Long> getSelectedSoftwareModules() {
        return selectedSoftwareModules;
    }

    @Override
    public void setLastSelectedEntityId(final Long value) {
        this.selectedBaseSwModuleId = Optional.ofNullable(value);
    }

    @Override
    public void setSelectedEnitities(final Set<Long> values) {
        this.selectedSoftwareModules = values;
    }

    public boolean isSwTypeFilterClosed() {
        return swTypeFilterClosed;
    }

    public void setSwTypeFilterClosed(final boolean swTypeFilterClosed) {
        this.swTypeFilterClosed = swTypeFilterClosed;
    }

    public boolean isSwModuleTableMaximized() {
        return swModuleTableMaximized;
    }

    public void setSwModuleTableMaximized(final boolean swModuleTableMaximized) {
        this.swModuleTableMaximized = swModuleTableMaximized;
    }

    public Set<String> getSelectedDeleteSWModuleTypes() {
        return selectedDeleteSWModuleTypes;
    }

    public boolean isArtifactDetailsMaximized() {
        return artifactDetailsMaximized;
    }

    public void setArtifactDetailsMaximized(final boolean artifactDetailsMaximized) {
        this.artifactDetailsMaximized = artifactDetailsMaximized;
    }

    public boolean isNoDataAvilableSoftwareModule() {
        return noDataAvilableSoftwareModule;
    }

    public void setNoDataAvilableSoftwareModule(final boolean noDataAvilableSoftwareModule) {
        this.noDataAvilableSoftwareModule = noDataAvilableSoftwareModule;
    }

    public void addFileToUploadState(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        filesInUploadState.put(fileUploadId, fileUploadProgress);
    }

    public void removeFilesInUploadState(final Collection<FileUploadId> filesToRemove) {
        for (final FileUploadId fileUploadId : filesToRemove) {
            filesInUploadState.remove(fileUploadId);
        }
    }

    public void removeFileInUploadState(final FileUploadId fileUploadId) {
        filesInUploadState.remove(fileUploadId);
    }

    public Map<FileUploadId, FileUploadProgress> getFilesInUploadState()
    {
        return filesInUploadState;
    }

    public void clearFilesInUploadState() {
        filesInUploadState.clear();
    }
}
