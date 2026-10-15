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

import java.time.Duration;

import org.awaitility.Awaitility;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * By-id cache hit/miss behaviour (no ACM) for the {@link TargetType} management service.
 */
// enable a real cache for the by-key (@Cacheable) cache too
@TestPropertySource(properties = "hawkbit.cache.targetType.spec=maximumSize=1000,expireAfterWrite=60s")
class TargetTypeManagementCacheTest extends AbstractTypeManagementCacheTest {

    /**
     * Scenario: evict a TargetType, read it (miss), then read it repeatedly (hits).
     * Proves: the first read (cache miss) hits the DB; the following reads are served from cache — 0 queries.
     */
    @Test
    void verifyTargetTypeCacheMissThenHit() {
        final TargetType targetType = testdataFactory.findOrCreateTargetType("cacheTargetType");
        evict(JpaTargetType.class.getSimpleName(), targetType.getId());

        final long beforeMiss = queryCount.countSelect();
        targetTypeManagement.get(targetType.getId()); // miss — hits DB
        assertThat(queryCount.countSelect() - beforeMiss)
                .as("cache miss must load from DB")
                .isPositive();

        final long beforeHits = queryCount.countSelect();
        for (int i = 0; i < 5; i++) {
            targetTypeManagement.get(targetType.getId()); // all served from cache
        }
        assertThat(queryCount.countSelect() - beforeHits)
                .as("repeated reads must be served from cache — 0 DB queries")
                .isZero();
    }

    /**
     * Scenario: read a TargetType by key repeatedly.
     * Proves: the by-key @Cacheable is applied - the first read hits the DB, the following reads are served from cache.
     */
    @Test
    void verifyTargetTypeByKeyCacheMissThenHit() {
        final TargetType targetType = testdataFactory.findOrCreateTargetType("cacheTargetTypeByKey");
        TenantAwareCacheManager.getInstance().getCache("targetType").evict(targetType.getKey());

        final long beforeMiss = queryCount.countSelect();
        targetTypeManagement.findByKey(targetType.getKey()); // miss — hits DB
        assertThat(queryCount.countSelect() - beforeMiss)
                .as("cache miss must load from DB")
                .isPositive();

        final long beforeHits = queryCount.countSelect();
        for (int i = 0; i < 5; i++) {
            targetTypeManagement.findByKey(targetType.getKey()); // all served from cache
        }
        assertThat(queryCount.countSelect() - beforeHits)
                .as("repeated reads by key must be served from cache — 0 DB queries")
                .isZero();
    }

    /**
     * Scenario: read a not existing TargetType by key repeatedly.
     * Proves: an empty result is not cached (and doesn't fail the cache put) - each read hits the DB.
     */
    @Test
    void verifyTargetTypeByKeyEmptyResultNotCached() {
        for (int i = 0; i < 2; i++) {
            final long before = queryCount.countSelect();
            assertThat(targetTypeManagement.findByKey("notExistingTargetTypeKey")).isEmpty();
            assertThat(queryCount.countSelect() - before)
                    .as("empty result must not be cached")
                    .isPositive();
        }
    }

    /**
     * Scenario: read a TargetType by key (cached), then update it.
     * Proves: the update (remote) event evicts the by-key cache - the following read by key returns the updated TargetType.
     */
    @Test
    void verifyTargetTypeByKeyCacheEvictedOnUpdate() {
        final TargetType targetType = testdataFactory.findOrCreateTargetType("cacheTargetTypeByKeyUpdate");
        assertThat(targetTypeManagement.findByKey(targetType.getKey())).isPresent(); // cached

        targetTypeManagement.update(TargetTypeManagement.Update.builder().id(targetType.getId()).description("updated").build());

        Awaitility.await().pollInSameThread().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(targetTypeManagement.findByKey(targetType.getKey())).hasValueSatisfying(
                        updated -> assertThat(updated.getDescription()).isEqualTo("updated")));
    }

    /**
     * Scenario: read a TargetType by key (cached), then delete it.
     * Proves: the delete (remote) event evicts the by-key cache - the following read by key returns empty.
     */
    @Test
    void verifyTargetTypeByKeyCacheEvictedOnDelete() {
        final TargetType targetType = testdataFactory.findOrCreateTargetType("cacheTargetTypeByKeyDelete");
        assertThat(targetTypeManagement.findByKey(targetType.getKey())).isPresent(); // cached

        targetTypeManagement.delete(targetType.getId());

        Awaitility.await().pollInSameThread().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(targetTypeManagement.findByKey(targetType.getKey())).isEmpty());
    }
}
