/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.eventbus.event;

/**
 * Abstract event definition class which holds the necessary revsion and tenant
 * information which every event needs.
 * 
 * @author Michael Hirsch
 * @see AbstractDistributedEvent for events which should be distributed to other
 *      cluster nodes
 */
public class AbstractEvent implements Event {

    private final long revision;
    private final String tenant;

    /**
     * @param revision
     *            the revision number of the event
     * @param tenant
     *            the tenant of the event
     */
    protected AbstractEvent(final long revision, final String tenant) {
        this.revision = revision;
        this.tenant = tenant;
    }

    @Override
    public long getRevision() {
        return revision;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

}
