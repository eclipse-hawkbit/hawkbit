/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.throttle;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Operator-owned per-tenant throttling configuration. Pure Spring properties — deliberately not
 * customer-facing tenant configuration, so a tenant cannot raise or disable its own throttle.
 * Caps how many of the shared DB connection pool's slots a single tenant may hold at once.
 * Per-tenant overrides live in the {@link #tenants} map.
 * <p/>
 * <strong>Configuration Examples:</strong>
 * <pre>{@code
 * # Mode 1: Disabled (default)
 * hawkbit.throttle.enabled=false
 *
 * # Mode 2: Hard cap, fast-reject
 * hawkbit.throttle.enabled=true
 * hawkbit.throttle.limit=5           # each tenant max 5 connections
 * hawkbit.throttle.timeout=0         # reject immediately when over limit
 * hawkbit.throttle.threshold=-1      # no threshold (always enforce limit)
 *
 * # Mode 3: Contention-aware burst allowance
 * hawkbit.throttle.enabled=true
 * hawkbit.throttle.limit=10          # ceiling per tenant
 * hawkbit.throttle.timeout=20s       # wait up to 20s before HTTP 429
 * hawkbit.throttle.threshold=8       # fairness kicks in at 8/10 pool utilization
 * spring.threads.virtual.enabled=true  # REQUIRED for timeout>0
 * }</pre>
 */
@Data
@ConfigurationProperties("hawkbit.throttle")
public class ThrottleProperties {

    /**
     * Master switch. Default OFF ⇒ no DataSource wrapping, no filter, byte-for-byte current behavior.
     */
    private boolean enabled = false;

    /**
     * Global per-tenant ceiling on concurrent connection borrows. {@code -1} = no ceiling (bounded
     * only by the dynamic fair share).
     */
    private int limit = -1;

    /**
     * Max wait for a permit before {@link ThrottledException} (→ HTTP 429). {@code 0} fast-rejects
     * instead of blocking (safe on platform threads); a positive value is wait-then-serve
     * backpressure (requires virtual threads enabled). Default: 0 (safe-first; switch to 20s after
     * virtual thread validation).
     */
    private Duration timeout = Duration.ZERO;

    /**
     * Fair-share capacity. {@code -1} or {@code 0} = auto-detect the Hikari {@code maximumPoolSize};
     * set explicitly only when auto-detect is not possible.
     */
    private int capacity = -1;

    /**
     * Contention threshold (absolute slot count). When global in-use reaches this threshold, fairness
     * enforcement activates: each tenant is capped at its dynamic fair share (capacity / activeTenants,
     * clamped to its limit). Below the threshold, any tenant may burst freely up to pool capacity.
     * {@code -1} = no threshold (always enforce hard cap on tenant limit).
     * Default: -1 (hard cap mode).
     */
    private int threshold = -1;

    /** Operator-only per-tenant overrides (tenant id → limit; {@code -1} = opt out). */
    private final Map<String, Integer> tenants = new HashMap<>();

    /**
     * Resolve the effective limit for a specific tenant.
     *
     * @param tenant the tenant identifier
     * @return the per-tenant ceiling: the tenant override if set in {@link #tenants}, else the global
     *             {@link #limit}. {@code -1} = unlimited (bounded only by capacity and dynamic fair share).
     */
    public int limit(final String tenant) {
        return tenants.getOrDefault(tenant, limit);
    }
}
