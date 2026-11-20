/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_STATUS_CODE_CANCELING;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantConfiguration;
import org.eclipse.hawkbit.repository.test.util.QueryCount;
import org.eclipse.hawkbit.repository.test.util.QueryCountConfiguration;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for the tenant configuration cache, including caching of not set keys.
 */
@Import(QueryCountConfiguration.class)
@TestPropertySource(properties = {
        // enable a real cache - the test defaults set maximumSize=0 (NOP), which would defeat these assertions
        "hawkbit.cache.JpaTenantConfiguration.spec=maximumSize=1000,expireAfterWrite=60s",
        "hawkbit.cache.JpaTenantConfiguration.cache-null=true" })
class TenantConfigurationCacheTest extends AbstractJpaIntegrationTest {

    private static final String CACHE_NAME = JpaTenantConfiguration.class.getSimpleName();

    @Autowired
    private QueryCount queryCount;

    @BeforeEach
    void clearCache() {
        TenantAwareCacheManager.getInstance().getCache(CACHE_NAME).clear();
    }

    /**
     * Scenario: read a key that is not set (and has no default) repeatedly.
     * Proves: the "not set" result is cached - the first read hits the DB, the following reads are served from cache.
     */
    @Test
    void notSetKeyIsCached() {
        final long beforeMiss = queryCount.countSelect();
        assertThat(getCanceling()).isNull(); // miss - hits DB
        assertThat(queryCount.countSelect() - beforeMiss)
                .as("cache miss must load from DB")
                .isPositive();

        final long beforeHits = queryCount.countSelect();
        for (int i = 0; i < 5; i++) {
            assertThat(getCanceling()).isNull(); // served from cache
        }
        assertThat(queryCount.countSelect() - beforeHits)
                .as("repeated reads of a not set key must be served from cache - 0 DB queries")
                .isZero();
    }

    /**
     * Scenario: read a not set key (cached as not set), then set it.
     * Proves: the create (remote) event evicts the cached "not set" result - the following read returns the new value.
     */
    @Test
    void createEvictsCachedNotSetKey() {
        assertThat(getCanceling()).isNull(); // cached as not set

        tenantConfigurationManagement().addOrUpdateConfiguration(ACTION_STATUS_CODE_CANCELING, 9001);

        Awaitility.await().pollInSameThread().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(getCanceling()).isEqualTo(9001));
    }

    /**
     * Scenario: set a key and read it (cached), then delete it.
     * Proves: the delete event evicts the cached value - the following read returns not set.
     */
    @Test
    void deleteEvictsCachedKey() {
        tenantConfigurationManagement().addOrUpdateConfiguration(ACTION_STATUS_CODE_CANCELING, 9001);
        Awaitility.await().pollInSameThread().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(getCanceling()).isEqualTo(9001)); // cached

        tenantConfigurationManagement().deleteConfiguration(ACTION_STATUS_CODE_CANCELING);

        Awaitility.await().pollInSameThread().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(getCanceling()).isNull());
    }

    // a key without default that is not set is returned as a global value with value null
    private static Integer getCanceling() {
        return tenantConfigurationManagement().getConfigurationValue(ACTION_STATUS_CODE_CANCELING, Integer.class).getValue();
    }
}
