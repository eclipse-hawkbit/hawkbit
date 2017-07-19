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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Artifact File System Repository")
@Stories("Test storing artifact binaries in the file-system")
public class ArtifactFilesystemRepositoryTest {
    private static final String TENANT = "test_tenant";

    private final ArtifactFilesystemProperties artifactResourceProperties = new ArtifactFilesystemProperties();

    private final ArtifactFilesystemRepository artifactFilesystemRepository = new ArtifactFilesystemRepository(
            artifactResourceProperties);

    @Test
    @Description("Verfies that an artifact can be successfully stored in the file-system repository")
    public void storeSuccessfully() throws IOException {
        final byte[] fileContent = randomBytes();
        final ArtifactFilesystem artifact = storeRandomArtifact(fileContent);

        final byte[] readContent = new byte[fileContent.length];
        IOUtils.read(artifact.getFileInputStream(), readContent);

        assertThat(readContent).isEqualTo(fileContent);
    }

    @Test
    @Description("Verfies that an artifact can be successfully stored in the file-system repository")
    public void getStoredArtifactBasedOnSHA1Hash() {

        final byte[] fileContent = randomBytes();
        final ArtifactFilesystem artifact = storeRandomArtifact(fileContent);

        final AbstractDbArtifact artifactBySha1 = artifactFilesystemRepository.getArtifactBySha1(TENANT,
                artifact.getHashes().getSha1());
        assertThat(artifactBySha1).isNotNull();
    }

    @Test
    @Description("Verfies that an artifact can be deleted in the file-system repository")
    public void deleteStoredArtifactBySHA1Hash() {
        final ArtifactFilesystem artifact = storeRandomArtifact(randomBytes());

        artifactFilesystemRepository.deleteBySha1(TENANT, artifact.getHashes().getSha1());

        assertThat(artifactFilesystemRepository.getArtifactBySha1(TENANT, artifact.getHashes().getSha1())).isNull();
    }

    @Test
    @Description("Verfies that all artifacts of a tenant can be deleted in the file-system repository")
    public void deleteStoredArtifactOfTenant() {
        final ArtifactFilesystem artifact = storeRandomArtifact(randomBytes());

        artifactFilesystemRepository.deleteByTenant(TENANT);

        assertThat(artifactFilesystemRepository.getArtifactBySha1(TENANT, artifact.getHashes().getSha1())).isNull();
    }

    @Test
    @Description("Verfies that an artifact which does not exists is deleted quietly in the file-system repository")
    public void deleteArtifactWhichDoesNotExistsBySHA1HashWithoutException() {
        try {
            artifactFilesystemRepository.deleteBySha1(TENANT, "sha1HashWhichDoesNotExists");
        } catch (final Exception e) {
            Assertions.fail("did not expect an exception while deleting a file which does not exists");
        }

        final ArtifactFilesystem artifact = storeRandomArtifact(randomBytes());
        try {
            artifactFilesystemRepository.deleteBySha1("tenantWhichDoesNotExist", artifact.getHashes().getSha1());
        } catch (final Exception e) {
            Assertions.fail("did not expect an exception while deleting a file which does not exists");
        }
    }

    private ArtifactFilesystem storeRandomArtifact(final byte[] fileContent) {
        final String fileName = "filename.tmp";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
        final ArtifactFilesystem store = artifactFilesystemRepository.store(TENANT, inputStream, fileName,
                "application/txt");
        return store;
    }

    private static byte[] randomBytes() {
        final byte[] randomBytes = new byte[20];
        final Random ran = new Random();
        ran.nextBytes(randomBytes);
        return randomBytes;
    }

}
