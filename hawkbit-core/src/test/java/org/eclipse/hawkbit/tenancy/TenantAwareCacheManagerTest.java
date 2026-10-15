/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class TenantAwareCacheManagerTest {

    /**
     * Double-checks that all permissions doesn't contain any hierarchies.
     */
    @Test
    void testNopCaches() {
        assertThat(TenantAwareCacheManager.isNop(null)).isTrue();
        assertThat(TenantAwareCacheManager.isNop("")).isTrue();
        assertThat(TenantAwareCacheManager.isNop(" ")).isTrue();
        assertThat(TenantAwareCacheManager.isNop("\t")).isTrue();
        assertThat(TenantAwareCacheManager.isNop("nop")).isTrue();
        assertThat(TenantAwareCacheManager.isNop("none")).isTrue();
        assertThat(TenantAwareCacheManager.isNop("maximumSize=0")).isTrue();
        assertThat(TenantAwareCacheManager.isNop("maximumSize=0,something")).isTrue();
        assertThat(TenantAwareCacheManager.isNop("something, maximumSize=0")).isTrue();
        assertThat(TenantAwareCacheManager.isNop("something, maximumSize=0 \t, something")).isTrue();
        assertThat(TenantAwareCacheManager.isNop(" expireAfterWrite=0")).isTrue();
        assertThat(TenantAwareCacheManager.isNop("expireAfterWrite=0,something")).isTrue();
        assertThat(TenantAwareCacheManager.isNop("something, expireAfterWrite =0")).isTrue();
        assertThat(TenantAwareCacheManager.isNop("something, expireAfterWrite= 0 \t, something")).isTrue();

        assertThat(TenantAwareCacheManager.isNop("maximumSize=01")).isFalse();
        assertThat(TenantAwareCacheManager.isNop("maximumSize=100")).isFalse();
        assertThat(TenantAwareCacheManager.isNop("expireAfterWrite=01")).isFalse();
        assertThat(TenantAwareCacheManager.isNop("expireAfterWrite=100")).isFalse();
    }

    /**
     * Verifies that null values are not cached by default and are cached if cache-null is configured for the cache.
     */
    @Test
    void nullValuesCachedOnlyIfAllowed() {
        final StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "hawkbit.cache.spec", "expireAfterWrite=60s",
                "hawkbit.cache.nullAllowed.cache-null", "true")));
        final TenantAwareCacheManager cacheManager = TenantAwareCacheManager.getInstance();
        cacheManager.init(env);

        final Cache nullNotAllowed = cacheManager.getCache("nullNotAllowed");
        assertThat(loadNullTwice(nullNotAllowed)).as("null shall not be cached by default").isEqualTo(2);
        nullNotAllowed.put("key", null); // shall not fail (as @Cacheable does for null results)
        assertThat(nullNotAllowed.get("key")).isNull();

        final Cache nullAllowed = cacheManager.getCache("nullAllowed");
        assertThat(loadNullTwice(nullAllowed)).as("null shall be cached if allowed").isEqualTo(1);
        nullAllowed.put("key2", null);
        assertThat(nullAllowed.get("key2")).isNotNull().extracting(Cache.ValueWrapper::get).isNull();
    }

    private static int loadNullTwice(final Cache cache) {
        final AtomicInteger loads = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            assertThat(cache.<Object> get("key", () -> {
                loads.incrementAndGet();
                return null;
            })).isNull();
        }
        return loads.get();
    }
}
