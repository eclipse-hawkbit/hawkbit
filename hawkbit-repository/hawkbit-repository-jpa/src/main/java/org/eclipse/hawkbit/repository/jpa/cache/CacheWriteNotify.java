/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.cache;

import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * An service which combines the functionality for functional use cases to write
 * into the cache an notify the writing to the cache to the event bus.
 */
@Service
public class CacheWriteNotify {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Writes the {@link CacheKeys#ROLLOUT_GROUP_CREATED} and
     * {@link CacheKeys#ROLLOUT_GROUP_TOTAL} into the cache and notfies the
     * eventPublisher with a {@link RolloutGroupCreatedEvent}.
     *
     * @param rolloutId
     *            the ID of the rollout the group has been created
     * @param rolloutGroup
     *            rollout group which has been created
     * @param totalRolloutGroup
     *            the total number of rollout groups for this rollout
     * @param createdRolloutGroup
     *            the number of already created groups of the rollout
     */
    public void rolloutGroupCreated(final Long rolloutId, final RolloutGroup rolloutGroup, final int totalRolloutGroup,
            final int createdRolloutGroup) {

        final Cache cache = cacheManager.getCache(JpaRollout.class.getName());
        final String cacheKeyGroupTotal = CacheKeys.entitySpecificCacheKey(String.valueOf(rolloutId),
                CacheKeys.ROLLOUT_GROUP_TOTAL);
        final String cacheKeyGroupCreated = CacheKeys.entitySpecificCacheKey(String.valueOf(rolloutId),
                CacheKeys.ROLLOUT_GROUP_CREATED);
        if (createdRolloutGroup < totalRolloutGroup) {
            cache.put(cacheKeyGroupTotal, totalRolloutGroup);
            cache.put(cacheKeyGroupCreated, createdRolloutGroup);
        } else {
            // in case we reached progress 100 delete the cache value again
            // because otherwise he will keep there forever
            cache.evict(cacheKeyGroupTotal);
            cache.evict(cacheKeyGroupCreated);
        }

        eventPublisher.publishEvent(new RolloutGroupCreatedEvent(rolloutGroup, applicationContext.getId()));
    }

}
