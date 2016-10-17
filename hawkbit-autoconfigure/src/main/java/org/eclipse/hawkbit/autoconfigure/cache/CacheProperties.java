package org.eclipse.hawkbit.autoconfigure.cache;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for configuring a cluster.
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
        return MILLISECONDS;
    }
}
