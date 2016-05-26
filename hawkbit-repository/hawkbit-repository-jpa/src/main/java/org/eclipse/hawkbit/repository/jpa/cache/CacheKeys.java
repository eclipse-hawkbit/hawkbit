/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.cache;

import org.eclipse.hawkbit.repository.jpa.eventbus.CacheFieldEntityListener;

/**
 * Constants for cache keys used in multiple classes.
 *
 *
 *
 *
 * @see CacheFieldEntityListener
 * @see CacheWriteNotify
 * @see CacheField
 *
 */
public final class CacheKeys {

    /**
     * The cache key name for the {@link UpdateActionStatus} download progress
     * event.
     */
    public static final String DOWNLOAD_PROGRESS_PERCENT = "download.progress.percent";
    public static final String ROLLOUT_GROUP_CREATED = "rollout.group.created";
    public static final String ROLLOUT_GROUP_TOTAL = "rollout.group.total";

    /**
     * utility class only private constructor.
     */
    private CacheKeys() {

    }

    /**
     * calculates the cache key for a specific entity. The cache key must be
     * different for each different identifiable entity by its ID.
     * 
     * @param entityId
     *            the ID of the entity to build the specific cache key for this
     *            entity
     * @param cacheKey
     *            the cache key for the field to be cached
     * @return the combined cache key based on the given {@code entityId} and
     *         {@code cacheKey}
     */
    public static String entitySpecificCacheKey(final String entityId, final String cacheKey) {
        return entityId + "." + cacheKey;
    }
}
