/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * A cache interface which handles multi tenancy.
 */
public interface TenancyCacheManager extends CacheManager {

    /**
     * A direct access for retrieving the cache without including the current
     * tenant key. This is necessary e.g. for retrieving caches not for the
     * current tenant.
     *
     * @param name
     *            the name of the cache to retrieve directly
     * @return the cache associated with the name without tenancy separation
     */
    Cache getDirectCache(String name);

    /**
     * Evicts all caches for a given tenant. All caches under a certain tenant
     * gets evicted.
     *
     * @param tenant
     *            the tenant to evict caches
     */
    void evictCaches(final String tenant);
}
