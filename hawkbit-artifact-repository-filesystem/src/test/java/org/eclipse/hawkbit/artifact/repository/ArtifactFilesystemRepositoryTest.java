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
import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Unit Tests - Artifact File System Repository")
@Story("Test storing artifact binaries in the file-system")
public class ArtifactFilesystemRepositoryTest {
    private static final Logger LOG = LoggerFactory.getLogger(ArtifactFilesystemRepositoryTest.class);

    private static final String TENANT = "test_tenant";

    private static ArtifactFilesystemProperties artifactResourceProperties;

    private static ArtifactFilesystemRepository artifactFilesystemRepository;

    @BeforeAll
    public static void setup() {
        artifactResourceProperties = new ArtifactFilesystemProperties();
        artifactResourceProperties.setPath(Files.createTempDir().getAbsolutePath());

        artifactFilesystemRepository = new ArtifactFilesystemRepository(artifactResourceProperties);
    }

    @AfterAll
    public static void afterClass() {
        if (new File(artifactResourceProperties.getPath()).exists()) {
            try {
                FileUtils.deleteDirectory(new File(artifactResourceProperties.getPath()));
            } catch (final IOException | IllegalArgumentException e) {
                LOG.warn("Cannot delete file-directory", e);
            }
        }
    }

    @Test
    @Description("Verfies that an artifact can be successfully stored in the file-system repository")
    public void storeSuccessfully() throws IOException {
        final byte[] fileContent = randomBytes();
        final AbstractDbArtifact artifact = storeRandomArtifact(fileContent);

        final byte[] readContent = new byte[fileContent.length];
        IOUtils.read(artifact.getFileInputStream(), readContent);

        assertThat(readContent).isEqualTo(fileContent);
    }

    @Test
    @Description("Verfies that an artifact can be successfully stored in the file-system repository")
    public void getStoredArtifactBasedOnSHA1Hash() {

        final byte[] fileContent = randomBytes();
        final AbstractDbArtifact artifact = storeRandomArtifact(fileContent);

        final AbstractDbArtifact artifactBySha1 = artifactFilesystemRepository.getArtifactBySha1(TENANT,
                artifact.getHashes().getSha1());
        assertThat(artifactBySha1).isNotNull();
    }

    @Test
    @Description("Verfies that an artifact can be deleted in the file-system repository")
    public void deleteStoredArtifactBySHA1Hash() {
        final AbstractDbArtifact artifact = storeRandomArtifact(randomBytes());

        artifactFilesystemRepository.deleteBySha1(TENANT, artifact.getHashes().getSha1());

        assertThat(artifactFilesystemRepository.getArtifactBySha1(TENANT, artifact.getHashes().getSha1())).isNull();
    }

    @Test
    @Description("Verfies that all artifacts of a tenant can be deleted in the file-system repository")
    public void deleteStoredArtifactOfTenant() {
        final AbstractDbArtifact artifact = storeRandomArtifact(randomBytes());

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

        final AbstractDbArtifact artifact = storeRandomArtifact(randomBytes());
        try {
            artifactFilesystemRepository.deleteBySha1("tenantWhichDoesNotExist", artifact.getHashes().getSha1());
        } catch (final Exception e) {
            Assertions.fail("did not expect an exception while deleting a file which does not exists");
        }
    }

    private AbstractDbArtifact storeRandomArtifact(final byte[] fileContent) {
        final String fileName = "filename.tmp";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
        return artifactFilesystemRepository.store(TENANT, inputStream, fileName, "application/txt", null);
    }

    private static byte[] randomBytes() {
        final byte[] randomBytes = new byte[20];
        final Random ran = new Random();
        ran.nextBytes(randomBytes);
        return randomBytes;
    }

}
