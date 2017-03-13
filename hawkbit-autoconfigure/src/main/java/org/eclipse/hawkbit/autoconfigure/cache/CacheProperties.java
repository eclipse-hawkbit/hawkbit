/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for configuring the cache within a cluster. The TTL (time to live)
 * is used for the lifetime limit of data in caches. After lifetime the data
 * gets reloaded out of the database.
 */
@ConfigurationProperties("hawkbit.cache.global")
public class CacheProperties {

    /**
     * TTL for cached entries in millis.
     */
    private int ttl;

    /**
     * Initial delay in millis
     */
    private int initialDelay;

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(final int initialDelay) {
        this.initialDelay = initialDelay;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(final int ttl) {
        this.ttl = ttl;
    }

    public final TimeUnit getTtlUnit() {
        return TimeUnit.MILLISECONDS;
    }
}
