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

import java.util.Collection;

import org.junit.jupiter.api.Test;

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
}