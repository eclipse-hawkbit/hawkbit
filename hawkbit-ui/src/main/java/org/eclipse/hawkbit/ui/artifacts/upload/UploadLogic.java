/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.util.ArrayList;
import java.util.List;

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

    // boolean checkForDuplicate(final String filename, final SoftwareModule
    // selectedSw,
    // final Set<CustomFile> selectedFiles) {
    // final Boolean isDuplicate = checkIfFileIsDuplicate(filename, selectedSw,
    // selectedFiles);
    // if (isDuplicate) {
    // getDuplicateFileNamesList().add(filename);
    // }
    // return isDuplicate;
    // }

    boolean containsFileName(final String fileName) {
        if (duplicateFileNamesList.isEmpty()) {
            return false;
        }
        return duplicateFileNamesList.contains(fileName);
    }

    boolean isFileInUploadState(final FileUploadId fileUploadId) {
        return artifactUploadState.getFilesInUploadState().containsKey(fileUploadId);
    }

    boolean isFileInUploadState(final String filename, final SoftwareModule softwareModule) {
        return artifactUploadState.getFilesInUploadState().containsKey(createFileUploadId(filename, softwareModule));
    }

    boolean isUploadComplete() {
        final int uploadedCount = artifactUploadState.getNumberOfFilesActuallyUploading().intValue();
        final int expectedUploadsCount = artifactUploadState.getNumberOfFileUploadsExpected().intValue();
        return uploadedCount == expectedUploadsCount;
    }

    boolean isUploadRunning() {
        return artifactUploadState.getNumberOfFileUploadsExpected().intValue() > 0
                || artifactUploadState.getNumberOfFilesActuallyUploading().intValue() > 0;
    }

    /**
     * Clears all temp data collected while uploading files.
     */
    void clearUploadDetailsIfAllUploadsFinished() {
        if (isUploadComplete()) {
            // delete file system zombies
            // artifactUploadState.getFileSelected()
            // .forEach(customFile -> FileUtils.deleteQuietly(new
            // File(customFile.getFilePath())));
            // artifactUploadState.clearFileSelected();
            artifactUploadState.clearBaseSwModuleList();
            artifactUploadState.clearNumberOfFilesActuallyUploading();
            artifactUploadState.clearNumberOfFileUploadsExpected();
            artifactUploadState.clearNumberOfFileUploadsFailed();
            // artifactUploadState.clearFileUploadStatus();
            clearDuplicateFileNamesList();
            artifactUploadState.clearFilesInUploadState();
        }
    }

    void resetUploadCounters() {
        artifactUploadState.clearNumberOfFilesActuallyUploading();
        artifactUploadState.clearNumberOfFileUploadsExpected();
        artifactUploadState.clearNumberOfFileUploadsFailed();
    }

    void updateUploadProcessCountersDueToUploadAdded() {
        artifactUploadState.incrementNumberOfFilesActuallyUploading();
        artifactUploadState.incrementNumberOfFileUploadsExpected();
    }

    void updateUploadProcessCountersDueToUploadFailed() {
        artifactUploadState.incrementNumberOfFileUploadsFailed();
        artifactUploadState.decrementNumberOfFilesActuallyUploading();
        artifactUploadState.decrementNumberOfFileUploadsExpected();
    }

    void updateUploadProcessCountersDueToUploadSucceeded() {
        artifactUploadState.decrementNumberOfFilesActuallyUploading();
        artifactUploadState.decrementNumberOfFileUploadsExpected();
    }

    static String createFileUploadId(final String filename, final SoftwareModule softwareModule) {
        return new StringBuilder(filename).append(":").append(
                HawkbitCommonUtil.getFormattedNameVersion(softwareModule.getName(), softwareModule.getVersion()))
                .toString();
    }
}
