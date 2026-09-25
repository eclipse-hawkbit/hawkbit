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
import static org.eclipse.hawkbit.throttle.Config.Priority.HIGH;
import static org.eclipse.hawkbit.throttle.Config.Priority.NORMAL;

import java.time.Duration;
import java.util.EnumMap;
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
     * Capacity that internal, non-tenant work is served ahead of the queue for ({@link Config.Priority#HIGH}.
     * Internal work is charged to the {@code null} key — schedulers, migrations and health checks run without a tenant context.
     *
     * <p>A floor it cannot be starved below, not a cap: beyond it system work competes as an ordinary key, so it
     * can neither be crowded out by tenant load nor crowd tenants out itself. Costs no reserved capacity — tenants
     * use those slots freely whenever system work is not. {@code 0} disables the priority. Must resolve to fewer
     * slots than {@link #capacity}, or tenant work may never be reached; the policy warns if it does not.
     *
     * <p>Default: {@code 4}, system work is served ahead of the queue for 4 slots
     */
    private int systemGranted = 4;

    /** Per-tenant limit overrides */
    private final LinkedCaseInsensitiveMap<TenantConfig> tenants = new LinkedCaseInsensitiveMap<>(Locale.ROOT);

    @Data
    public static class TenantConfig {

        private int limit = -1;
        private Duration timeout;
        private int maxPending = -1;
    }

    public Config toConfig(final int capacity) {
        if (systemGranted >= capacity) {
            log.warn("""
                            Throttle system granted ({}) is not below capacity ({}): priority admissions can consume the whole \
                            resource, so tenant work may never be served. Lower `hawkbit.throttle.system-granted`.""",
                    systemGranted, capacity);
        }
        // pre init system and per tenant policy overrides in order to do not build for avery key (tenant)
        final Policy systemPolicy = Policy.builder()
                .permits(capacity)
                .timeout(timeout)
                .pendingRemoveStrategy(FIFO)
                .priority(HIGH) // set high priority to use high priority granted
                .build();
        final Policy defaultTenantPolicy = Policy.builder() // policy if not explicitly overridden
                .permits(limit < 0 ? capacity : limit)
                .timeout(timeout)
                .pendingRemoveStrategy(FIFO)
                .priority(NORMAL)
                .build();
        final Map<String, Policy> tenantToPolicy = tenants.entrySet().stream() // overrides
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    final TenantConfig tenantConfig = e.getValue();
                    return Policy.builder()
                            .permits(tenantConfig.limit == -1 ? capacity : tenantConfig.limit)
                            .timeout(tenantConfig.timeout == null ? timeout : tenantConfig.timeout)
                            .maxPending(tenantConfig.maxPending)
                            .pendingRemoveStrategy(FIFO)
                            .priority(NORMAL)
                            .build();
                }, (policy, duplicate) -> policy, () -> new LinkedCaseInsensitiveMap<>(Locale.ROOT))); // case-insensitive map
        return Config.builder()
                .priorityToGranted(new EnumMap<>(Map.of(HIGH, systemGranted))) // reserve system priority granted
                .permits(capacity)
                .timeout(timeout)
                .maxPending(maxPending)
                // we want to remove the first pending - best chance to have been already expired on caller
                .pendingRemoveStrategy(FIFO)
                .keyPolicyThreshold(Math.min(threshold, capacity))
                // tenant null <=> system. It still could be a tenant work, but we still don't know it
                .keyPolicyFn(tenant -> tenant == null ? systemPolicy : tenantToPolicy.getOrDefault(tenant, defaultTenantPolicy))
                .build();
    }
}
