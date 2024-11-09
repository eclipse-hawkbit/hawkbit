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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.junit.jupiter.api.Test;

@Feature("Unit Tests - Artifact File System Repository")
@Story("Test storing artifact binaries in the file-system")
class ArtifactFilesystemTest {

    @Test
    @Description("Verifies that an exception is thrown on opening an InputStream when file does not exists")
    void getInputStreamOfNonExistingFileThrowsException() {
        final File file = new File("fileWhichTotalDoesNotExists");
        final ArtifactFilesystem underTest = new ArtifactFilesystem(
                file, "fileWhichTotalDoesNotExists",
                new DbArtifactHash("1", "2", "3"), 0L, null);
        try {
            underTest.getFileInputStream();
            Assertions.fail("Expected a FileNotFoundException because file does not exists");
        } catch (final RuntimeException e) {
            assertThat(e.getCause()).isInstanceOf(FileNotFoundException.class);
        }
    }

    @Test
    @Description("Verifies that an InputStream can be opened if file exists")
    void getInputStreamOfExistingFile() throws IOException {
        final File createTempFile = Files.createTempFile(ArtifactFilesystemTest.class.getSimpleName(), "").toFile();
        createTempFile.deleteOnExit();

        final ArtifactFilesystem underTest = new ArtifactFilesystem(
                createTempFile, ArtifactFilesystemTest.class.getSimpleName(), new DbArtifactHash("1", "2", "3"), 0L, null);
        final byte[] buffer = new byte[1024];
        assertThat(IOUtils.read(underTest.getFileInputStream(), buffer)).isEqualTo(0);
    }
}