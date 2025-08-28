/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.eclipse.hawkbit.repository.artifact.encryption.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.artifact.exception.ArtifactEncryptionUnsupportedException;
import org.junit.jupiter.api.Test;

/**
 * Test class to verify that no {@link ArtifactEncryptionService} required beans
 * are loaded and therefore the encryption support is not given.
 * <p/>
 * Feature: Unit Tests - Repository<br/>
 * Story: Artifact Encryption Service
 */
class ArtifactEncryptionServiceTest {

    /**
     * Verify that no artifact encryption support is given
     */
    @Test
    void verifyNoArtifactEncryptionSupport() {
        final ArtifactEncryptionService artifactEncryptionService = ArtifactEncryptionService.getInstance();

        assertThat(artifactEncryptionService.isEncryptionSupported()).isFalse();
        assertThatExceptionOfType(ArtifactEncryptionUnsupportedException.class)
                .isThrownBy(() -> artifactEncryptionService.addEncryptionSecrets(1L));
        assertThatExceptionOfType(ArtifactEncryptionUnsupportedException.class)
                .isThrownBy(() -> artifactEncryptionService.encryptArtifact(1L, null));
        assertThatExceptionOfType(ArtifactEncryptionUnsupportedException.class)
                .isThrownBy(() -> artifactEncryptionService.decryptArtifact(1L, null));
    }
}