/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.vaadin.ui.Html5File;

public class UploadLogic {

    private static final Logger LOG = LoggerFactory.getLogger(UploadLogic.class);

    private final List<String> duplicateFileNamesList = new ArrayList<>();

    private final ArtifactUploadState artifactUploadState;

    private final Object fileStateListWriteLock = new Object();

    public UploadLogic(final ArtifactUploadState artifactUploadState) {
        this.artifactUploadState = artifactUploadState;
    }

    boolean isDirectory(final Html5File file) {
        return StringUtils.isEmpty(file.getType()) && file.getFileSize() % 4096 == 0;
    }

    List<String> getDuplicateFileNamesList() {
        return duplicateFileNamesList;
    }

    void clearDuplicateFileNamesList() {
        duplicateFileNamesList.clear();
    }

    boolean containsDuplicateFiles() {
        return !duplicateFileNamesList.isEmpty();
    }

    boolean containsFileName(final String fileName) {
        if (duplicateFileNamesList.isEmpty()) {
            return false;
        }
        return duplicateFileNamesList.contains(fileName);
    }

    boolean isFileInUploadState(final FileUploadId fileUploadId) {
        return artifactUploadState.getAllFilesFromOverallUploadProcessList().containsKey(fileUploadId);
    }

    boolean isFileInUploadState(final String filename, final SoftwareModule softwareModule) {
        return artifactUploadState.getAllFilesFromOverallUploadProcessList()
                .containsKey(new FileUploadId(filename, softwareModule));
    }

    boolean isUploadComplete() {
        final int inProgressCount = artifactUploadState.getFilesInUploadProgressState().size();
        final int succeededUploadCount = artifactUploadState.getFilesInSucceededState().size();
        final int failedUploadCount = artifactUploadState.getFilesInFailedState().size();
        final int overallUploadCount = artifactUploadState.getAllFilesFromOverallUploadProcessList().size();

        // check consistency
        if (inProgressCount + succeededUploadCount + failedUploadCount != overallUploadCount) {
            LOG.error("IllegalState: \n{}", getStateListslogMessage());
            throw new IllegalStateException();
        }

        return inProgressCount == 0;
    }

    String getStateListslogMessage() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("Overall uploads: " + artifactUploadState.getAllFilesFromOverallUploadProcessList().size());
        buffer.append("\n");
        buffer.append("succeeded uploads: " + artifactUploadState.getFilesInSucceededState().size());
        buffer.append("\n");
        buffer.append("Failed Uploads: " + artifactUploadState.getFilesInFailedState().size());
        buffer.append("\n");
        buffer.append("Uploads in progress: " + artifactUploadState.getFilesInUploadProgressState().size());
        return buffer.toString();
    }

    boolean isUploadRunning() {
        final int inProgressCount = artifactUploadState.getFilesInUploadProgressState().size();
        final int succeededUploadCount = artifactUploadState.getFilesInSucceededState().size();
        final int failedUploadCount = artifactUploadState.getFilesInFailedState().size();
        final int overallUploadCount = artifactUploadState.getAllFilesFromOverallUploadProcessList().size();

        // check consistency
        if (inProgressCount + succeededUploadCount + failedUploadCount != overallUploadCount) {
            LOG.error("IllegalState: \n{}", getStateListslogMessage());
            throw new IllegalStateException();
        }

        return inProgressCount > 0;
    }

    /**
     * Clears all temp data collected while uploading files.
     */
    void clearUploadDetails() {
        // delete file system zombies
        for (final FileUploadProgress fileUploadProgress : artifactUploadState.getAllFilesFromOverallUploadProcessList()
                .values()) {
            FileUtils.deleteQuietly(new File(fileUploadProgress.getFilePath()));
        }
        artifactUploadState.clearBaseSwModuleList();
        clearDuplicateFileNamesList();
        clearFileStates();
    }

    void uploadStarted(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        synchronized (fileStateListWriteLock) {
            artifactUploadState.addFileToOverallUploadProcessList(fileUploadId, fileUploadProgress);
            artifactUploadState.addFileToUploadInProgressState(fileUploadId);
        }
    }

    void uploadInProgress(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        synchronized (fileStateListWriteLock) {
            artifactUploadState.updateFileProgressInOverallUploadProcessList(fileUploadId, fileUploadProgress);
        }
    }

    void uploadFailed(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        synchronized (fileStateListWriteLock) {
            artifactUploadState.updateFileProgressInOverallUploadProcessList(fileUploadId, fileUploadProgress);
            artifactUploadState.removeFileInUploadProgressState(fileUploadId);
            artifactUploadState.addFileToFailedState(fileUploadId);
        }
    }

    void uploadSucceeded(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        synchronized (fileStateListWriteLock) {
            artifactUploadState.updateFileProgressInOverallUploadProcessList(fileUploadId, fileUploadProgress);
            artifactUploadState.removeFileInUploadProgressState(fileUploadId);
            artifactUploadState.addFileToSucceededState(fileUploadId);
        }
    }

    void clearFileStates() {
        synchronized (fileStateListWriteLock) {
            artifactUploadState.clearFilesInFailedState();
            artifactUploadState.clearFilesInSucceededState();
            artifactUploadState.clearFilesInUploadProgressState();
            artifactUploadState.clearOverallUploadProcessList();
        }
    }

    static String createFileUploadId(final String filename, final SoftwareModule softwareModule) {
        return new StringBuilder(filename).append(":").append(
                HawkbitCommonUtil.getFormattedNameVersion(softwareModule.getName(), softwareModule.getVersion()))
                .toString();
    }
}
