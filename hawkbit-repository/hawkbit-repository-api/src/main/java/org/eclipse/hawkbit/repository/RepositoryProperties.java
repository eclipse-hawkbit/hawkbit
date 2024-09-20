/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import lombok.Data;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configuration properties for the repository.
 */
@Data
@ConfigurationProperties("hawkbit.server.repository")
public class RepositoryProperties {

    /**
     * Set to <code>true</code> if the repository has to reject
     * {@link ActionStatus} entries for actions that are closed. This is
     * especially useful if the action status feedback channel order from the
     * device cannot be guaranteed.
     *
     * Note: if this is enforced you have to make sure that the feedback channel
     * from the devices is in order.
     */
    private boolean rejectActionStatusForClosedAction;

    /**
     * Set to <code>true</code> if the repository should publish
     * {@link TargetPollEvent}s in case a target connects to the repository.
     * Activated by default but may be worth to disable if not needed.
     */
    private boolean publishTargetPollEvent = true;

    /**
     * Maximum number of poll operations queued before flush.
     */
    private int pollPersistenceQueueSize = 10_000;

    /**
     * Maximum time before queue is flushed in {@link TimeUnit#MILLISECONDS}.
     */
    private long pollPersistenceFlushTime = TimeUnit.SECONDS.toMillis(10);

    /**
     * Set to true to persist polls immediately.
     */
    private boolean eagerPollPersistence;

    /**
     * If an {@link org.eclipse.hawkbit.repository.model.Action} has a weight of null this value is used as weight.
     */
    private int actionWeightIfAbsent = 1000;

    /**
     * Defines a timeout for the lock during invalidation of distribution sets
     * (in seconds).
     */
    private long dsInvalidationLockTimeout = 5;

    private boolean implicitTenantCreateAllowed;

    private List<String> skipImplicitLockForTags =
            List.of("skip-implicit-lock", "skip_implicit_lock", "SKIP_IMPLICIT_LOCK", "SKIP-IMPLICIT-LOCK");

    /**
     * The minimum period (in milli-seconds) on which dynamic rollouts should make attempt to involve
     * new targets
     */
    private long dynamicRolloutsMinInvolvePeriodMS = 60_000;
}