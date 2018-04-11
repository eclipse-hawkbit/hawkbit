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
import java.io.Serializable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Html5File;

public class UploadLogic implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(UploadLogic.class);

    private final ArtifactUploadState artifactUploadState;

    private final transient Object fileStateListWriteLock = new Object();

    public UploadLogic(final ArtifactUploadState artifactUploadState) {
        this.artifactUploadState = artifactUploadState;
    }

    boolean isDirectory(final Html5File file) {
        return StringUtils.isBlank(file.getType()) && file.getFileSize() % 4096 == 0;
    }

    boolean isFileInUploadState(final FileUploadId fileUploadId) {
        return artifactUploadState.getAllFilesFromOverallUploadProcessList().containsKey(fileUploadId);
    }

    boolean isFileInUploadState(final String filename, final SoftwareModule softwareModule) {
        return artifactUploadState.getAllFilesFromOverallUploadProcessList()
                .containsKey(new FileUploadId(filename, softwareModule));
    }

    boolean isFileInFailedState(final FileUploadId fileUploadId) {
        return artifactUploadState.getFilesInFailedState().contains(fileUploadId);
    }

    boolean isAtLeastOneUploadInProgress() {
        return getInProgressCount() > 0;
    }

    boolean areAllUploadsFinished() {
        return getInProgressCount() == 0;
    }

    private int getInProgressCount() {
        final int succeededUploadCount = artifactUploadState.getFilesInSucceededState().size();
        final int failedUploadCount = artifactUploadState.getFilesInFailedState().size();
        final int overallUploadCount = artifactUploadState.getAllFilesFromOverallUploadProcessList().size();
        final int inProgressCount = overallUploadCount - failedUploadCount - succeededUploadCount;

        assertFileStateConsistency(inProgressCount);

        return inProgressCount;
    }

    private void assertFileStateConsistency(final int inProgressCount) {
        if (inProgressCount < 0) {
            LOG.error("IllegalState: \n{}", getStateListslogMessage());
            throw new IllegalStateException();
        }
    }

    String getStateListslogMessage() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("Overall uploads: " + artifactUploadState.getAllFilesFromOverallUploadProcessList().size());
        buffer.append("\n");
        buffer.append("succeeded uploads: " + artifactUploadState.getFilesInSucceededState().size());
        buffer.append("\n");
        buffer.append("Failed Uploads: " + artifactUploadState.getFilesInFailedState().size());
        return buffer.toString();
    }

    /**
     * Clears all temp data collected while uploading files.
     */
    void clearUploadDetails() {
        // TODO rollouts: change to debug
        LOG.info("Cleaning up temp data...");
        // delete file system zombies
        for (final FileUploadProgress fileUploadProgress : artifactUploadState.getAllFilesFromOverallUploadProcessList()
                .values()) {
            if (StringUtils.isNoneBlank(fileUploadProgress.getFilePath())) {
                FileUtils.deleteQuietly(new File(fileUploadProgress.getFilePath()));
            }
        }
        artifactUploadState.clearBaseSwModuleList();
        clearFileStates();
    }

    void uploadStarted(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        if (isFileInFailedState(fileUploadId)) {
            LOG.warn("Upload already failed, upload started is ignored");
            return;
        }
        synchronized (fileStateListWriteLock) {
            artifactUploadState.addFileToOverallUploadProcessList(fileUploadId, fileUploadProgress);
        }
    }

    void uploadInProgress(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        if (isFileInFailedState(fileUploadId)) {
            LOG.warn("Upload already failed, progress update is ignored");
            return;
        }
        synchronized (fileStateListWriteLock) {
            artifactUploadState.updateFileProgressInOverallUploadProcessList(fileUploadId, fileUploadProgress);
        }
    }

    void uploadFailed(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        synchronized (fileStateListWriteLock) {
            artifactUploadState.updateFileProgressInOverallUploadProcessList(fileUploadId, fileUploadProgress);
            artifactUploadState.addFileToFailedState(fileUploadId);
        }
    }

    void allUploadsAbortedByUser() {
        synchronized (fileStateListWriteLock) {
            artifactUploadState
                    .addAllFilesToFailedState(artifactUploadState.getAllFilesFromOverallUploadProcessList().keySet());
            artifactUploadState.clearFilesInSucceededState();
        }
    }

    void uploadSucceeded(final FileUploadId fileUploadId, final FileUploadProgress fileUploadProgress) {
        if (isFileInFailedState(fileUploadId)) {
            LOG.warn("Upload already failed, progress success is ignored");
            return;
        }
        synchronized (fileStateListWriteLock) {
            artifactUploadState.updateFileProgressInOverallUploadProcessList(fileUploadId, fileUploadProgress);
            artifactUploadState.addFileToSucceededState(fileUploadId);
        }
    }

    private void clearFileStates() {
        synchronized (fileStateListWriteLock) {
            artifactUploadState.clearFilesInFailedState();
            artifactUploadState.clearFilesInSucceededState();
            artifactUploadState.clearOverallUploadProcessList();
        }
    }

    static String createFileUploadId(final String filename, final SoftwareModule softwareModule) {
        return new StringBuilder(filename).append(":").append(
                HawkbitCommonUtil.getFormattedNameVersion(softwareModule.getName(), softwareModule.getVersion()))
                .toString();
    }

    boolean isMoreThanOneSoftwareModulesSelected() {
        return artifactUploadState.getSelectedSoftwareModules().size() > 1;
    }

    boolean isNoSoftwareModuleSelected() {
        return !artifactUploadState.getSelectedBaseSwModuleId().isPresent();
    }
}
