/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.cluster;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for {@link JdbcLockRegistry}. This class is not thread safe.
 * Adds support for keeping lock longer than ttl if really used by the instance (renew).
 */
@Slf4j
@NullMarked
public class DistributedLockRepository extends DefaultLockRepository {

    private static final int MAX_DELETE_RETRY = 10;

    // period between successive refresh tics
    private static final String TIC_PERIOD_MS = "${hawkbit.repository.cluster.lock.ticPeriodMS:2000}";

    private final Duration renewTtl;

    // if null refresh is effectively disabled (when both REFRESH_ON_REMAINS_MS and REFRESH_ON_REMAINS_PERCENT are non-positive)
    // otherwise, a refresh is triggered at refreshAfterMillis after lock acquisition or last refresh
    @Nullable
    private final Integer refreshAfterMillis;
    // lock <-> next refresh time
    private final Map<String, Instant> lockToRefreshTime = new ConcurrentHashMap<>();

    public DistributedLockRepository(final DataSource dataSource, final LockProperties lockProperties) {
        super(dataSource);

        renewTtl = Duration.ofMillis(lockProperties.getTtl());

        final int timeToLive = lockProperties.getTtl();
        final int triggerOnRemainMS = Math.max(
                lockProperties.getRefreshOnRemainMS(),
                timeToLive * lockProperties.getRefreshOnRemainPercent() / 100);
        final int refreshAfterMS = timeToLive - triggerOnRemainMS;
        refreshAfterMillis = refreshAfterMS <= 0 ? null : refreshAfterMS;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean delete(final String lock) {
        synchronized (this) {
            lockToRefreshTime.remove(lock);
        }
        return delete(lock, 0);
    }

    private boolean delete(final String lock, final int count) {
        try {
            return super.delete(lock);
        } catch (final PessimisticLockingFailureException e) {
            if (count < MAX_DELETE_RETRY) {
                log.debug("Failed to delete cluster lock {}. We try again.", lock, e);
                return delete(lock, count + 1);
            } else {
                log.warn("Failed to delete cluster lock {}!", lock, e);
                return false;
            }
        }
    }

    // Spring Integration 7.0 calls acquire(String, Duration) directly; the deprecated acquire(String) is no longer invoked
    // by JdbcLockRegistry. Override the new method to populate lockToRefreshTime for the refresh mechanism.
    @Override
    public boolean acquire(final String lock, final Duration ttl) {
        final boolean acquired = super.acquire(lock, ttl);
        if (acquired) {
            Optional.ofNullable(refreshAfterMillis).ifPresent(afterMillis ->
                    lockToRefreshTime.put(lock, Instant.now().plus(afterMillis, ChronoUnit.MILLIS)));
        }
        return acquired;
    }

    @SuppressWarnings({ "java:S1066" })
    @Scheduled(initialDelayString = TIC_PERIOD_MS, fixedDelayString = TIC_PERIOD_MS)
    public void renew() {
        Optional.ofNullable(refreshAfterMillis).ifPresentOrElse(afterMillis -> {
            final Instant now = Instant.now();
            lockToRefreshTime.forEach((lock, refreshTime) -> {
                if (now.isAfter(refreshTime)) {
                    synchronized (this) {
                        // if delete is called while iterating we must skip record update
                        // otherwise, the lock will be unavailable for everyone until expiration
                        if (lockToRefreshTime.containsKey(lock)) {
                            if (!renew(lock, renewTtl)) { // try to update record in lock table
                                log.warn("Failed to renew cluster lock {}!", lock);
                            }
                        }
                    }
                }
            });
        }, lockToRefreshTime::clear);
    }
}