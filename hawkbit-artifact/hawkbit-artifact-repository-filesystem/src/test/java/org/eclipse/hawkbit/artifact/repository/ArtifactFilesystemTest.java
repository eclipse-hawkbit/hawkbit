/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - Artifact File System Repository<br/>
 * Story: Test storing artifact binaries in the file-system
 */
class ArtifactFilesystemTest {

    /**
     * Verifies that an exception is thrown on opening an InputStream when file does not exists
     */
    @Test    void getInputStreamOfNonExistingFileThrowsException() {
        final File file = new File("fileWhichTotalDoesNotExists");
        final ArtifactFilesystem underTest = new ArtifactFilesystem(
                file, "fileWhichTotalDoesNotExists",
                new DbArtifactHash("1", "2", "3"), 0L, null);
        assertThatThrownBy(underTest::getFileInputStream)
                .isInstanceOf(ArtifactFileNotFoundException.class)
                .hasCauseInstanceOf(FileNotFoundException.class);
    }

    /**
     * Verifies that an InputStream can be opened if file exists
     */
    @Test    void getInputStreamOfExistingFile() throws IOException {
        final ArtifactFilesystem underTest = new ArtifactFilesystem(
                AbstractArtifactRepository.createTempFile(false), ArtifactFilesystemTest.class.getSimpleName(),
                new DbArtifactHash("1", "2", "3"), 0L, null);
        assertThat(IOUtils.read(underTest.getFileInputStream(), new byte[16])).isZero();
    }
}