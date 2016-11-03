/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;

/**
 * A default implementation of the {@link DownloadIdCache} which uses the
 * {@link CacheManager} implementation to store the download-ids.
 */
public class DefaultDownloadIdCache implements DownloadIdCache {

    static final String DOWNLOAD_ID_CACHE = "DowonloadIdCache";

    private final CacheManager cacheManager;

    /**
     * @param cacheManager
     *            the underlying cache-manager to store the download-ids
     */
    @Autowired
    public DefaultDownloadIdCache(final CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void put(final String downloadId, final DownloadArtifactCache object) {
        getCache().put(downloadId, object);
    }

    @Override
    public DownloadArtifactCache get(final String downloadId) {
        final ValueWrapper valueWrapper = getCache().get(downloadId);
        return (valueWrapper == null) ? null : (DownloadArtifactCache) valueWrapper.get();
    }

    @Override
    public void evict(final String downloadId) {
        getCache().evict(downloadId);
    }

    private Cache getCache() {
        if (cacheManager instanceof TenancyCacheManager) {
            return ((TenancyCacheManager) cacheManager).getDirectCache(DOWNLOAD_ID_CACHE);
        }
        return cacheManager.getCache(DOWNLOAD_ID_CACHE);
    }
}
