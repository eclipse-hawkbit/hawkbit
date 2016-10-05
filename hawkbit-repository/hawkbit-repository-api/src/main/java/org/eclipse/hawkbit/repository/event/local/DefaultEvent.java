/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.local;

import org.eclipse.hawkbit.repository.event.Event;
import org.eclipse.hawkbit.repository.event.remote.TenantAwareDistributedEvent;
import org.springframework.context.ApplicationEvent;

/**
 * Abstract event definition class which holds the necessary revision and tenant
 * information which every event needs.
 * 
 * @see TenantAwareDistributedEvent for events which should be distributed to
 *      other cluster nodes
 */
public class DefaultEvent extends ApplicationEvent implements Event {

    private static final long serialVersionUID = 1L;
    private final String tenant;
    private final long revision;

    /**
     * @param revision
     *            the revision number of the event
     * @param tenant
     *            the tenant of the event
     */
    protected DefaultEvent(final long revision, final String tenant) {
        super("LOCAL_EVENT");
        this.revision = revision;
        this.tenant = tenant;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    public long getRevision() {

        return revision;
    }

}
