package org.eclipse.hawkbit.ui.artifacts.state;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadId;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Management UI")
@Stories("Upload UI state")
@RunWith(MockitoJUnitRunner.class)
public class ArtifactUploadStateTest {

    @Mock
    public SoftwareModuleFilters softwareModuleFilters;

    @Mock
    public FileUploadId fileUploadId1;

    @Mock
    public FileUploadId fileUploadId2;

    @Mock
    public FileUploadId fileUploadId3;

    @Mock
    public FileUploadProgress fileUploadProgress;

    private ArtifactUploadState stateUnderTest;

    @Before
    public void setupTest() {
        stateUnderTest = new ArtifactUploadState(softwareModuleFilters);
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
    public void getFilesInSucceededStateReturnsNothingAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadSucceededFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertThat(stateUnderTest.getFilesInSucceededState()).isEmpty();
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
    public void getFilesInSucceededStateReturnsOnlySucceedeFileIds() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadFailedFor(fileUploadId2);
        simulateUploadSucceededFor(fileUploadId3);

        assertThat(stateUnderTest.getFilesInSucceededState()).containsOnly(fileUploadId3);
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
            stateUnderTest.uploadInProgress(fileUploadId, fileUploadProgress);
        }
    }

    private void simulateUploadSucceededFor(final FileUploadId... fileUploadIds) {
        for (final FileUploadId fileUploadId : fileUploadIds) {
            stateUnderTest.uploadSucceeded(fileUploadId, fileUploadProgress);
        }
    }

    private void simulateUploadFailedFor(final FileUploadId... fileUploadIds) {
        for (final FileUploadId fileUploadId : fileUploadIds) {
            stateUnderTest.uploadFailed(fileUploadId, fileUploadProgress);
        }
    }

}
