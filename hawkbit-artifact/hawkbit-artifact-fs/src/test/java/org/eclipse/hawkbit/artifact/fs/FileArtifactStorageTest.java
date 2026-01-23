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
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
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
    @SuppressWarnings("java:S2245") // used for tests only, no need of secure random
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
                delete(new File(artifactResourceProperties.getPath()));
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

        try (final InputStream is = artifactFilesystemRepository.getBySha1(TENANT, artifact.getHashes().sha1())) {
            final byte[] readContent = is.readAllBytes();
            assertThat(readContent).isEqualTo(fileContent);
        }
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

    private static void delete(final File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                // delete children
                final File[] children = file.listFiles();
                if (children != null) {
                    for (final File child : children) {
                        delete(child);
                    }
                }
            }

            Files.delete(file.toPath());
        }
    }
}