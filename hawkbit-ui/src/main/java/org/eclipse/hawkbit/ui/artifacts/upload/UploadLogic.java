package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.state.CustomFile;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.vaadin.ui.Html5File;

public class UploadLogic {

    private static final Logger LOG = LoggerFactory.getLogger(UploadLogic.class);

    private final List<String> duplicateFileNamesList = new ArrayList<>();

    private boolean hasDirectory;

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

    boolean checkForDuplicate(final String filename, final SoftwareModule selectedSw,
            final Set<CustomFile> selectedFiles) {
        final Boolean isDuplicate = checkIfFileIsDuplicate(filename, selectedSw, selectedFiles);
        if (isDuplicate) {
            getDuplicateFileNamesList().add(filename);
        }
        return isDuplicate;
    }

    boolean containsFileName(final String fileName) {
        if (duplicateFileNamesList.isEmpty()) {
            return false;
        }
        return duplicateFileNamesList.contains(fileName);
    }

    /**
     * Check if the selected file is duplicate. i.e. already selected for upload
     * for same software module.
     *
     * @param name
     *            file name
     * @param selectedSoftwareModule
     *            the current selected software module
     * @return Boolean
     */
    Boolean checkIfFileIsDuplicate(final String name, final SoftwareModule selectedSoftwareModule,
            final Set<CustomFile> selectedFiles) {
        Boolean isDuplicate = false;
        final String currentBaseSoftwareModuleKey = HawkbitCommonUtil
                .getFormattedNameVersion(selectedSoftwareModule.getName(), selectedSoftwareModule.getVersion());

        for (final CustomFile customFile : selectedFiles) {
            final String fileSoftwareModuleKey = HawkbitCommonUtil.getFormattedNameVersion(
                    customFile.getBaseSoftwareModuleName(), customFile.getBaseSoftwareModuleVersion());
            if (customFile.getFileName().equals(name) && currentBaseSoftwareModuleKey.equals(fileSoftwareModuleKey)) {
                isDuplicate = true;
                break;
            }
        }
        return isDuplicate;
    }

    void updateFileSize(final String name, final long size, final SoftwareModule selectedSoftwareModule,
            final Set<CustomFile> selectedFiles) {
        final String currentBaseSoftwareModuleKey = HawkbitCommonUtil
                .getFormattedNameVersion(selectedSoftwareModule.getName(), selectedSoftwareModule.getVersion());

        for (final CustomFile customFile : selectedFiles) {
            final String fileSoftwareModuleKey = HawkbitCommonUtil.getFormattedNameVersion(
                    customFile.getBaseSoftwareModuleName(), customFile.getBaseSoftwareModuleVersion());
            if (customFile.getFileName().equals(name) && currentBaseSoftwareModuleKey.equals(fileSoftwareModuleKey)) {
                customFile.setFileSize(size);
                break;
            }
        }
    }

    boolean hasDirectory() {
        return hasDirectory;
    }

    void setHasDirectory(final boolean hasDirectory) {
        this.hasDirectory = hasDirectory;
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

    boolean isArtifactDetailsDisplayed(final Long softwareModuleId) {
        return artifactUploadState.getSelectedBaseSwModuleId().map(moduleId -> moduleId.equals(softwareModuleId))
                .orElse(false);
    }

    /**
     * Clears all temp data collected while uploading files.
     */
    void clearUploadDetailsIfAllUploadsFinished() {
        if (isUploadComplete()) {
            // delete file system zombies
            artifactUploadState.getFileSelected()
                    .forEach(customFile -> FileUtils.deleteQuietly(new File(customFile.getFilePath())));
            artifactUploadState.clearFileSelected();
            artifactUploadState.clearBaseSwModuleList();
            artifactUploadState.clearNumberOfFilesActuallyUploading();
            artifactUploadState.clearNumberOfFileUploadsExpected();
            artifactUploadState.clearNumberOfFileUploadsFailed();
            artifactUploadState.clearFileUploadStatus();
            clearDuplicateFileNamesList();
        }
    }

    void resetUploadState() {
        if (artifactUploadState.getNumberOfFilesActuallyUploading().intValue() >= artifactUploadState
                .getNumberOfFileUploadsExpected().intValue() && !artifactUploadState.getFileSelected().isEmpty()) {
            artifactUploadState.clearNumberOfFilesActuallyUploading();
            artifactUploadState.clearNumberOfFileUploadsExpected();
        }
    }
}
