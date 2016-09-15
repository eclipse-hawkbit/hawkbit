/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event.remote;

import org.eclipse.hawkbit.eventbus.event.Event;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An abstract class of the {@link DistributedEvent} implementation which holds
 * all the necessary information of distributing events to other nodes.
 *
 */
public abstract class AbstractDistributedEvent extends RemoteApplicationEvent implements Event {

    @JsonProperty(required = true)
    private transient String tenant;

    protected AbstractDistributedEvent(@JsonProperty("tenant") final String tenant) {
        // for serialization libs like jackson
        this(new Object(), tenant, null);
    }

    public AbstractDistributedEvent(final Object source, final String tenant, final String originService) {
        super(source, originService, null);
        this.tenant = tenant;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    /**
     * @param tenant
     *            the tenant to set
     */
    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

}
