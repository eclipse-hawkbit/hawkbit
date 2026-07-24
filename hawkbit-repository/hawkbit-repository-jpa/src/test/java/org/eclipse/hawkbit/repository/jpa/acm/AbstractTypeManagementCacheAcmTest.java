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

import org.eclipse.hawkbit.repository.test.util.QueryCount;
import org.eclipse.hawkbit.repository.test.util.QueryCountConfiguration;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Base for the per-type ACM-aware cache tests served by the repository wrapper ({@code BaseEntityRepositoryWrapper}).
 * <p>
 * Focus: prove the wrapper caches the RAW (permission-agnostic) entity by id and enforces access in-memory on top, so
 * that one cache entry is shared across principals, a permission check on a cache hit costs 0 DB queries, and a denied
 * caller is rejected without a DB hit.
 * <p>
 * Enables a real cache (test defaults set {@code maximumSize=0}, a NOP cache, which would defeat these assertions) and
 * counts SQL via a provider-agnostic JDBC recorder ({@link QueryCount}), so the same assertions hold under both
 * EclipseLink and Hibernate.
 */
@Import(QueryCountConfiguration.class)
@TestPropertySource(properties = {
        // enable a real cache - the test defaults set maximumSize=0 (NOP), which would defeat these assertions
        "hawkbit.cache.JpaDistributionSetType.spec=maximumSize=1000,expireAfterWrite=60s",
        "hawkbit.cache.JpaSoftwareModuleType.spec=maximumSize=1000,expireAfterWrite=60s",
        "hawkbit.cache.JpaTargetType.spec=maximumSize=1000,expireAfterWrite=60s" })
abstract class AbstractTypeManagementCacheAcmTest extends AbstractAccessControllerManagementTest {

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
