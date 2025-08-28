/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.filesystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.hawkbit.repository.artifact.AbstractArtifactRepository;
import org.eclipse.hawkbit.repository.artifact.exception.ArtifactBinaryNotFoundException;
import org.eclipse.hawkbit.repository.artifact.model.DbArtifactHash;
import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - Artifact File System Repository<br/>
 * Story: Test storing artifact binaries in the file-system
 */
class ArtifactFilesystemTest {

    /**
     * Verifies that an exception is thrown on opening an InputStream when file does not exists
     */
    @Test
    void getInputStreamOfNonExistingFileThrowsException() {
        final File file = new File("fileWhichTotalDoesNotExists");
        final ArtifactFilesystem underTest = new ArtifactFilesystem(
                file, "fileWhichTotalDoesNotExists",
                new DbArtifactHash("1", "2", "3"), 0L, null);
        assertThatThrownBy(underTest::getFileInputStream).isInstanceOf(ArtifactBinaryNotFoundException.class);
    }

    /**
     * Verifies that an InputStream can be opened if file exists
     */
    @Test
    void getInputStreamOfExistingFile() throws IOException {
        final ArtifactFilesystem underTest = new ArtifactFilesystem(
                AbstractArtifactRepository.createTempFile(false), ArtifactFilesystemTest.class.getSimpleName(),
                new DbArtifactHash("1", "2", "3"), 0L, null);
        assertThat(IOUtils.read(underTest.getFileInputStream(), new byte[16])).isZero();
    }
}