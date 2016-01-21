/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

import org.eclipse.hawkbit.eventbus.event.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;

/**
 * An service which combines the functionality for functional use cases to write
 * into the cache an notify the writing to the cache to the {@link EventBus}.
 *
 *
 *
 */
@Service
public class CacheWriteNotify {

    /**
    *
    */
    private static final int DOWNLOAD_PROGRESS_MAX = 100;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private TenantAware tenantAware;

    /**
     * writes the download progress in percentage into the cache
     * {@link CacheKeys#DOWNLOAD_PROGRESS_PERCENT} and notifies the
     * {@link EventBus} with a {@link DownloadProgressEvent}.
     * 
     * @param statusId
     *            the ID of the {@link ActionStatus}
     * @param progressPercent
     *            the progress in percentage which must be between 0-100
     */
    public void downloadProgressPercent(final long statusId, final int progressPercent) {

        final Cache cache = cacheManager.getCache(Action.class.getName());
        final String cacheKey = CacheKeys.entitySpecificCacheKey(String.valueOf(statusId),
                CacheKeys.DOWNLOAD_PROGRESS_PERCENT);
        if (progressPercent < DOWNLOAD_PROGRESS_MAX) {
            cache.put(cacheKey, progressPercent);
        } else {
            // in case we reached progress 100 delete the cache value again
            // because otherwise he will
            // keep there forever
            cache.evict(cacheKey);
        }

        eventBus.post(new DownloadProgressEvent(tenantAware.getCurrentTenant(), statusId, progressPercent));
    }

    /**
     * @param cacheManager
     *            the cacheManager to set
     */
    void setCacheManager(final CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * @param eventBus
     *            the eventBus to set
     */
    void setEventBus(final EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * @param tenantAware
     *            the tenantAware to set
     */
    void setTenantAware(final TenantAware tenantAware) {
        this.tenantAware = tenantAware;
    }
}
