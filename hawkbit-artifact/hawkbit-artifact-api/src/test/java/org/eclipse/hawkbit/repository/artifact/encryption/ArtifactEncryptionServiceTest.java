/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.artifact.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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