/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the repository.
 *
 */
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
     * If an {@link Action} has a weight of null this value is used as weight.
     */
    private int actionWeightIfAbsent = 1000;

    /**
     * Defines a timeout for the lock during invalidation of distribution sets
     * (in seconds).
     */
    private long dsInvalidationLockTimeout = 5;

    public boolean isEagerPollPersistence() {
        return eagerPollPersistence;
    }

    public void setEagerPollPersistence(final boolean eagerPollPersistence) {
        this.eagerPollPersistence = eagerPollPersistence;
    }

    public long getPollPersistenceFlushTime() {
        return pollPersistenceFlushTime;
    }

    public void setPollPersistenceFlushTime(final long pollPersistenceFlushTime) {
        this.pollPersistenceFlushTime = pollPersistenceFlushTime;
    }

    public int getPollPersistenceQueueSize() {
        return pollPersistenceQueueSize;
    }

    public void setPollPersistenceQueueSize(final int pollPersistenceQueueSize) {
        this.pollPersistenceQueueSize = pollPersistenceQueueSize;
    }

    public boolean isRejectActionStatusForClosedAction() {
        return rejectActionStatusForClosedAction;
    }

    public void setRejectActionStatusForClosedAction(final boolean rejectActionStatusForClosedAction) {
        this.rejectActionStatusForClosedAction = rejectActionStatusForClosedAction;
    }

    public boolean isPublishTargetPollEvent() {
        return publishTargetPollEvent;
    }

    public void setPublishTargetPollEvent(final boolean publishTargetPollEvent) {
        this.publishTargetPollEvent = publishTargetPollEvent;
    }

    public int getActionWeightIfAbsent() {
        return actionWeightIfAbsent;
    }

    public void setActionWeightIfAbsent(final int actionWeightIfAbsent) {
        this.actionWeightIfAbsent = actionWeightIfAbsent;
    }

    public long getDsInvalidationLockTimeout() {
        return dsInvalidationLockTimeout;
    }

    public void setDsInvalidationLockTimeout(final long dsInvalidationLockTimeout) {
        this.dsInvalidationLockTimeout = dsInvalidationLockTimeout;
    }

}
