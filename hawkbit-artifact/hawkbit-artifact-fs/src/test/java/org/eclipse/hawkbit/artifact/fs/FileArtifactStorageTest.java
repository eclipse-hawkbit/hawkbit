/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.fs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.artifact.AbstractArtifactStorage;
import org.eclipse.hawkbit.artifact.exception.ArtifactBinaryNotFoundException;
import org.eclipse.hawkbit.artifact.model.StoredArtifactInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - Artifact File System Repository<br/>
 * Story: Test storing artifact binaries in the file-system
 */
@Slf4j
class FileArtifactStorageTest {

    private static final String TENANT = "test_tenant";
    @SuppressWarnings("java:S1068") // used for tests only, no need of secure random
    private static final Random RND = new Random();

    private static FileArtifactProperties artifactResourceProperties;
    private static FileArtifactStorage artifactFilesystemRepository;

    @BeforeAll
    static void setup() {
        artifactResourceProperties = new FileArtifactProperties();
        artifactResourceProperties.setPath(AbstractArtifactStorage.createTempFile(true).toString());

        artifactFilesystemRepository = new FileArtifactStorage(artifactResourceProperties);
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
    @Test
    void storeSuccessfully() throws IOException {
        final byte[] fileContent = randomBytes();
        final StoredArtifactInfo artifact = storeRandomArtifact(fileContent);

        final byte[] readContent = new byte[fileContent.length];
        IOUtils.read(artifactFilesystemRepository.getBySha1(TENANT, artifact.getHashes().sha1()), readContent);
        assertThat(readContent).isEqualTo(fileContent);
    }

    /**
     * Verifies that an artifact can be successfully stored in the file-system repository
     */
    @Test
    void getStoredArtifactBasedOnSHA1Hash() throws IOException {
        final byte[] fileContent = randomBytes();
        final StoredArtifactInfo artifact = storeRandomArtifact(fileContent);

        assertThat(artifactFilesystemRepository.getBySha1(TENANT, artifact.getHashes().sha1())).isNotNull();
    }

    /**
     * Verifies that an artifact can be deleted in the file-system repository
     */
    @Test
    void deleteStoredArtifactBySHA1Hash() throws IOException {
        final StoredArtifactInfo artifact = storeRandomArtifact(randomBytes());
        artifactFilesystemRepository.deleteBySha1(TENANT, artifact.getHashes().sha1());

        final String sha1Hash = artifact.getHashes().sha1();
        assertThatExceptionOfType(ArtifactBinaryNotFoundException.class)
                .isThrownBy(() -> artifactFilesystemRepository.getBySha1(TENANT, sha1Hash));
    }

    /**
     * Verifies that all artifacts of a tenant can be deleted in the file-system repository
     */
    @Test
    void deleteStoredArtifactOfTenant() throws IOException {
        final StoredArtifactInfo artifact = storeRandomArtifact(randomBytes());
        artifactFilesystemRepository.deleteByTenant(TENANT);

        final String sha1Hash = artifact.getHashes().sha1();
        assertThatExceptionOfType(ArtifactBinaryNotFoundException.class)
                .isThrownBy(() -> artifactFilesystemRepository.getBySha1(TENANT, sha1Hash));
    }

    /**
     * Verifies that an artifact which does not exists is deleted quietly in the file-system repository
     */
    @Test
    void deleteArtifactWhichDoesNotExistsBySHA1HashWithoutException() throws IOException {
        try {
            artifactFilesystemRepository.deleteBySha1(TENANT, "sha1HashWhichDoesNotExists");
        } catch (final Exception e) {
            Assertions.fail("did not expect an exception while deleting a file which does not exists");
        }

        final StoredArtifactInfo artifact = storeRandomArtifact(randomBytes());
        try {
            artifactFilesystemRepository.deleteBySha1("tenantWhichDoesNotExist", artifact.getHashes().sha1());
        } catch (final Exception e) {
            Assertions.fail("did not expect an exception while deleting a file which does not exists");
        }
    }

    private static byte[] randomBytes() {
        final byte[] randomBytes = new byte[20];
        RND.nextBytes(randomBytes);
        return randomBytes;
    }

    private StoredArtifactInfo storeRandomArtifact(final byte[] fileContent) throws IOException {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent)) {
            return artifactFilesystemRepository.store(TENANT, inputStream, "filename.tmp", "application/txt", null);
        }
    }
}