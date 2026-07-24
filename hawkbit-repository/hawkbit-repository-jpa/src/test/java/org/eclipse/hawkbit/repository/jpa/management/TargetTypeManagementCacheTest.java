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

}
