/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.state;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.ui.artifacts.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadId;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress.FileUploadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.mockito.junit.jupiter.MockitoExtension;

@Feature("Unit Tests - Management UI")
@Story("Upload UI state")
@ExtendWith(MockitoExtension.class)
public class ArtifactUploadStateTest {

    @Mock
    public FileUploadId fileUploadId1;

    @Mock
    public FileUploadId fileUploadId2;

    @Mock
    public FileUploadId fileUploadId3;

    public FileUploadProgress fileUploadProgressStateFailed;
    public FileUploadProgress fileUploadProgressStateSucceeded;
    public FileUploadProgress fileUploadProgressStateInProgress;

    private ArtifactUploadState stateUnderTest;

    @BeforeEach
    public void setupTest() {
        stateUnderTest = new ArtifactUploadState();
    }

    @Test
    public void isAtLeastOneUploadInProgressReturnsTrueForUploadInProgress() {
        simulateUploadInProgressFor(fileUploadId1);
        simulateUploadFailedFor(fileUploadId2);
        simulateUploadSucceededFor(fileUploadId3);

        assertThat(stateUnderTest.isAtLeastOneUploadInProgress()).isTrue();
    }

    @Test
    public void isAtLeastOneUploadInProgressReturnsFalseForFailedAndSuccededUploads() {
        simulateUploadFailedFor(fileUploadId1, fileUploadId2);
        simulateUploadSucceededFor(fileUploadId3);

        assertThat(stateUnderTest.isAtLeastOneUploadInProgress()).isFalse();
    }

    @Test
    public void areAllUploadsFinishedReturnsTrueForFailedAndSuccededUpload() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadSucceededFor(fileUploadId1);
        simulateUploadFailedFor(fileUploadId2, fileUploadId3);

        assertThat(stateUnderTest.areAllUploadsFinished()).isTrue();
    }

    @Test
    public void areAllUploadsFinishedReturnsFalseForUploadInProgress() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);

        assertThat(stateUnderTest.areAllUploadsFinished()).isFalse();
    }

    @Test
    public void areAllUploadsFinishedReturnsTrueAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertThat(stateUnderTest.areAllUploadsFinished()).isTrue();
    }

    @Test
    public void isAtLeastOneUploadInProgressReturnsFalseAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertThat(stateUnderTest.isAtLeastOneUploadInProgress()).isFalse();
    }

    @Test
    public void getAllFileUploadIdsFromOverallUploadProcessListReturnsNothingAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertThat(stateUnderTest.getAllFileUploadIdsFromOverallUploadProcessList()).isEmpty();
    }

    @Test
    public void getAllFileUploadProgressValuesFromOverallUploadProcessListReturnsNothingAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertThat(stateUnderTest.getAllFileUploadProgressValuesFromOverallUploadProcessList()).isEmpty();
    }

    @Test
    public void getFilesInFailedStateReturnsNothingAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadFailedFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertThat(stateUnderTest.getFilesInFailedState()).isEmpty();
    }

    @Test
    public void isFileInUploadStateReturnsFalseAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1);

        stateUnderTest.clearFileStates();

        assertThat(stateUnderTest.isFileInUploadState(fileUploadId1)).isFalse();
    }

    @Test
    public void getAllFileUploadIdsFromOverallUploadProcessListReturnsAllFileIds() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadFailedFor(fileUploadId1);
        simulateUploadSucceededFor(fileUploadId2);

        assertThat(stateUnderTest.getAllFileUploadIdsFromOverallUploadProcessList())
                .containsExactlyInAnyOrder(fileUploadId1, fileUploadId2, fileUploadId3);
    }

    @Test
    public void getFilesInFailedStateReturnsOnlyFailedFileIds() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadFailedFor(fileUploadId2);
        simulateUploadSucceededFor(fileUploadId3);

        assertThat(stateUnderTest.getFilesInFailedState()).containsOnly(fileUploadId2);
    }

    @Test
    public void isFileInUploadStateReturnsTrueForFileInProgress() {
        simulateUploadInProgressFor(fileUploadId1);

        assertThat(stateUnderTest.isFileInUploadState(fileUploadId1)).isTrue();
    }

    @Test
    public void isFileInUploadStateReturnsTrueForFileUploadFailed() {
        simulateUploadInProgressFor(fileUploadId1);
        simulateUploadFailedFor(fileUploadId1);

        assertThat(stateUnderTest.isFileInUploadState(fileUploadId1)).isTrue();
    }

    @Test
    public void isFileInUploadStateReturnsTrueForFileUploadSucceeded() {
        simulateUploadInProgressFor(fileUploadId1);
        simulateUploadSucceededFor(fileUploadId1);

        assertThat(stateUnderTest.isFileInUploadState(fileUploadId1)).isTrue();
    }

    @Test
    public void isFileInUploadStateReturnsFalseForUnknownFileId() {

        assertThat(stateUnderTest.isFileInUploadState(fileUploadId1)).isFalse();
    }

    private void simulateUploadInProgressFor(final FileUploadId... fileUploadIds) {
        for (final FileUploadId fileUploadId : fileUploadIds) {
            final FileUploadProgress fileUploadProgress = new FileUploadProgress(fileUploadId,
                    FileUploadStatus.UPLOAD_IN_PROGRESS);
            stateUnderTest.updateFileUploadProgress(fileUploadId, fileUploadProgress);
        }
    }

    private void simulateUploadSucceededFor(final FileUploadId... fileUploadIds) {
        for (final FileUploadId fileUploadId : fileUploadIds) {
            final FileUploadProgress fileUploadProgress = new FileUploadProgress(fileUploadId,
                    FileUploadStatus.UPLOAD_SUCCESSFUL);
            stateUnderTest.updateFileUploadProgress(fileUploadId, fileUploadProgress);
        }
    }

    private void simulateUploadFailedFor(final FileUploadId... fileUploadIds) {
        for (final FileUploadId fileUploadId : fileUploadIds) {
            final FileUploadProgress fileUploadProgress = new FileUploadProgress(fileUploadId,
                    FileUploadStatus.UPLOAD_FAILED);
            stateUnderTest.updateFileUploadProgress(fileUploadId, fileUploadProgress);
        }
    }

}
