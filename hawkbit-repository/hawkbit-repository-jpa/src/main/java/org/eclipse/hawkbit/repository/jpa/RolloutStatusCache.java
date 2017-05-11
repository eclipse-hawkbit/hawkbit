/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.cache.TenancyCacheManager;
import org.eclipse.hawkbit.cache.TenantAwareCacheManager;
import org.eclipse.hawkbit.repository.event.remote.entity.AbstractActionEvent;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.event.EventListener;

/**
 * Internal cache for Rollout status.
 *
 */
public class RolloutStatusCache {
    private static final String CACHE_RO_NAME = "RolloutStatus";
    private static final String CACHE_GR_NAME = "RolloutGroupStatus";
    private final TenancyCacheManager cacheManager;
    private final TenantAware tenantAware;

    RolloutStatusCache(final TenantAware tenantAware) {
        this.tenantAware = tenantAware;
        this.cacheManager = new TenantAwareCacheManager(new CaffeineCacheManager(), tenantAware);
    }

    Map<Long, List<TotalTargetCountActionStatus>> getRolloutStatus(final List<Long> rollouts) {
        final Cache cache = cacheManager.getCache(CACHE_RO_NAME);

        return retrieveFromCache(rollouts, cache);
    }

    List<TotalTargetCountActionStatus> getRolloutStatus(final Long rolloutId) {
        final Cache cache = cacheManager.getCache(CACHE_RO_NAME);

        return retrieveFromCache(rolloutId, cache);
    }

    Map<Long, List<TotalTargetCountActionStatus>> getRolloutGroupStatus(final List<Long> rolloutGroups) {
        final Cache cache = cacheManager.getCache(CACHE_GR_NAME);

        return retrieveFromCache(rolloutGroups, cache);
    }

    List<TotalTargetCountActionStatus> getRolloutGroupStatus(final Long groupId) {
        final Cache cache = cacheManager.getCache(CACHE_GR_NAME);

        return retrieveFromCache(groupId, cache);
    }

    private Map<Long, List<TotalTargetCountActionStatus>> retrieveFromCache(final List<Long> ids, final Cache cache) {
        return ids.stream().map(rolloutId -> cache.get(rolloutId, CachedTotalTargetCountActionStatus.class))
                .filter(Objects::nonNull).collect(Collectors.toMap(CachedTotalTargetCountActionStatus::getRolloutId,
                        CachedTotalTargetCountActionStatus::getStatus));
    }

    private List<TotalTargetCountActionStatus> retrieveFromCache(final Long id, final Cache cache) {
        final CachedTotalTargetCountActionStatus cacheItem = cache.get(id, CachedTotalTargetCountActionStatus.class);

        if (cacheItem == null) {
            return null;
        }

        return cacheItem.getStatus();
    }

    void putRolloutStatus(final Map<Long, List<TotalTargetCountActionStatus>> put) {
        final Cache cache = cacheManager.getCache(CACHE_RO_NAME);
        putIntoCache(put, cache);
    }

    void putRolloutStatus(final Long rolloutId, final List<TotalTargetCountActionStatus> status) {
        final Cache cache = cacheManager.getCache(CACHE_RO_NAME);
        putIntoCache(rolloutId, status, cache);
    }

    void putRolloutGroupStatus(final Map<Long, List<TotalTargetCountActionStatus>> put) {
        final Cache cache = cacheManager.getCache(CACHE_GR_NAME);
        putIntoCache(put, cache);
    }

    void putRolloutGroupStatus(final Long groupId, final List<TotalTargetCountActionStatus> status) {
        final Cache cache = cacheManager.getCache(CACHE_GR_NAME);
        putIntoCache(groupId, status, cache);
    }

    private void putIntoCache(final Long rolloutId, final List<TotalTargetCountActionStatus> status,
            final Cache cache) {
        cache.put(rolloutId, new CachedTotalTargetCountActionStatus(rolloutId, status));
    }

    private void putIntoCache(final Map<Long, List<TotalTargetCountActionStatus>> put, final Cache cache) {
        put.entrySet().forEach(entry -> cache.put(entry.getKey(),
                new CachedTotalTargetCountActionStatus(entry.getKey(), entry.getValue())));
    }

    @EventListener(classes = AbstractActionEvent.class)
    private void invalidateCachedTotalTargetCountActionStatus(final AbstractActionEvent event) {
        if (event.getRolloutId() == null) {
            return;
        }

        Cache cache = tenantAware.runAsTenant(event.getTenant(), () -> cacheManager.getCache(CACHE_RO_NAME));
        cache.evict(event.getRolloutId());

        if (event.getRolloutGroupId() == null) {
            return;
        }

        cache = tenantAware.runAsTenant(event.getTenant(), () -> cacheManager.getCache(CACHE_GR_NAME));
        cache.evict(event.getRolloutId());
    }

    private static class CachedTotalTargetCountActionStatus {
        private final long rolloutId;
        private final List<TotalTargetCountActionStatus> status;

        private CachedTotalTargetCountActionStatus(final long rolloutId,
                final List<TotalTargetCountActionStatus> status) {
            this.rolloutId = rolloutId;
            this.status = status;
        }

        public long getRolloutId() {
            return rolloutId;
        }

        public List<TotalTargetCountActionStatus> getStatus() {
            return status;
        }
    }
}
