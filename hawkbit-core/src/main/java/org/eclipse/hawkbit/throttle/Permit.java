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

/**
 * A granted throttle slot. Closing it releases the slot back to the {@link Throttle}.
 *
 * <p><b>Parent and child permits.</b> Permits form a tree that models one <em>unit of work</em>:
 * <ul>
 *   <li>{@link Throttle#acquire(String, Duration)} returns a <b>root</b> permit — a new unit of work.</li>
 *   <li>{@link #acquire(Duration)} returns a <b>child</b> of this permit — more of the resource for the
 *       <em>same</em> unit of work, e.g. a nested {@code REQUIRES_NEW} transaction borrowing a second
 *       connection while the outer one is still open. The child inherits the parent's key, so the whole
 *       tree is charged to one tenant however deeply it nests.</li>
 * </ul>
 *
 * <p>The distinction is what makes the engine deadlock-aware: a child is requested by a caller that
 * <em>already holds</em> part of the resource, so making it wait can pin the resource, whereas a root
 * request holds nothing and is always safe to park. Children are therefore admitted ahead of the fair
 * share, and refused outright when waiting would leave every unit of work blocked on another.
 *
 * <p><b>Lifetimes are independent.</b> Closing a parent releases only its own slot; open children stay
 * valid and release on their own {@link #close()}. Ordering is not enforced, because a nested JDBC
 * connection may outlive the outer one. The unit of work stays counted until every permit in the tree is
 * closed, so leaking a child keeps the tree alive — close permits in a {@code finally} or
 * try-with-resources.
 */
public interface Permit extends AutoCloseable {

    /**
     * Acquire another permit for the same unit of work, as a child of this one.
     *
     * @param timeout maximum time to wait
     * @return a child {@link Permit}, charged to the same key as this one
     * @throws ThrottledException if no slot became available in time, or if waiting would deadlock every
     *         unit of work currently holding the resource
     * @throws IllegalStateException if this permit is already closed
     */
    Permit acquire(Duration timeout);

    /**
     * Returns if the permit's root (coult be the same) is stale / released
     *
     * @return if the permit is stale
     */
    boolean isRootStale();

    /**
     * Release this permit back to the throttle. Idempotent — multiple calls are safe but only the first has effect.
     * Does not affect this permit's children.
     */
    @Override
    void close();
}
