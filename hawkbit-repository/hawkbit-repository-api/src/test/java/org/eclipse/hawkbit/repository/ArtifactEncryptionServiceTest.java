/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.exception.ArtifactEncryptionUnsupportedException;
import org.junit.jupiter.api.Test;

/**
 * Test class to verify that no {@link ArtifactEncryptionService} required beans
 * are loaded and therefore the encryption support is not given.
 */
@Feature("Unit Tests - Repository")
@Story("Artifact Encryption Service")
class ArtifactEncryptionServiceTest {

    @Test
    @Description("Verify that no artifact encryption support is given")
    void verifyNoArtifactEncryptionSupport() {
        final ArtifactEncryptionService artifactEncryptionService = ArtifactEncryptionService.getInstance();

        assertThat(artifactEncryptionService.isEncryptionSupported()).isFalse();
        assertThatExceptionOfType(ArtifactEncryptionUnsupportedException.class)
                .isThrownBy(() -> artifactEncryptionService.addSoftwareModuleEncryptionSecrets(1L));
        assertThatExceptionOfType(ArtifactEncryptionUnsupportedException.class)
                .isThrownBy(() -> artifactEncryptionService.encryptSoftwareModuleArtifact(1L, null));
        assertThatExceptionOfType(ArtifactEncryptionUnsupportedException.class)
                .isThrownBy(() -> artifactEncryptionService.decryptSoftwareModuleArtifact(1L, null));
    }
}
