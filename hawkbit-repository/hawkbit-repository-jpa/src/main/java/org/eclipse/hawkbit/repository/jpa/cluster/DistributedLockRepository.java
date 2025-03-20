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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for {@link JdbcLockRegistry}. This class is not thread safe. Adds support for keeping lock longer then ttl
 * if really used by the instance.
 */
@Slf4j
public class DistributedLockRepository extends DefaultLockRepository {

    private static final int MAX_DELETE_RETRY = 10;

    // period between successive refresh tics
    private static final String TIC_PERIOD_MS = "${hawkbit.repository.cluster.lock.ticPeriodMS:2000}";

    private final PlatformTransactionManager txManager;

    private final int refreshOnRemainMS;
    private final int refreshOnRemainPercent;

    // if empty refresh is effectively disabled (when both REFRESH_ON_REMAINS_MS and REFRESH_ON_REMAINS_PERCENT are non-positive)
    // otherwise, a refresh is triggered at refreshAfterMillis after lock acquisition or last refresh
    private Optional<Integer> refreshAfterMillis;
    // lock <-> next refresh time
    private final Map<String, Instant> lockToRefreshTime = new ConcurrentHashMap<>();

    /**
     * @param dataSource to use for managing the locks
     */
    public DistributedLockRepository(final DataSource dataSource, final LockProperties lockProperties, final PlatformTransactionManager txManager) {
        super(dataSource);
        this.txManager = txManager;

        this.refreshOnRemainMS = lockProperties.getRefreshOnRemainMS();
        this.refreshOnRemainPercent = lockProperties.getRefreshOnRemainPercent();

        setTimeToLive(lockProperties.getTtl());
    }

    // interceptor that handles refreshAfterMillis update when time to live is changed
    @Override
    public void setTimeToLive(final int timeToLive) {
        super.setTimeToLive(timeToLive);
        refreshAfterMillis = refreshAfterMillis(timeToLive);
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

    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    @Override
    public boolean acquire(final String lock) {
        try {
            // real acquisition (made by super.acquire) is made in a new transaction 
            // because we need to know real (after transaction commit) result Ïto know if it is really successful.
            // otherwise the super.acquire will return result before been committed and could be false positive
            final boolean acquired = DeploymentHelper.runInNewTransaction(
                txManager, "lock-acquire", Isolation.READ_COMMITTED.value(), status -> super.acquire(lock));
            if (acquired) {
                // update next refresh time
                refreshAfterMillis.ifPresent(
                    afterMillis -> lockToRefreshTime.put(lock, Instant.now().plus(afterMillis, ChronoUnit.MILLIS)));
            }
            return acquired;
        } catch (final DataIntegrityViolationException | DeadlockLoserDataAccessException e) {
            log.debug("Could not acquire cluster lock {}. I guess another node has it.", lock, e);
            return false;
        } catch (final QueryTimeoutException e) {
            log.debug("Query timed out for lock {}.", lock, e);
            throw new TransactionTimedOutException("DB query timed out for lock " + lock, e);
        }
    }

    @SuppressWarnings({"java:S1066"})
    @Scheduled(initialDelayString = TIC_PERIOD_MS, fixedDelayString = TIC_PERIOD_MS)
    public void refresh() {
        refreshAfterMillis.ifPresentOrElse(afterMillis -> {
            final Instant now = Instant.now();
            lockToRefreshTime.forEach((lock, refreshTime) -> {
                if (now.isAfter(refreshTime)) {
                    synchronized (this) {
                        // if delete is called while iterating we must skip record update
                        // otherwise, the lock will be unavailable for everyone until expiration
                        if (lockToRefreshTime.containsKey(lock)) {
                            if (!acquire(lock)) { // try to update record in lock table
                                log.warn("Failed to refresh cluster lock {}!", lock);
                            }
                        }
                    }
                }
            });
        }, lockToRefreshTime::clear);
    }

    private Optional<Integer> refreshAfterMillis(final int timeToLive) {
        final int triggerOnRemainMS = Math.max(refreshOnRemainMS, timeToLive * refreshOnRemainPercent / 100);
        final int refreshAfterMS = timeToLive - triggerOnRemainMS;
        return refreshAfterMS <= 0 ? Optional.empty() : Optional.of(refreshAfterMS);
    }
// * May be required if the super class doesn't execute in new transactions
// * See https://github.com/spring-projects/spring-integration/issues/3683
// * may be not needed anymore
//    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
//    @Override
//    public boolean isAcquired(final String lock) {
//        return super.isAcquired(lock);
//    }
//
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    @Override
//    public void close() {
//        super.close();
//    }
}