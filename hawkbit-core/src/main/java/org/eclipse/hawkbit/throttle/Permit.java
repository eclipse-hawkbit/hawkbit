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

/**
 * A granted throttle slot. Closing it releases the slot back to the {@link TenantThrottle}
 * (decrementing the global and per-tenant in-use counters and waking the next eligible waiter).
 * Instances are AutoCloseable-style but do not extend it to avoid exception signature requirements.
 */
@FunctionalInterface
public interface Permit {

    /**
     * Release this permit back to the throttle. Idempotent — multiple calls are safe but only the
     * first has effect. Typically called automatically by {@link ThrottledConnection#close()}.
     */
    void close();
}
