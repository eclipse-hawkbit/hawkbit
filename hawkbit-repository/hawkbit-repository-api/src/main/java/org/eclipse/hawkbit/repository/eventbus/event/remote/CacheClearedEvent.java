/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event.remote;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CacheClearedEvent extends AbstractDistributedEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private String cacheName;

    /**
     * Constructor.
     *
     * @param tenant
     *            the tenant for this event
     * @param cacheName
     *            the name of the cache which was cleared
     */
    // @JsonCreator
    protected CacheClearedEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("cacheName") final String cacheName) {
        this(tenant, cacheName, null);
    }

    /**
     * Constructor.
     *
     * @param tenant
     *            the tenant for this event
     * @param cacheName
     *            the name of the cache which was cleared
     */
    @JsonCreator
    protected CacheClearedEvent() {
        super(new Object(), null, null);
    }

    /**
     * Constructor.
     *
     * @param tenant
     *            the tenant for this event
     * @param cacheName
     *            the name of the cache which was cleared
     */
    public CacheClearedEvent(final String tenant, @NotNull final String cacheName, final String originService) {
        super(cacheName, tenant, originService);
        this.cacheName = cacheName;
    }

    @JsonIgnore
    public String getCacheName() {
        return cacheName;
    }
}
