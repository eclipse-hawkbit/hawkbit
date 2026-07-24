/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.hawkbit.auth.SpPermission.READ_PREFIX;
import static org.eclipse.hawkbit.auth.SpPermission.TARGET_TYPE;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;

/**
 * ACM-aware by-id cache behaviour for the {@code TargetType} management service, served by the repository wrapper
 * ({@code BaseEntityRepositoryWrapper}). The wrapper mechanism is type-agnostic; this covers the cached-hit and
 * denied-caller paths for this type.
 */
class TargetTypeManagementCacheAcmTest extends AbstractTypeManagementCacheAcmTest {

    /**
     * Scenario: admin warms the cache, then a user permitted to read the entity reads it.
     * Proves: the permitted caller is served from the shared cache — 0 DB queries.
     */
    @Test
    void verifyPermittedUserServedFromCacheWithoutQuery() {
        targetTypeManagement.get(targetType1.getId()); // warm cache as admin

        runAs(withAuthorities(READ_PREFIX + TARGET_TYPE + "/id==" + targetType1.getId()), () -> {
            final long before = readQueries();
            targetTypeManagement.get(targetType1.getId());
            assertThat(readQueries() - before)
                    .as("cache hit for permitted user must produce 0 DB queries")
                    .isZero();
        });
    }

    /**
     * Scenario: cache is warmed, then a user NOT permitted for this entity calls get().
     * Proves: the in-memory gate rejects the cached entity with {@link EntityNotFoundException} and 0 DB queries —
     * a cache hit does not leak the entity to an unauthorized caller.
     */
    @Test
    void verifyDeniedUserThrowsWithoutQuery() {
        targetTypeManagement.get(targetType1.getId()); // warm cache

        runAs(withAuthorities(READ_PREFIX + TARGET_TYPE + "/id==" + targetType2.getId()), () -> {
            final long before = readQueries();
            assertThatThrownBy(() -> targetTypeManagement.get(targetType1.getId()))
                    .isInstanceOf(EntityNotFoundException.class);
            assertThat(readQueries() - before)
                    .as("permission rejection from cache must produce 0 DB queries")
                    .isZero();
        });
    }
}
