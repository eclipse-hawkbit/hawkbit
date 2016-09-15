/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

/**
 * Abstract event definition class which holds the necessary revsion and tenant
 * information which every event needs.
 * 
 * @see AbstractDistributedEvent for events which should be distributed to other
 *      cluster nodes
 */
public class DefaultEvent implements Event {

    private final long revision;
    private final String tenant;

    /**
     * @param revision
     *            the revision number of the event
     * @param tenant
     *            the tenant of the event
     */
    protected DefaultEvent(final long revision, final String tenant) {
        this.revision = revision;
        this.tenant = tenant;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

}
