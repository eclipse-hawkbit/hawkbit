/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Artifact File System Repository")
@Stories("Test storing artifact binaries in the file-system")
public class ArtifactFilesystemTest {

    @Test
    @Description("Verifies that an exception is thrown on opening an InputStream when file does not exists")
    public void getInputStreamOfNonExistingFileThrowsException() {
        final File file = new File("fileWhichTotalDoesNotExists");
        final ArtifactFilesystem underTest = new ArtifactFilesystem(file, "fileWhichTotalDoesNotExists",
                new DbArtifactHash("1", "2"), 0L, null);
        try {
            underTest.getFileInputStream();
            Assertions.fail("Expected a FileNotFoundException because file does not exists");
        } catch (final RuntimeException e) {
            assertThat(e.getCause()).isInstanceOf(FileNotFoundException.class);
        }
    }

    @Test
    @Description("Verifies that an InputStream can be opened if file exists")
    public void getInputStreamOfExistingFile() throws IOException {
        final File createTempFile = File.createTempFile(ArtifactFilesystemTest.class.getSimpleName(), "");
        createTempFile.deleteOnExit();

        final ArtifactFilesystem underTest = new ArtifactFilesystem(createTempFile,
                ArtifactFilesystemTest.class.getSimpleName(), new DbArtifactHash("1", "2"), 0L, null);
        final byte[] buffer = new byte[1024];
        IOUtils.read(underTest.getFileInputStream(), buffer);
    }
}
