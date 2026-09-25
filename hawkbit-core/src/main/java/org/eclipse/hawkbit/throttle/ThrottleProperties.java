/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.throttle.Throttle.Policy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * Operator-owned per-tenant throttling configuration.  Caps how many of the shared object per domain (e.g. DB connection pool's slots)
 * a single tenant may hold at once. Per-tenant overrides live in the {@link ThrottleConfig#tenants} map.
 *
 * <p><strong>Configuration Examples:</strong>
 * <pre>{@code
 * # Mode 1: Disabled (default)
 * hawkbit.throttle.db.enabled=false
 *
 * # Mode 2: Hard cap, fast-reject
 * hawkbit.throttle.db.enabled=true
 * hawkbit.throttle.db.limit=5           # each tenant max 5 connections
 * hawkbit.throttle.db.timeout=0         # reject immediately when over limit
 * hawkbit.throttle.db.threshold=-1      # no threshold (always enforce limit)
 *
 * # Mode 3: Contention-aware burst allowance
 * hawkbit.throttle.db.enabled=true
 * hawkbit.throttle.db.limit=10          # ceiling per tenant
 * hawkbit.throttle.db.timeout=20s       # wait up to 20s before HTTP 429
 * hawkbit.throttle.db.threshold=8       # fairness kicks in at 8/10 pool utilization
 * spring.threads.virtual.enabled=true   # consider it for timeout>0
 * }</pre>
 */
@Data
@ConfigurationProperties("hawkbit") // real prefix is "hawkbit.throttle" but the rest comes from the single value - throttle map
public class ThrottleProperties {

    // throttle config domain to throttle config mapping
    private Map<String, ThrottleConfig> throttle = new HashMap<>();

    @Data
    public static class ThrottleConfig {

        /**
         * Master switch. Default OFF ⇒ no DataSource wrapping, no filter, byte-for-byte current behavior.
         */
        private boolean enabled = false;

        /**
         * Total permits. {@code -1} or {@code 0} = auto-detect the Hikari {@code maximumPoolSize}; set explicitly only
         * when auto-detect is not possible.
         *
         * <p>Because every borrow takes a permit — tenant, system and nested alike — permits map one-to-one onto
         * pooled connections, so this should equal the pool size. No headroom is needed: reentrant requests that would
         * deadlock are refused rather than parked.
         */
        private int capacity = -1;

        /**
         * Contention threshold (absolute slot count). When global in-use reaches this threshold, fairness
         * enforcement activates: each tenant is served in round-robin, capped by its limit.
         * Below the threshold, any tenant may burst freely up to pool capacity.
         * {@code -1} = no threshold (always enforce hard cap on tenant limit, 0 or any < -1 is essentially the same).
         * Default: -1 (hard cap mode).
         */
        private int threshold = -1;

        /**
         * Global per-tenant ceiling on concurrent connection borrows. {@code -1} = no ceiling.
         */
        private int limit = -1;

        /**
         * Capacity that internal, non-tenant work is served ahead of the queue for. Internal work
         * is charged to the {@code null} key — schedulers, migrations and health checks run without a tenant context.
         *
         * <p>A floor it cannot be starved below, not a cap: beyond it system work competes as an ordinary key, so it
         * can neither be crowded out by tenant load nor crowd tenants out itself. Costs no reserved capacity — tenants
         * use those slots freely whenever system work is not. {@code 0} disables the priority. Must resolve to fewer
         * slots than {@link #capacity}, or tenant work may never be reached; the policy warns if it does not.
         */
        private int systemFloor = 1; // default - at least 1 slot with priority for internal work

        /**
         * Max wait for a permit before {@link ThrottledException} (→ HTTP 429). {@code 0} fast-rejects
         * instead of blocking (safe on platform threads); a positive value is wait-then-serve
         * backpressure (requires virtual threads enabled). Default: 0 (safe-first; switch to 20s after
         * virtual thread validation).
         */
        private Duration timeout = Duration.ZERO;

        /** Operator-only per-tenant limit overrides (tenant id → limit; {@code -1} = opt out). */
        private final LinkedCaseInsensitiveMap<Integer> tenants = new LinkedCaseInsensitiveMap<>(Locale.ROOT);

        /**
         * Resolve the effective limit for a specific tenant.
         *
         * @param tenant the tenant identifier
         * @return the per-tenant ceiling: the tenant override if set in {@link #tenants}, else the global
         *         {@link #limit}. {@code -1} = unlimited (bounded only by capacity).
         */
        public int limit(final String tenant) {
            return tenants.getOrDefault(tenant, limit);
        }

        public Policy toPolicy(final int capacity) {
            return new PolicyImpl(capacity, getThreshold(), this::limit, Math.clamp(systemFloor, 0, capacity));
        }
    }

    /**
     * Default {@link Policy}: per-key ceilings from a lookup function, and a priority floor granted to internal, non-tenant work —
     * the {@code null} key.
     *
     * @param capacity total permits (should equal the real size of the guarded resource)
     * @param burstThreshold in-use count at which fair-share enforcement activates; {@code -1} to always enforce
     * @param ceilings key → ceiling on concurrently held permits; {@code -1} = unlimited
     * @param systemFloor permits the {@code null} key is served ahead of the queue for; {@code 0} = no priority
     */
    @Slf4j
    private record PolicyImpl(int capacity, int burstThreshold, ToIntFunction<String> ceilings, int systemFloor) implements Policy {

        public PolicyImpl {
            // Priority-floor admissions are served before the fair-share pass, so if the granted floor can cover the whole
            // resource there is no arrival order in which a non-priority key is reached. Warn rather than reject: the
            // operator may be running a deployment that is genuinely internal-work-only.
            if (systemFloor >= capacity) {
                log.warn("""
                                Throttle system floor ({}) is not below capacity ({}): priority admissions can consume the whole \
                                resource, so tenant work may never be served. Lower hawkbit.throttle.<resource>.system-floor.""",
                        systemFloor, capacity);
            }
        }

        @Override
        public int ceiling(final String key) {
            return ceilings.applyAsInt(key);
        }

        @Override
        public int priorityFloor(final String key) {
            return key == null ? systemFloor : 0;
        }
    }
}
