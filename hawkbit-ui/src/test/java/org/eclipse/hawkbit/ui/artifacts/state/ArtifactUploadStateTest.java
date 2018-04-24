package org.eclipse.hawkbit.ui.artifacts.state;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadId;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress;
import org.hamcrest.Matchers;
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

        assertTrue(stateUnderTest.isAtLeastOneUploadInProgress());
    }

    @Test
    public void isAtLeastOneUploadInProgressReturnsFalseForFailedAndSuccededUploads() {
        simulateUploadFailedFor(fileUploadId1, fileUploadId2);
        simulateUploadSucceededFor(fileUploadId3);

        assertFalse(stateUnderTest.isAtLeastOneUploadInProgress());
    }

    @Test
    public void areAllUploadsFinishedReturnsTrueForFailedAndSuccededUpload() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadSucceededFor(fileUploadId1);
        simulateUploadFailedFor(fileUploadId2, fileUploadId3);

        assertTrue(stateUnderTest.areAllUploadsFinished());
    }

    @Test
    public void areAllUploadsFinishedReturnsFalseForUploadInProgress() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);

        assertFalse(stateUnderTest.areAllUploadsFinished());
    }

    @Test
    public void areAllUploadsFinishedReturnsTrueAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertTrue(stateUnderTest.areAllUploadsFinished());
    }

    @Test
    public void isAtLeastOneUploadInProgressReturnsFalseAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertFalse(stateUnderTest.isAtLeastOneUploadInProgress());
    }

    @Test
    public void getAllFileUploadIdsFromOverallUploadProcessListReturnsNothingAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertThat(stateUnderTest.getAllFileUploadIdsFromOverallUploadProcessList(),
                Matchers.emptyCollectionOf(FileUploadId.class));
    }

    @Test
    public void getAllFileUploadProgressValuesFromOverallUploadProcessListReturnsNothingAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertThat(stateUnderTest.getAllFileUploadProgressValuesFromOverallUploadProcessList(),
                Matchers.emptyCollectionOf(FileUploadProgress.class));
    }

    @Test
    public void getFilesInFailedStateReturnsNothingAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadFailedFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertThat(stateUnderTest.getFilesInFailedState(), Matchers.emptyCollectionOf(FileUploadId.class));
    }

    @Test
    public void isFileInUploadStateReturnsFalseAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1);

        stateUnderTest.clearFileStates();

        assertFalse(stateUnderTest.isFileInUploadState(fileUploadId1));
    }

    @Test
    public void getFilesInSucceededStateReturnsNothingAfterFileStateReset() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadSucceededFor(fileUploadId1, fileUploadId2, fileUploadId3);

        stateUnderTest.clearFileStates();

        assertThat(stateUnderTest.getFilesInSucceededState(), Matchers.emptyCollectionOf(FileUploadId.class));
    }

    @Test
    public void getAllFileUploadIdsFromOverallUploadProcessListReturnsAllFileIds() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadFailedFor(fileUploadId1);
        simulateUploadSucceededFor(fileUploadId2);

        assertThat(stateUnderTest.getAllFileUploadIdsFromOverallUploadProcessList(),
                Matchers.containsInAnyOrder(fileUploadId1, fileUploadId2, fileUploadId3));
    }

    @Test
    public void getFilesInSucceededStateReturnsOnlySucceedeFileIds() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadFailedFor(fileUploadId2);
        simulateUploadSucceededFor(fileUploadId3);

        assertThat(stateUnderTest.getFilesInSucceededState(), Matchers.contains(fileUploadId3));
    }

    @Test
    public void getFilesInFailedStateReturnsOnlyFailedFileIds() {
        simulateUploadInProgressFor(fileUploadId1, fileUploadId2, fileUploadId3);
        simulateUploadFailedFor(fileUploadId2);
        simulateUploadSucceededFor(fileUploadId3);

        assertThat(stateUnderTest.getFilesInFailedState(), Matchers.contains(fileUploadId2));
    }

    @Test
    public void isFileInUploadStateReturnsTrueForFileInProgress() {
        simulateUploadInProgressFor(fileUploadId1);

        assertTrue(stateUnderTest.isFileInUploadState(fileUploadId1));
    }

    @Test
    public void isFileInUploadStateReturnsTrueForFileUploadFailed() {
        simulateUploadInProgressFor(fileUploadId1);
        simulateUploadFailedFor(fileUploadId1);

        assertTrue(stateUnderTest.isFileInUploadState(fileUploadId1));
    }

    @Test
    public void isFileInUploadStateReturnsTrueForFileUploadSucceeded() {
        simulateUploadInProgressFor(fileUploadId1);
        simulateUploadSucceededFor(fileUploadId1);

        assertTrue(stateUnderTest.isFileInUploadState(fileUploadId1));
    }

    @Test
    public void isFileInUploadStateReturnsFalseForUnknownFileId() {

        assertFalse(stateUnderTest.isFileInUploadState(fileUploadId1));
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
