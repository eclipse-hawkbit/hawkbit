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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Data;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
     * Maximum number of targets with buffered (not yet persisted) polls. A target polling again before the flush doesn't take
     * another entry. If full, the poll time is written directly (skipped if the target is locked).
     */
    private int pollPersistenceQueueSize = 10_000;

    /**
     * Fill level of the poll queue, in percent (1-100) of {@link #pollPersistenceQueueSize}, at which a flush is triggered
     * in addition to the periodic one (see {@link #pollPersistenceFlushTime}). Only targets newly added since the last flush are
     * counted.
     */
    private int pollPersistenceFlushThresholdPercent = 50;

    /**
     * Maximum time before queue is flushed in {@link TimeUnit#MILLISECONDS}.
     */
    private long pollPersistenceFlushTime = TimeUnit.SECONDS.toMillis(10);

    /**
     * Maximum time in {@link TimeUnit#MILLISECONDS} a queued poll is kept for the next flush if it couldn't be persisted
     * (e.g. the target was locked). Older polls are dropped.
     */
    private long pollPersistenceMaxQueueTime = TimeUnit.MINUTES.toMillis(5);

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

    /**
     * Default delay after which a tenant soft deleted via {@link SystemManagement#softDeleteTenant(String)} becomes soft
     * deleted (inaccessible), counted from the delete request time. Until then the tenant stays fully usable. If not positive the
     * tenant becomes soft deleted immediately.
     */
    private Duration tenantSoftDeleteAfter = Duration.ofDays(7);

    /**
     * Delay after which a tenant soft deleted via {@link SystemManagement#softDeleteTenant(String)} is hard deleted (with all
     * its data), counted from the delete request time (not from the soft delete time). Until then the tenant could be restored.
     * If not positive soft deletes are disabled. If positive, it must not be less than {@link #tenantSoftDeleteAfter} - otherwise
     * the application fails to start.
     */
    private Duration tenantHardDeleteAfter = Duration.ofDays(30);
}
