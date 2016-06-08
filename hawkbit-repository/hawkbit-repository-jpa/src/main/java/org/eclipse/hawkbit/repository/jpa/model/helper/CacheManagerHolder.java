/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model.helper;

import org.eclipse.hawkbit.repository.jpa.model.CacheFieldEntityListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

/**
 * A singleton bean which holds the {@link CacheManager} to have to the cache
 * manager in beans not instantiated by spring e.g. JPA entities or
 * {@link CacheFieldEntityListener} which cannot be autowired.
 *
 *
 * 
 */
public final class CacheManagerHolder {

    private static final CacheManagerHolder SINGLETON = new CacheManagerHolder();

    @Autowired
    private CacheManager cacheManager;

    private CacheManagerHolder() {

    }

    /**
     * @return the cache manager holder singleton instance
     */
    public static CacheManagerHolder getInstance() {
        return SINGLETON;
    }

    /**
     * @return the cacheManager
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Normally not used when using spring-boot then the cachemanager is
     * autowired to the CacheManagerHolder, but for testing purposes.
     * 
     * @param cacheManager
     *            the cache manager to set for the cache manager holder.
     */
    public void setCacheManager(final CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}
