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

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.QueryCount;
import org.eclipse.hawkbit.repository.test.util.QueryCountConfiguration;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Base for the per-type by-id cache tests (no ACM). Enables a real cache for the cached type management services and
 * counts SQL via a provider-agnostic JDBC recorder ({@link QueryCount}), so the same assertions hold under both
 * EclipseLink and Hibernate.
 * <p>
 * A dedicated class hierarchy (rather than folding these into the per-type {@code *ManagementTest} classes) is required
 * because the real-cache specs below are class-level and must not change caching semantics for the unrelated
 * assertions in those classes - the test defaults set {@code maximumSize=0} (NOP cache), which would otherwise defeat
 * every hit/miss assertion here.
 */
@Import(QueryCountConfiguration.class)
@TestPropertySource(properties = {
        // enable a real cache - the test defaults set maximumSize=0 (NOP), which would defeat these assertions
        "hawkbit.cache.JpaDistributionSetType.spec=maximumSize=1000,expireAfterWrite=60s",
        "hawkbit.cache.JpaSoftwareModuleType.spec=maximumSize=1000,expireAfterWrite=60s",
        "hawkbit.cache.JpaTargetType.spec=maximumSize=1000,expireAfterWrite=60s" })
abstract class AbstractTypeManagementCacheTest extends AbstractJpaIntegrationTest {

    @Autowired
    private QueryCount queryCount;

    /** Evicts the given id from the by-id cache named after the entity class' simple name. */
    protected void evict(final String cacheName, final Long id) {
        TenantAwareCacheManager.getInstance().getCache(cacheName).evict(id);
    }

    /** Cumulative count of {@code SELECT} statements sent to the DB - use deltas around the measured section. */
    protected long readQueries() {
        return queryCount.selects();
    }
}
