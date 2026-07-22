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
import static org.eclipse.hawkbit.auth.SpPermission.DISTRIBUTION_SET_TYPE;
import static org.eclipse.hawkbit.auth.SpPermission.READ_PREFIX;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.junit.jupiter.api.Test;

/**
 * ACM-aware by-id cache behaviour for the {@code DistributionSetType} management service, served by the repository
 * wrapper ({@code BaseEntityRepositoryWrapper}).
 */
class DistributionSetTypeManagementCacheAcmTest extends AbstractTypeManagementCacheAcmTest {

    /**
     * Scenario: admin warms the cache, then a user permitted to read the entity reads it.
     * Proves: the permitted caller is served from the shared cache — 0 DB queries.
     */
    @Test
    void verifyPermittedUserServedFromCacheWithoutQuery() {
        distributionSetTypeManagement.get(dsType1.getId()); // warm cache as admin

        // measure inside runAs, after the harness' one-off tenant setup, so only the cache access is counted
        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE + "/id==" + dsType1.getId()), () -> {
            final long before = readQueries();
            distributionSetTypeManagement.get(dsType1.getId());
            assertThat(readQueries() - before)
                    .as("cache hit for permitted user must produce 0 DB queries")
                    .isZero();
        });
    }

    /**
     * Scenario: cache is warmed, then a user NOT permitted for this entity calls get().
     * Proves: the in-memory gate rejects the cached entity with {@link EntityNotFoundException} and 0 DB queries —
     * a cache hit does not leak the entity to an unauthorized caller. Regression guard for the "cached read skips ACM"
     * bug (matches the pre-cache ACM-spec path, which filtered the row out and made getById throw ENF).
     */
    @Test
    void verifyDeniedUserThrowsWithoutQuery() {
        distributionSetTypeManagement.get(dsType1.getId()); // warm cache

        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE + "/id==" + dsType2.getId()), () -> {
            final long before = readQueries();
            assertThatThrownBy(() -> distributionSetTypeManagement.get(dsType1.getId()))
                    .isInstanceOf(EntityNotFoundException.class);
            assertThat(readQueries() - before)
                    .as("permission rejection from cache must produce 0 DB queries")
                    .isZero();
        });
    }

    /**
     * Scenario: cache is warmed, then a denied user calls find().
     * Proves: find() returns {@link java.util.Optional#empty()} for the denied caller — not the entity, not an
     * exception (the find() path filters instead of throwing).
     */
    @Test
    void verifyDeniedUserFindReturnsEmpty() {
        distributionSetTypeManagement.get(dsType1.getId()); // warm cache

        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE + "/id==" + dsType2.getId()), () -> assertThat(distributionSetTypeManagement
                .find(dsType1.getId())).isEmpty());
    }

    /**
     * Scenario: cold cache, a permitted user reads the same id twice.
     * Proves: the first read (miss) populates the cache with the raw entity; the second read is served from cache
     * — 0 DB queries.
     */
    @Test
    void verifyCacheMissThenHitWithoutQuery() {
        evict(JpaDistributionSetType.class.getSimpleName(), dsType1.getId());

        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE + "/id==" + dsType1.getId()), () -> {
            distributionSetTypeManagement.get(dsType1.getId()); // miss — populates cache

            final long before = readQueries();
            distributionSetTypeManagement.get(dsType1.getId()); // hit
            assertThat(readQueries() - before)
                    .as("second ACM call must serve from cache — 0 DB queries")
                    .isZero();
        });
    }

    /**
     * Scenario: cold cache; a DENIED user triggers the miss, then a PERMITTED user reads the same id.
     * Proves: the denied miss still caches the RAW (not permission-filtered) entity — the denied caller is rejected
     * in-memory with {@link EntityNotFoundException}, and the permitted caller immediately after is served from the
     * now-warm cache with 0 DB queries. Confirms the gate (not the cache) enforces access: nothing leaks to the
     * denied caller, nothing is wrongly withheld from the permitted one.
     */
    @Test
    void verifyDeniedColdMissCachesRawEntityForPermittedUser() {
        evict(JpaDistributionSetType.class.getSimpleName(), dsType1.getId());

        // denied user causes the cold cache miss
        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE + "/id==" + dsType2.getId()), () -> assertThatThrownBy(
                () -> distributionSetTypeManagement.get(dsType1.getId()))
                .isInstanceOf(EntityNotFoundException.class));

        // raw entity is now cached; a permitted user must serve from cache without any DB access
        // (measure inside runAs, after the harness' one-off tenant setup)
        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE + "/id==" + dsType1.getId()), () -> {
            final long before = readQueries();
            assertThat(distributionSetTypeManagement.get(dsType1.getId()).getId()).isEqualTo(dsType1.getId());
            assertThat(readQueries() - before)
                    .as("permitted user after a denied cold-miss must serve from cache — 0 DB queries")
                    .isZero();
        });
    }
}
