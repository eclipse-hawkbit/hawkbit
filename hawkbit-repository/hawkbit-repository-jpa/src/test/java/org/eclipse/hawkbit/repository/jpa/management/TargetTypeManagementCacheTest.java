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

import java.util.Set;

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * By-id cache hit/miss behaviour (no ACM) for the {@link TargetType} management service.
 */
class TargetTypeManagementCacheTest extends AbstractTypeManagementCacheTest {

    /**
     * Scenario: evict a TargetType, read it (miss), then read it repeatedly (hits).
     * Proves: the first read (cache miss) hits the DB; the following reads are served from cache — 0 queries.
     */
    @Test
    void verifyTargetTypeCacheMissThenHit() {
        final TargetType targetType = testdataFactory.findOrCreateTargetType("cacheTargetType");
        evict(JpaTargetType.class.getSimpleName(), targetType.getId());

        final long beforeMiss = readQueries();
        targetTypeManagement.get(targetType.getId()); // miss — hits DB
        assertThat(readQueries() - beforeMiss)
                .as("cache miss must load from DB")
                .isPositive();

        final long beforeHits = readQueries();
        for (int i = 0; i < 5; i++) {
            targetTypeManagement.get(targetType.getId()); // all served from cache
        }
        assertThat(readQueries() - beforeHits)
                .as("repeated reads must be served from cache — 0 DB queries")
                .isZero();
    }

    /**
     * Scenario: load a TargetType on a cold cache, then walk its compatible distribution set types.
     * Documents a KNOWN GAP: {@code TargetType.distributionSetTypes} is a {@code @ManyToMany} with the default LAZY
     * fetch, so it is NOT materialized during the by-id cache load. Navigating it on a cache hit currently re-queries
     * (observed: 8 DB queries), i.e. the by-id cache does not cover this relation - unlike the EAGER
     * {@code DistributionSetType.elements}. Disabled until the relation-overload fix lands (make the relation resolve
     * from the shared type cache / batch-fetch / eager); this is its acceptance test.
     */
    @Disabled("known gap: lazy @ManyToMany is not served by the by-id cache - re-enable with the relation-overload fix")
    @Test
    void verifyCachedEntityDistributionSetTypesAccessibleWithoutQuery() {
        final TargetType targetType = testdataFactory.createTargetType("cacheTargetTypeRel", Set.of(standardDsType));
        evict(JpaTargetType.class.getSimpleName(), targetType.getId());
        final TargetType loaded = targetTypeManagement.get(targetType.getId());

        final long before = readQueries();
        assertThat(loaded.getDistributionSetTypes()).hasSize(1);
        assertThat(readQueries() - before)
                .as("distribution set types on the fully-loaded cached entity must not hit DB")
                .isZero();
    }
}
