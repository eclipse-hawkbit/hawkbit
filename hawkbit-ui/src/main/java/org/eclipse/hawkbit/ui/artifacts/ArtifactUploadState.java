/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.details.ArtifactDetailsGridLayoutUiState;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadId;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress.FileUploadStatus;
import org.eclipse.hawkbit.ui.common.state.GridLayoutUiState;
import org.eclipse.hawkbit.ui.common.state.TypeFilterLayoutUiState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * User status of Artifact upload.
 */
@VaadinSessionScope
@SpringComponent
public class ArtifactUploadState implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactUploadState.class);

    private final TypeFilterLayoutUiState smTypeFilterLayoutUiState;
    private final GridLayoutUiState smGridLayoutUiState;
    private final ArtifactDetailsGridLayoutUiState artifactDetailsGridLayoutUiState;

    private boolean statusPopupMinimized;

    /**
     * Map that holds all files that were selected for upload. They remain in
     * the list even if upload fails, succeeds or is aborted by the user.
     */
    private Map<FileUploadId, FileUploadProgress> overallFilesInUploadProcess;

    /**
     * Constructor for ArtifactUploadState
     */
    public ArtifactUploadState() {
        this.smTypeFilterLayoutUiState = new TypeFilterLayoutUiState();
        this.smGridLayoutUiState = new GridLayoutUiState();
        this.artifactDetailsGridLayoutUiState = new ArtifactDetailsGridLayoutUiState();
    }

    /**
     * Minimize or maximize the status popup view
     *
     * @param statusPopupMinimized
     *          boolean
     */
    public void setStatusPopupMinimized(final boolean statusPopupMinimized) {
        this.statusPopupMinimized = statusPopupMinimized;
    }

    /**
     * Checks if the status popup view is in minimized state
     *
     * @return boolean
     */
    public boolean isStatusPopupMinimized() {
        return statusPopupMinimized;
    }

    /**
     * Get the Software module type filter UI state
     *
     * @return TypeFilterLayoutUiState
     */
    public TypeFilterLayoutUiState getSmTypeFilterLayoutUiState() {
        return smTypeFilterLayoutUiState;
    }

    /**
     *  Get the Software module grid UI state
     *
     * @return GridLayoutUiState
     */
    public GridLayoutUiState getSmGridLayoutUiState() {
        return smGridLayoutUiState;
    }

    /**
     * Get the Artifact details grid UI state
     *
     * @return ArtifactDetailsGridLayoutUiState
     */
    public ArtifactDetailsGridLayoutUiState getArtifactDetailsGridLayoutUiState() {
        return artifactDetailsGridLayoutUiState;
    }

    /**
     * Remove all the files from the upload process list
     *
     * @param filesToRemove
     *          Collection of fie upload ID
     */
    public void removeFilesFromOverallUploadProcessList(final Collection<FileUploadId> filesToRemove) {
        getOverallFilesInUploadProcessMap().keySet().removeAll(filesToRemove);
    }

    /**
     * Get all the IDs of uploaded files from the upload process
     *
     * @return List of IDs of  uploaded files
     */
    public Set<FileUploadId> getAllFileUploadIdsFromOverallUploadProcessList() {
        return Collections.unmodifiableSet(getOverallFilesInUploadProcessMap().keySet());
    }

    /**
     * Get file upload progress values from process list
     *
     * @return List of FileUploadProgress
     */
    public Collection<FileUploadProgress> getAllFileUploadProgressValuesFromOverallUploadProcessList() {
        return Collections.unmodifiableCollection(getOverallFilesInUploadProcessMap().values());
    }

    /**
     * Get all the IDs of files from the failed upload
     *
     * @return List of IDs of  uploaded files
     */
    public Set<FileUploadId> getFilesInFailedState() {
        return Collections.unmodifiableSet(getFailedUploads());
    }

    /**
     * Get upload progress of the file
     *
     * @param fileUploadId
     *          FileUploadId
     *
     * @return FileUploadProgress
     */
    public FileUploadProgress getFileUploadProgress(final FileUploadId fileUploadId) {
        return getOverallFilesInUploadProcessMap().get(fileUploadId);
    }

    /**
     * Get all files that were selected for upload
     *
     * @param fileUploadId
     *          FileUploadId
     * @param fileUploadProgress
     *          FileUploadProgress
     */
    public void updateFileUploadProgress(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        getOverallFilesInUploadProcessMap().put(fileUploadId, fileUploadProgress);
    }

    /**
     * Check upload state of the file
     *
     * @param fileUploadId
     *          FileUploadId
     *
     * @return boolean
     */
    public boolean isFileInUploadState(final FileUploadId fileUploadId) {
        return getOverallFilesInUploadProcessMap().containsKey(fileUploadId);
    }

    /**
     * Check upload state of the file link to the related software module
     *
     * @param filename
     *          Name of the file
     *
     * @param softwareModule
     *          the {@link SoftwareModule} for which the file is uploaded
     *
     * @return boolean
     */
    public boolean isFileInUploadState(final String filename, final SoftwareModule softwareModule) {
        return isFileInUploadState(new FileUploadId(filename, softwareModule));
    }

    /**
     * Check if at least one file upload is in progress
     *
     * @return boolean
     */
    public boolean isAtLeastOneUploadInProgress() {
        return getInProgressCount() > 0;
    }

    /**
     * Check if all file uploads are finished
     *
     * @return boolean
     */
    public boolean areAllUploadsFinished() {
        return getInProgressCount() == 0;
    }

    private int getInProgressCount() {
        final int succeededUploadCount = getSucceededUploads().size();
        final int failedUploadCount = getFailedUploads().size();
        final int overallUploadCount = getOverallFilesInUploadProcessMap().size();

        final int inProgressCount = overallUploadCount - failedUploadCount - succeededUploadCount;

        assertFileStateConsistency(inProgressCount, overallUploadCount, succeededUploadCount, failedUploadCount);

        return inProgressCount;
    }

    private static void assertFileStateConsistency(final int inProgressCount, final int overallUploadCount,
            final int succeededUploadCount, final int failedUploadCount) {
        if (inProgressCount < 0) {
            LOG.error("IllegalState: \n{}",
                    getStateListLogMessage(overallUploadCount, succeededUploadCount, failedUploadCount));
        }
    }

    private static String getStateListLogMessage(final int overallUploadCount, final int succeededUploadCount,
            final int failedUploadCount) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("Overall uploads: " + overallUploadCount);
        buffer.append("| succeeded uploads: " + succeededUploadCount);
        buffer.append("| Failed Uploads: " + failedUploadCount);
        return buffer.toString();
    }

    /**
     * Removes all of the data from this Upload process collection
     */
    public void clearFileStates() {
        getOverallFilesInUploadProcessMap().clear();
    }

    /**
     * Clears all temp data collected while uploading files.
     */
    public void clearUploadTempData() {
        LOG.debug("Cleaning up temp data...");
        // delete file system zombies
        for (final FileUploadProgress fileUploadProgress : getAllFileUploadProgressValuesFromOverallUploadProcessList()) {
            if (StringUtils.hasText(fileUploadProgress.getFilePath())) {
                final boolean deleted = FileUtils.deleteQuietly(new File(fileUploadProgress.getFilePath()));
                if (!deleted) {
                    LOG.warn("TempFile was not deleted: {}", fileUploadProgress.getFilePath());
                }
            }
        }
        clearFileStates();
    }

    /**
     * Checks if an upload is in progress for the given Software Module
     * 
     * @param softwareModuleId
     *            id of the software module
     *
     * @return boolean
     */
    public boolean isUploadInProgressForSelectedSoftwareModule(final Long softwareModuleId) {
        for (final FileUploadId fileUploadId : getAllFileUploadIdsFromOverallUploadProcessList()) {
            if (fileUploadId.getSoftwareModuleId().equals(softwareModuleId)) {
                return true;
            }
        }
        return false;
    }

    private Map<FileUploadId, FileUploadProgress> getOverallFilesInUploadProcessMap() {
        if (overallFilesInUploadProcess == null) {
            overallFilesInUploadProcess = Maps.newConcurrentMap();
        }
        return overallFilesInUploadProcess;
    }

    private Set<FileUploadId> getFailedUploads() {
        final Collection<FileUploadProgress> allFileUploadProgressObjects = getOverallFilesInUploadProcessMap()
                .values();
        final Set<FileUploadId> failedFileUploads = Sets.newHashSet();
        for (final FileUploadProgress fileUploadProgress : allFileUploadProgressObjects) {
            if (fileUploadProgress.getFileUploadStatus() == FileUploadStatus.UPLOAD_FAILED) {
                failedFileUploads.add(fileUploadProgress.getFileUploadId());
            }
        }
        return failedFileUploads;
    }

    private Set<FileUploadId> getSucceededUploads() {
        final Collection<FileUploadProgress> allFileUploadProgressObjects = getOverallFilesInUploadProcessMap()
                .values();
        final Set<FileUploadId> succeededFileUploads = Sets.newHashSet();
        for (final FileUploadProgress fileUploadProgress : allFileUploadProgressObjects) {
            if (fileUploadProgress.getFileUploadStatus() == FileUploadStatus.UPLOAD_SUCCESSFUL) {
                succeededFileUploads.add(fileUploadProgress.getFileUploadId());
            }
        }
        return succeededFileUploads;
    }
}
