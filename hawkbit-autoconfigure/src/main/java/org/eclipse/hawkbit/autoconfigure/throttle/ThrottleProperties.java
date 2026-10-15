/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.throttle;

import static org.eclipse.hawkbit.throttle.Config.PendingRemoveStrategy.FIFO;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.throttle.Config;
import org.eclipse.hawkbit.throttle.Config.Policy;
import org.eclipse.hawkbit.throttle.ThrottledException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * TODO: Still in implementation. Not finished. Don't use it
 * 
 * Operator-owned per-tenant throttling configuration. Caps how many of the shared db connection a single tenant may hold at once.
 * Per-tenant overrides live in the {@link #tenants} map.
 *
 * <p><strong>Configuration Examples:</strong>
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
 * hawkbit.throttle.max-pending=190   # since we block thread (20 sec) we reject if threads become too many.
 *                                    # over all tenants, so capacity + max-pending stays below the container's limit
 * hawkbit.throttle.threshold=7       # fairness kicks in at 7/10 pool utilization
 * }</pre>
 */
@Slf4j
@Data
@ConfigurationProperties("hawkbit.throttle") // real prefix is "hawkbit.throttle" but the rest comes from the single value - throttle map
public class ThrottleProperties {

    /**
     * Master switch. Default OFF ⇒ no DataSource wrapping, no filter, byte-for-byte current behavior.
     *
     * <p>Default: {@code false}, disabled
     */
    private boolean enabled = false;

    /**
     * Total permits. {@code -1} = auto-detect the Hikari {@code maximumPoolSize}; set explicitly only when auto-detect is not possible.
     *
     * <p>Default: {@code -1}, auto-detect
     */
    private int capacity = -1;

    /**
     * Global per-tenant ceiling on concurrent connection borrows.
     *
     * <p>Default: {@code -1}, no limit
     */
    private int limit = -1;

    /**
     * Max wait for a permit before {@link ThrottledException} (→ HTTP 429).
     * {@code 0} fast-rejects instead of blocking; a positive value is wait-then-served backpressure.
     *
     * <p>Default: 0, fast-reject
     */
    private Duration timeout = Duration.ZERO;

    /**
     * Max total pending requests, {@code -1} = no limit, limit it when no virtual threads. {@code 0} = no pending.
     *
     * <p>Default: {@code -1}, no limit
     */
    private int maxPending = -1;

    /**
     * Contention threshold (absolute slot count). When global in-use reaches this threshold, fairness (per key/tenant) enforcement activates:
     * each tenant is served in round-robin, capped by its limit. Below the threshold, any tenant may burst freely up to pool capacity.
     * {@code <= 0} = no threshold (always enforce hard cap on tenant limit).
     *
     * <p>Default: {@code -1}, hard cap mode
     */
    private int threshold = -1;

    /**
     * Capacity of the high priority work, of any key: internal, non-tenant work ({@code null} key — migrations and health checks run
     * without a tenant context), and the schedulers running their per tenant work with
     * {@link org.eclipse.hawkbit.throttle.Throttle#withHighPriority(java.util.function.Supplier) high priority}.
     *
     * <p>High priority work is served ahead of the queue for these slots, beyond them it waits for its turn — as one more key in the
     * round-robin. It is not counted to, nor waits for, the tenant limit, and it waits by the global {@link #timeout}, not the tenant
     * one. Costs no reserved capacity — tenants use those slots freely whenever high priority work is not.
     * {@code 0} disables serving ahead - high priority work then only takes its turn, still not bound by the tenant limit. Must resolve to fewer
     * slots than {@link #capacity}, or tenant work may never be reached; the policy warns if it does not.
     *
     * <p>Default: {@code 4}, high priority work is served ahead of the queue for 4 slots
     */
    private int highPriorityGranted = 4;

    /** Per-tenant limit overrides */
    private final LinkedCaseInsensitiveMap<TenantConfig> tenants = new LinkedCaseInsensitiveMap<>(Locale.ROOT);

    @Data
    public static class TenantConfig {

        private int limit = -1;
        private Duration timeout;
        private int maxPending = -1;
    }

    public Config toConfig(final int capacity) {
        if (highPriorityGranted >= capacity) {
            log.warn("""
                            Throttle high priority granted ({}) is not below capacity ({}): priority admissions can consume the whole \
                            resource, so tenant work may never be served. Lower `hawkbit.throttle.high-priority-granted`.""",
                    highPriorityGranted, capacity);
        }
        // pre init system and per tenant policy overrides in order to do not build for avery key (tenant)
        final Policy systemPolicy = Policy.builder()
                .permits(capacity)
                .timeout(timeout)
                .pendingRemoveStrategy(FIFO)
                .highPriority(true) // to use high priority granted
                .build();
        final Policy defaultTenantPolicy = Policy.builder() // policy if not explicitly overridden
                .permits(limit < 0 ? capacity : limit)
                .timeout(timeout)
                .pendingRemoveStrategy(FIFO)
                .build();
        final Map<String, Policy> tenantToPolicy = tenants.entrySet().stream() // overrides
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    final TenantConfig tenantConfig = e.getValue();
                    return Policy.builder()
                            .permits(tenantConfig.limit == -1 ? capacity : tenantConfig.limit)
                            .timeout(tenantConfig.timeout == null ? timeout : tenantConfig.timeout)
                            .maxPending(tenantConfig.maxPending)
                            .pendingRemoveStrategy(FIFO)
                            .build();
                }, (policy, duplicate) -> policy, () -> new LinkedCaseInsensitiveMap<>(Locale.ROOT))); // case-insensitive map
        return Config.builder()
                .highPriorityGranted(highPriorityGranted)
                .permits(capacity)
                .timeout(timeout)
                .maxPending(maxPending)
                // we want to remove the first pending - best chance to have been already expired on caller
                .pendingRemoveStrategy(FIFO)
                .keyPolicyThreshold(Math.min(threshold, capacity))
                // tenant null <=> system
                .keyPolicyFn(tenant -> tenant == null ? systemPolicy : tenantToPolicy.getOrDefault(tenant, defaultTenantPolicy))
                .build();
    }
}
