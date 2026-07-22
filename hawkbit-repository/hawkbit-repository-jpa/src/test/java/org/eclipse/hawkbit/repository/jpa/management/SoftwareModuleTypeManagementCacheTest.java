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

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.junit.jupiter.api.Test;

/**
 * By-id cache hit/miss behaviour (no ACM) for the {@link SoftwareModuleType} management service.
 */
class SoftwareModuleTypeManagementCacheTest extends AbstractTypeManagementCacheTest {

    /**
     * Scenario: evict an SMType, read it (miss), read it again (hit).
     * Proves: a leaf entity (no OneToMany/ManyToMany, so no N+1) still caches correctly — miss = exactly 1 query,
     * hit = 0 queries.
     */
    @Test
    void verifySoftwareModuleTypeCacheMissThenHit() {
        evict(JpaSoftwareModuleType.class.getSimpleName(), osType.getId());

        final long afterEvict = readQueries();
        softwareModuleTypeManagement.get(osType.getId()); // miss
        assertThat(readQueries() - afterEvict)
                .as("SMType cache miss must produce exactly 1 query")
                .isEqualTo(1);

        final long afterMiss = readQueries();
        softwareModuleTypeManagement.get(osType.getId()); // hit
        assertThat(readQueries() - afterMiss)
                .as("SMType cache hit must produce 0 queries")
                .isZero();
    }
}
