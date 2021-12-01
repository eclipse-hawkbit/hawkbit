/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.eclipse.hawkbit.repository.exception.ArtifactEncryptionUnsupportedException;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

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
