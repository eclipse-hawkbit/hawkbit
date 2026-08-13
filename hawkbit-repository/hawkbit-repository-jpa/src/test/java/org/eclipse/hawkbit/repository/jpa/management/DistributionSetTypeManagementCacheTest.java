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
     * Proves two things: (1) the cold miss loads the DS-type WITHOUT the {@code sp_software_module_type} by-id storm -
     * since {@code DistributionSetTypeElement.smType} is no longer {@code @MapsId}, materialising the elements no longer
     * pulls one {@code SoftwareModuleType} entity per element; (2) the next five reads are served from the by-id cache
     * (0 queries). Before the fix the miss issued 1 DSType + 1 element + N smType by-id selects; now it issues no smType
     * by-id at all. Provider-agnostic (the storm was EclipseLink-only; Hibernate already joined - both now yield 0).
     */
    @Test
    void missLoadsTypeWithoutModuleTypeStormThenReadsAreCached() {
        evict(JpaDistributionSetType.class.getSimpleName(), standardDsType.getId());

        queryUtil.resetQueries();
        distributionSetTypeManagement.get(standardDsType.getId()); // cache miss -> DB
        assertThat(queryUtil.countSelectQueries()).as("cache miss must load from DB").isPositive();
        assertThat(queryUtil.countSelectsFromTable("sp_software_module_type"))
                .as("loading a DS-type must NOT storm sp_software_module_type by-id (element smType is no longer @MapsId)")
                .isZero();

        queryUtil.resetQueries();
        for (int i = 0; i < 5; i++) {
            distributionSetTypeManagement.get(standardDsType.getId()); // all served from cache
        }
        assertThat(queryUtil.countSelectQueries()).as("repeated reads must be served from cache - 0 DB queries").isZero();
    }
}
