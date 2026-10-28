/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.auth;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.context.AccessContext;

/**
 * Tracks the permissions that are evaluated and granted in the current thread, e.g. during a REST request.
 * <p/>
 * Tracking is off unless explicitly started (see {@link #start()}). Then all permission checks (the {@code @PreAuthorize}
 * {@code hasPermission} / {@code hasAuthority} checks and the programmatic {@link SpPermission#hasPermission(String)} checks) that
 * grant access {@link #record(String) record} the permission. Checks done as system code (e.g. in {@link AccessContext#asSystem})
 * are not recorded since they don't depend on the user's permissions.
 * <p/>
 * It is used to verify that the documented required permissions of an endpoint match the actually checked ones.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PermissionTracker {

    /**
     * Enables the permission tracking (off by default). If disabled, the authority checks are not even decorated for tracking.
     */
    public static final String ENABLED_PROPERTY = "hawkbit.server.mgmt.permission-tracking.enabled";

    private static final ThreadLocal<Set<String>> GRANTED = new ThreadLocal<>();

    // set on the first start() - until then (i.e. always if tracking is disabled) record() returns immediately, skipping the thread local lookup
    private static volatile boolean enabled = false;

    /**
     * Starts tracking in the current thread. Must be followed by {@link #stop()} (in a finally block).
     */
    public static void start() {
        enabled = true;
        GRANTED.set(new LinkedHashSet<>());
    }

    /**
     * Stops tracking in the current thread.
     *
     * @return the permissions granted since {@link #start()}, empty if tracking wasn't started
     */
    public static Set<String> stop() {
        final Set<String> granted = GRANTED.get();
        GRANTED.remove();
        return granted == null ? Collections.emptySet() : Collections.unmodifiableSet(granted);
    }

    /**
     * Records a granted permission if tracking is started in the current thread and it is not run as system code.
     *
     * @param permission the granted permission
     */
    public static void record(final String permission) {
        if (!enabled) {
            return;
        }
        final Set<String> granted = GRANTED.get();
        if (granted != null && !AccessContext.isCurrentThreadSystemCode()) {
            granted.add(permission);
        }
    }
}
