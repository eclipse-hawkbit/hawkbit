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
import java.util.Optional;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

@Builder
@Value
@Accessors(fluent = true)
public class Config {

    int highPriorityGranted; // free permits are granted to high priority first, up to that number. 0 disables serving ahead

    int permits; // capacity, max permits to be granted
    @Builder.Default
    Duration timeout = Duration.ZERO; // non-positive timeout means direct reject. In this case maxPending is ignored
    @Builder.Default
    int maxPending = -1; // max pending requests over all keys together, -1 means no limit
    @Builder.Default
    PendingRemoveStrategy pendingRemoveStrategy = PendingRemoveStrategy.FIFO; // strategy to remove pending requests when maxPending is reached
    int keyPolicyThreshold; // shall be less than permits, when lest that number permits are acquired - doesn't consider key specific permits limit
    @Builder.Default
    @ToString.Exclude
    @Getter(AccessLevel.PRIVATE)
    Function<String, Policy> keyPolicyFn = key -> null; // function to get the policy for a given key

    //--- per key ---

    public boolean highPriority(final String key) {
        return Optional.ofNullable(keyPolicyFn.apply(key)).map(Policy::highPriority).orElse(false);
    }

    public int permits(final String key) {
        final Policy keyPolicy = keyPolicyFn.apply(key);
        if (keyPolicy == null) {
            return permits;
        } else {
            final int keyPermits = keyPolicy.permits();
            return keyPermits >= 0 ? keyPermits : permits; // if key policy has permits >= 0, use it, otherwise use default permits
        }
    }

    public Duration timeout(final String key) {
        return Optional.ofNullable(keyPolicyFn.apply(key)).map(Policy::timeout).orElse(timeout);
    }

    @SuppressWarnings("java:S3358") // easy readable that way
    public int maxPending(final String key) {
        final Policy keyPolicy = keyPolicyFn.apply(key);
        return keyPolicy == null || keyPolicy.maxPending() < 0 ?
                maxPending :
                maxPending < 0 ? keyPolicy.maxPending() : Math.min(maxPending, keyPolicy.maxPending());
    }

    public PendingRemoveStrategy pendingRemoveStrategy(final String key) {
        return Optional.ofNullable(keyPolicyFn.apply(key)).map(Policy::pendingRemoveStrategy).orElse(pendingRemoveStrategy);
    }

    public enum PendingRemoveStrategy {
        LIFO, // last in first out
        FIFO // fist in first out
    }

    @Builder
    @Value
    public static class Policy {

        @Builder.Default
        int permits = -1; // capacity, max permits to be granted, <= 0 means no permits (blocked)
        Duration timeout; // non-positive timeout means direct reject. In this case maxPending is ignored
        @Builder.Default
        int maxPending = -1; // max pending requests for this key, -1 means no key bound (only Config#maxPending), 0 - no pending
        @Builder.Default
        PendingRemoveStrategy pendingRemoveStrategy = PendingRemoveStrategy.FIFO; // strategy to remove pending requests when maxPending is reached
        boolean highPriority; // if the key work is high priority, e.g. the internal work
    }
}
