/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.junit.jupiter.api.Test;

/**
 * By-id cache hit/miss behaviour (no ACM) for the {@link DistributionSetType} management service.
 * <p>
 * Focus: prove that {@code get(id)} is served from the cache after the first load, so the repeated
 * DSType + N×SoftwareModuleType read storm happens once per entity instead of on every request.
 */
class DistributionSetTypeManagementCacheTest extends AbstractTypeManagementCacheTest {

    /**
     * Scenario: evict, then read the same DSType six times.
     * Proves: only the first read (cache miss) hits the DB; the next five are served from cache — 0 queries —
     * eliminating the repeated DSType + N×SoftwareModuleType query storm that motivated the cache.
     * <p/>
     * Note: a single miss still issues several queries — EclipseLink resolves each element's smType
     * {@code @ManyToOne} with its own ReadObjectQuery, which JOIN FETCH / an entity graph cannot collapse. That
     * per-miss cost is inherent and amortized by the cache; the point is it happens once per entity, not per request.
     */
    @Test
    void verifyRepeatedReadsOnlyMissHitsDb() {
        evict(JpaDistributionSetType.class.getSimpleName(), standardDsType.getId());

        final long beforeMiss = readQueries();
        distributionSetTypeManagement.get(standardDsType.getId()); // miss — hits DB
        assertThat(readQueries() - beforeMiss)
                .as("cache miss must load from DB")
                .isPositive();

        final long beforeHits = readQueries();
        for (int i = 0; i < 5; i++) {
            distributionSetTypeManagement.get(standardDsType.getId()); // all served from cache
        }
        assertThat(readQueries() - beforeHits)
                .as("repeated reads must be served from cache — 0 DB queries")
                .isZero();
    }

    /**
     * Scenario: load a DSType on a cold cache, then walk its mandatory/optional module types.
     * Proves: the cached entity is complete — elements and their smTypes are fetched eagerly during the load, so
     * navigating them afterwards triggers no extra query (no lazy N+1 hidden behind the cache).
     * The default test DS type has 1 mandatory (os) and 2 optional (runtime, app) module types.
     */
    @Test
    void verifyCachedEntityModuleTypesAccessibleWithoutQuery() {
        evict(JpaDistributionSetType.class.getSimpleName(), standardDsType.getId());
        final DistributionSetType loaded = distributionSetTypeManagement.get(standardDsType.getId());

        final long before = readQueries();
        assertThat(loaded.getMandatoryModuleTypes()).hasSize(1); // os
        assertThat(loaded.getOptionalModuleTypes()).hasSize(2); // runtime + app
        assertThat(readQueries() - before)
                .as("module types on the fully-loaded cached entity must not hit DB")
                .isZero();
    }
}
