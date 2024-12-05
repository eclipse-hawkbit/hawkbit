/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.cache;

import java.util.concurrent.TimeUnit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for configuring the cache within a cluster. The TTL (time to live) is used for the lifetime limit of data in caches.
 * After lifetime the data gets reloaded out of the database.
 */
@Data
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

    public final TimeUnit getTtlUnit() {
        return TimeUnit.MILLISECONDS;
    }
}