/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.state;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadId;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress;
import org.eclipse.hawkbit.ui.common.ManagementEntityState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactUploadState.class);

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

    private final Set<FileUploadId> failedUploads = ConcurrentHashMap.newKeySet();

    private final ReentrantReadWriteLock fileStateRWLock = new ReentrantReadWriteLock();


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

    public boolean isMoreThanOneSoftwareModulesSelected() {
        return getSelectedSoftwareModules().size() > 1;
    }

    public boolean isNoSoftwareModuleSelected() {
        return !getSelectedBaseSwModuleId().isPresent();
    }

    public void removeFilesFromOverallUploadProcessList(final Collection<FileUploadId> filesToRemove) {
        fileStateRWLock.writeLock().lock();
        overallFilesInUploadProcess.keySet().removeAll(filesToRemove);
        fileStateRWLock.writeLock().unlock();
    }

    public Set<FileUploadId> getAllFileUploadIdsFromOverallUploadProcessList() {
        fileStateRWLock.readLock().lock();
        final Set<FileUploadId> set = Collections.unmodifiableSet(overallFilesInUploadProcess.keySet());
        fileStateRWLock.readLock().unlock();

        return set;
    }

    public Collection<FileUploadProgress> getAllFileUploadProgressValuesFromOverallUploadProcessList() {
        fileStateRWLock.readLock().lock();
        final Collection<FileUploadProgress> collection = Collections
                .unmodifiableCollection(overallFilesInUploadProcess.values());
        fileStateRWLock.readLock().unlock();

        return collection;
    }

    public Set<FileUploadId> getFilesInSucceededState() {
        fileStateRWLock.readLock().lock();
        final Set<FileUploadId> set = Collections.unmodifiableSet(succeededUploads);
        fileStateRWLock.readLock().unlock();

        return set;
    }

    public Set<FileUploadId> getFilesInFailedState() {
        fileStateRWLock.readLock().lock();
        final Set<FileUploadId> set = Collections.unmodifiableSet(failedUploads);
        fileStateRWLock.readLock().unlock();

        return set;
    }

    public FileUploadProgress getFileUploadProgress(final FileUploadId fileUploadId) {
        fileStateRWLock.readLock().lock();
        final FileUploadProgress fileUploadProgress = overallFilesInUploadProcess.get(fileUploadId);
        fileStateRWLock.readLock().unlock();

        return fileUploadProgress;
    }

    public void uploadInProgress(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        fileStateRWLock.writeLock().lock();
        overallFilesInUploadProcess.put(fileUploadId, fileUploadProgress);
        fileStateRWLock.writeLock().unlock();
    }

    public void uploadFailed(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        fileStateRWLock.writeLock().lock();
        overallFilesInUploadProcess.put(fileUploadId, fileUploadProgress);
        failedUploads.add(fileUploadId);
        fileStateRWLock.writeLock().unlock();
    }

    public void uploadSucceeded(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        fileStateRWLock.writeLock().lock();
        overallFilesInUploadProcess.put(fileUploadId, fileUploadProgress);
        succeededUploads.add(fileUploadId);
        fileStateRWLock.writeLock().unlock();
    }

    public boolean isFileInUploadState(final FileUploadId fileUploadId) {
        fileStateRWLock.readLock().lock();
        final boolean isFileInUploadState = overallFilesInUploadProcess.containsKey(fileUploadId);
        fileStateRWLock.readLock().unlock();

        return isFileInUploadState;
    }

    public boolean isFileInUploadState(final String filename, final SoftwareModule softwareModule) {
        return isFileInUploadState(new FileUploadId(filename, softwareModule));
    }

    public boolean isAtLeastOneUploadInProgress() {
        return getInProgressCount() > 0;
    }

    public boolean areAllUploadsFinished() {
        return getInProgressCount() == 0;
    }

    private int getInProgressCount() {
        fileStateRWLock.readLock().lock();
        final int succeededUploadCount = succeededUploads.size();
        final int failedUploadCount = failedUploads.size();
        final int overallUploadCount = overallFilesInUploadProcess.size();
        fileStateRWLock.readLock().unlock();

        final int inProgressCount = overallUploadCount - failedUploadCount - succeededUploadCount;

        assertFileStateConsistency(inProgressCount, overallUploadCount, succeededUploadCount, failedUploadCount);

        return inProgressCount;
    }

    private void assertFileStateConsistency(final int inProgressCount, final int overallUploadCount,
            final int succeededUploadCount, final int failedUploadCount) {
        if (inProgressCount < 0) {
            LOG.error("IllegalState: \n{}",
                    getStateListslogMessage(overallUploadCount, succeededUploadCount, failedUploadCount));
            throw new IllegalStateException();
        }
    }

    String getStateListslogMessage(final int overallUploadCount, final int succeededUploadCount,
            final int failedUploadCount) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("Overall uploads: " + overallUploadCount);
        buffer.append("\n");
        buffer.append("succeeded uploads: " + succeededUploadCount);
        buffer.append("\n");
        buffer.append("Failed Uploads: " + failedUploadCount);
        return buffer.toString();
    }

    public void clearFileStates() {
        fileStateRWLock.writeLock().lock();
        failedUploads.clear();
        succeededUploads.clear();
        overallFilesInUploadProcess.clear();
        fileStateRWLock.writeLock().unlock();
    }

    /**
     * Clears all temp data collected while uploading files.
     */
    public void clearUploadDetails() {
        LOG.debug("Cleaning up temp data...");
        // delete file system zombies
        for (final FileUploadProgress fileUploadProgress : getAllFileUploadProgressValuesFromOverallUploadProcessList()) {
            if (StringUtils.isNoneBlank(fileUploadProgress.getFilePath())) {
                FileUtils.deleteQuietly(new File(fileUploadProgress.getFilePath()));
            }
        }
        clearBaseSwModuleList();

        clearFileStates();
    }

}
