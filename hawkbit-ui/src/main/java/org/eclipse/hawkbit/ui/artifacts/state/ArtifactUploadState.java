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
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * Map that holds all files that were selected for upload. They remain in
     * the list even if upload fails, succeeds or is aborted by the user.
     */
    private final Map<FileUploadId, FileUploadProgress> overallFilesInUploadProcess = new ConcurrentHashMap<>();

    private final Set<FileUploadId> succeededUploads = ConcurrentHashMap.newKeySet();

    private final Set<FileUploadId> filesInUploadProgressState = ConcurrentHashMap.newKeySet();

    private final Set<FileUploadId> failedUploads = ConcurrentHashMap.newKeySet();


    @Autowired
    ArtifactUploadState(final SoftwareModuleFilters softwareModuleFilters) {
        this.softwareModuleFilters = softwareModuleFilters;
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

    public void addFileToOverallUploadProcessList(final FileUploadId fileUploadId,
            final FileUploadProgress fileUploadProgress) {
        overallFilesInUploadProcess.put(fileUploadId, fileUploadProgress);
    }

    public void updateFileProgressInOverallUploadProcessList(final FileUploadId fileUploadId,
            final FileUploadProgress fileUploadProgress) {
        addFileToOverallUploadProcessList(fileUploadId, fileUploadProgress);
    }

    public void removeFilesFromOverallUploadProcessList(final Collection<FileUploadId> filesToRemove) {
        for (final FileUploadId fileUploadId : filesToRemove) {
            overallFilesInUploadProcess.remove(fileUploadId);
        }
    }

    public void removeFileFromOverallUploadProcessList(final FileUploadId fileUploadId) {
        overallFilesInUploadProcess.remove(fileUploadId);
    }

    public Map<FileUploadId, FileUploadProgress> getAllFilesFromOverallUploadProcessList()
    {
        return overallFilesInUploadProcess;
    }

    public void clearOverallUploadProcessList() {
        overallFilesInUploadProcess.clear();
    }

    public void addFileToSucceededState(final FileUploadId fileUploadId) {
        succeededUploads.add(fileUploadId);
    }

    public Set<FileUploadId> getFilesInSucceededState() {
        return succeededUploads;
    }

    public void clearFilesInSucceededState() {
        succeededUploads.clear();
    }

    public void addFileToFailedState(final FileUploadId fileUploadId) {
        failedUploads.add(fileUploadId);
    }

    public Set<FileUploadId> getFilesInFailedState() {
        return failedUploads;
    }

    public void clearFilesInFailedState() {
        failedUploads.clear();
    }

    public void addFileToUploadInProgressState(final FileUploadId fileUploadId) {
        filesInUploadProgressState.add(fileUploadId);
    }

    public Set<FileUploadId> getFilesInUploadProgressState() {
        return filesInUploadProgressState;
    }

    public void removeFileInUploadProgressState(final FileUploadId fileUploadId) {
        filesInUploadProgressState.remove(fileUploadId);
    }

    public void clearFilesInUploadProgressState() {
        filesInUploadProgressState.clear();
    }

    public FileUploadProgress getFileUploadProgress(final FileUploadId fileUploadId) {
        return overallFilesInUploadProcess.get(fileUploadId);
    }
}
