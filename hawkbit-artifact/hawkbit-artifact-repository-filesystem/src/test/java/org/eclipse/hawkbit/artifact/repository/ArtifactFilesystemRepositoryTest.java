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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
/**
 * Feature: Unit Tests - Artifact File System Repository<br/>
 * Story: Test storing artifact binaries in the file-system
 */
class ArtifactFilesystemRepositoryTest {

    private static final String TENANT = "test_tenant";

    private static ArtifactFilesystemProperties artifactResourceProperties;
    private static ArtifactFilesystemRepository artifactFilesystemRepository;

    @BeforeAll
    static void setup() {
        artifactResourceProperties = new ArtifactFilesystemProperties();
        artifactResourceProperties.setPath(AbstractArtifactRepository.createTempFile(true).toString());

        artifactFilesystemRepository = new ArtifactFilesystemRepository(artifactResourceProperties);
    }

    @AfterAll
    static void afterClass() {
        if (new File(artifactResourceProperties.getPath()).exists()) {
            try {
                FileUtils.deleteDirectory(new File(artifactResourceProperties.getPath()));
            } catch (final IOException | IllegalArgumentException e) {
                log.warn("Cannot delete file-directory", e);
            }
        }
    }

    /**
     * Verifies that an artifact can be successfully stored in the file-system repository
     */
    @Test    void storeSuccessfully() throws IOException {
        final byte[] fileContent = randomBytes();
        final AbstractDbArtifact artifact = storeRandomArtifact(fileContent);

        final byte[] readContent = new byte[fileContent.length];
        IOUtils.read(artifact.getFileInputStream(), readContent);
        assertThat(readContent).isEqualTo(fileContent);
    }

    /**
     * Verifies that an artifact can be successfully stored in the file-system repository
     */
    @Test    void getStoredArtifactBasedOnSHA1Hash() throws IOException {
        final byte[] fileContent = randomBytes();
        final AbstractDbArtifact artifact = storeRandomArtifact(fileContent);

        assertThat(artifactFilesystemRepository.getArtifactBySha1(TENANT, artifact.getHashes().getSha1())).isNotNull();
    }

    /**
     * Verifies that an artifact can be deleted in the file-system repository
     */
    @Test    void deleteStoredArtifactBySHA1Hash() throws IOException {
        final AbstractDbArtifact artifact = storeRandomArtifact(randomBytes());
        artifactFilesystemRepository.deleteBySha1(TENANT, artifact.getHashes().getSha1());

        assertThat(artifactFilesystemRepository.getArtifactBySha1(TENANT, artifact.getHashes().getSha1())).isNull();
    }

    /**
     * Verifies that all artifacts of a tenant can be deleted in the file-system repository
     */
    @Test    void deleteStoredArtifactOfTenant() throws IOException {
        final AbstractDbArtifact artifact = storeRandomArtifact(randomBytes());
        artifactFilesystemRepository.deleteByTenant(TENANT);

        assertThat(artifactFilesystemRepository.getArtifactBySha1(TENANT, artifact.getHashes().getSha1())).isNull();
    }

    /**
     * Verifies that an artifact which does not exists is deleted quietly in the file-system repository
     */
    @Test    void deleteArtifactWhichDoesNotExistsBySHA1HashWithoutException() throws IOException {
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

    private static byte[] randomBytes() {
        final byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        return randomBytes;
    }

    private AbstractDbArtifact storeRandomArtifact(final byte[] fileContent) throws IOException {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent)) {
            return artifactFilesystemRepository.store(TENANT, inputStream, "filename.tmp", "application/txt", null);
        }
    }
}