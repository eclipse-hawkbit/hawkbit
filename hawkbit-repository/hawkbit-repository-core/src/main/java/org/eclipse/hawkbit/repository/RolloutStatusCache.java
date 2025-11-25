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

import static org.eclipse.hawkbit.context.System.asSystemAsTenant;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutGroupDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutStoppedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.AbstractActionEvent;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager;
import org.springframework.cache.Cache;
import org.springframework.context.event.EventListener;

/**
 * Internal cache for Rollout status.
 */
public class RolloutStatusCache {

    private static final String CACHE_RO_NAME = "RolloutStatus";
    private static final String CACHE_GR_NAME = "RolloutGroupStatus";

    private static final TenantAwareCacheManager CACHE_MANAGER = TenantAwareCacheManager.getInstance();

    /**
     * Retrieves cached list of {@link TotalTargetCountActionStatus} of {@link Rollout}s.
     *
     * @param rollouts rolloutIds to retrieve cache entries for
     * @return map of cached entries
     */
    public static Map<Long, List<TotalTargetCountActionStatus>> getRolloutStatus(final List<Long> rollouts) {
        return retrieveFromCache(rollouts, getRolloutStatusCache());
    }

    /**
     * Retrieves cached list of {@link TotalTargetCountActionStatus} of {@link Rollout}s.
     *
     * @param rolloutId to retrieve cache entries for
     * @return map of cached entries
     */
    public static List<TotalTargetCountActionStatus> getRolloutStatus(final Long rolloutId) {
        return retrieveFromCache(rolloutId, getRolloutStatusCache());
    }

    /**
     * Retrieves cached list of {@link TotalTargetCountActionStatus} of {@link RolloutGroup}s.
     *
     * @param rolloutGroups rolloutGroupsIds to retrieve cache entries for
     * @return map of cached entries
     */
    public static Map<Long, List<TotalTargetCountActionStatus>> getRolloutGroupStatus(final List<Long> rolloutGroups) {
        return retrieveFromCache(rolloutGroups, getGroupStatusCache());
    }

    /**
     * Retrieves cached list of {@link TotalTargetCountActionStatus} of {@link RolloutGroup}.
     *
     * @param groupId to retrieve cache entries for
     * @return map of cached entries
     */
    public static List<TotalTargetCountActionStatus> getRolloutGroupStatus(final Long groupId) {
        return retrieveFromCache(groupId, getGroupStatusCache());
    }

    /**
     * Put map of {@link TotalTargetCountActionStatus} for multiple
     * {@link Rollout}s into cache.
     *
     * @param put map of cached entries
     */
    public static void putRolloutStatus(final Map<Long, List<TotalTargetCountActionStatus>> put) {
        putIntoCache(put, getRolloutStatusCache());
    }

    /**
     * Put {@link TotalTargetCountActionStatus} for one {@link Rollout}s into cache.
     *
     * @param rolloutId the cache entries belong to
     * @param status list to cache
     */
    public static void putRolloutStatus(final Long rolloutId, final List<TotalTargetCountActionStatus> status) {
        putIntoCache(rolloutId, status, getRolloutStatusCache());
    }

    /**
     * Put {@link TotalTargetCountActionStatus} for multiple {@link RolloutGroup}s into cache.
     *
     * @param put map of cached entries
     */
    public static void putRolloutGroupStatus(final Map<Long, List<TotalTargetCountActionStatus>> put) {
        putIntoCache(put, getGroupStatusCache());
    }

    /**
     * Put {@link TotalTargetCountActionStatus} for multiple {@link RolloutGroup}s into cache.
     *
     * @param groupId the cache entries belong to
     * @param status list to cache
     */
    public static void putRolloutGroupStatus(final Long groupId, final List<TotalTargetCountActionStatus> status) {
        putIntoCache(groupId, status, getGroupStatusCache());
    }

    @EventListener(classes = AbstractActionEvent.class)
    public void invalidateCachedTotalTargetCountActionStatus(final AbstractActionEvent event) {
        if (event.getRolloutId() != null) {
            final Cache cache = asSystemAsTenant(event.getTenant(), () -> CACHE_MANAGER.getCache(CACHE_RO_NAME));
            cache.evict(event.getRolloutId());
        }

        if (event.getRolloutGroupId() != null) {
            final Cache cache = asSystemAsTenant(event.getTenant(), () -> CACHE_MANAGER.getCache(CACHE_GR_NAME));
            cache.evict(event.getRolloutGroupId());
        }
    }

    @EventListener(classes = RolloutDeletedEvent.class)
    public void invalidateCachedTotalTargetCountOnRolloutDelete(final RolloutDeletedEvent event) {
        final Cache cache = asSystemAsTenant(event.getTenant(), () -> CACHE_MANAGER.getCache(CACHE_RO_NAME));
        cache.evict(event.getEntityId());
    }

    @EventListener(classes = RolloutGroupDeletedEvent.class)
    public void invalidateCachedTotalTargetCountOnRolloutGroupDelete(final RolloutGroupDeletedEvent event) {
        final Cache cache = asSystemAsTenant(event.getTenant(), () -> CACHE_MANAGER.getCache(CACHE_GR_NAME));
        cache.evict(event.getEntityId());
    }

    @EventListener(classes = RolloutStoppedEvent.class)
    public void invalidateCachedTotalTargetCountOnRolloutStopped(final RolloutStoppedEvent event) {
        final Cache cache = asSystemAsTenant(event.getTenant(), () -> CACHE_MANAGER.getCache(CACHE_RO_NAME));
        cache.evict(event.getRolloutId());
        event.getRolloutGroupIds().forEach(
                groupId -> asSystemAsTenant(event.getTenant(), () -> CACHE_MANAGER.getCache(CACHE_GR_NAME)).evict(groupId));
    }

    private static @NotNull Map<Long, List<TotalTargetCountActionStatus>> retrieveFromCache(final List<Long> ids, @NotNull final Cache cache) {
        return ids.stream()
                .map(id -> cache.get(id, CachedTotalTargetCountActionStatus.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CachedTotalTargetCountActionStatus::id, CachedTotalTargetCountActionStatus::status));
    }

    private static List<TotalTargetCountActionStatus> retrieveFromCache(final Long id, @NotNull final Cache cache) {
        final CachedTotalTargetCountActionStatus cacheItem = cache.get(id, CachedTotalTargetCountActionStatus.class);
        if (cacheItem == null) {
            return Collections.emptyList();
        }
        return cacheItem.status();
    }

    private static void putIntoCache(final Long id, final List<TotalTargetCountActionStatus> status, @NotNull final Cache cache) {
        cache.put(id, new CachedTotalTargetCountActionStatus(id, status));
    }

    private static void putIntoCache(final Map<Long, List<TotalTargetCountActionStatus>> put, @NotNull final Cache cache) {
        put.forEach((k, v) -> cache.put(k, new CachedTotalTargetCountActionStatus(k, v)));
    }

    private static @NotNull Cache getRolloutStatusCache() {
        return Objects.requireNonNull(CACHE_MANAGER.getCache(CACHE_RO_NAME), "Cache '" + CACHE_RO_NAME + "' is null!");
    }

    private static @NotNull Cache getGroupStatusCache() {
        return Objects.requireNonNull(CACHE_MANAGER.getCache(CACHE_GR_NAME), "Cache '" + CACHE_RO_NAME + "' is null!");
    }

    private record CachedTotalTargetCountActionStatus(long id, List<TotalTargetCountActionStatus> status) {}
}