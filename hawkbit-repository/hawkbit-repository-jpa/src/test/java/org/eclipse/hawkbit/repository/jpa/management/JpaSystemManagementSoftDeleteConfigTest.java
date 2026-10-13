/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;

import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - Repository<br/>
 * Story: Tenant soft delete configuration
 */
class JpaSystemManagementSoftDeleteConfigTest {

    /**
     * Verifies that the system management can't be created (the application doesn't boot) if the soft delete after is greater
     * than the hard delete after - soft deletes would always fail since the hard delete time would be before the soft delete time.
     */
    @Test
    void softDeleteAfterGreaterThanHardDeleteAfterFailsOnStartup() {
        final RepositoryProperties repositoryProperties = repositoryProperties(Duration.ofDays(31), Duration.ofDays(30));
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> systemManagement(repositoryProperties));
    }

    /**
     * Verifies that the system management is created if the soft delete after is less than or equal to the hard delete after.
     */
    @Test
    void softDeleteAfterNotGreaterThanHardDeleteAfterIsAccepted() {
        assertThatCode(() -> systemManagement(repositoryProperties(Duration.ofDays(7), Duration.ofDays(30)))).doesNotThrowAnyException();
        assertThatCode(() -> systemManagement(repositoryProperties(Duration.ofDays(30), Duration.ofDays(30)))).doesNotThrowAnyException();
        // defaults
        assertThatCode(() -> systemManagement(new RepositoryProperties())).doesNotThrowAnyException();
    }

    /**
     * Verifies that the check is skipped if soft deletes are disabled (hard delete after not positive).
     */
    @Test
    void softDeletesDisabledIsAccepted() {
        assertThatCode(() -> systemManagement(repositoryProperties(Duration.ofDays(7), Duration.ZERO))).doesNotThrowAnyException();
    }

    private static RepositoryProperties repositoryProperties(final Duration softDeleteAfter, final Duration hardDeleteAfter) {
        final RepositoryProperties repositoryProperties = new RepositoryProperties();
        repositoryProperties.setTenantSoftDeleteAfter(softDeleteAfter);
        repositoryProperties.setTenantHardDeleteAfter(hardDeleteAfter);
        return repositoryProperties;
    }

    private static JpaSystemManagement systemManagement(final RepositoryProperties repositoryProperties) {
        return new JpaSystemManagement(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, repositoryProperties, null);
    }
}
