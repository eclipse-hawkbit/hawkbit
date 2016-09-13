/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.springframework.cloud.bus.event;

import org.eclipse.hawkbit.eventbus.event.Event;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An abstract class of the {@link DistributedEvent} implementation which holds
 * all the necessary information of distributing events to other nodes.
 *
 */
public abstract class AbstractDistributedEvent extends RemoteApplicationEvent implements Event {

    protected AbstractDistributedEvent() {
        // for serialization libs like jackson
        this(new Object(), null);
    }

    public AbstractDistributedEvent(final Object source, final String originService) {
        super(source, originService, null);
    }

    @Override
    @JsonIgnore
    public long getRevision() {
        return -1;
    }

}
