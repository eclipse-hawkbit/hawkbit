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
 * An abstract class of the {@link DistributedEvent} implementation which holds
 * all the necessary information of distributing events to other nodes.
 *
 *
 *
 *
 */
public abstract class AbstractDistributedEvent implements DistributedEvent {

    private static final long serialVersionUID = 1L;
    private final long revision;
    private String originNodeId;
    private String nodeId;
    private final String tenant;

    /**
     *
     * @param revision
     *            the revision of this event
     * @param tenant
     *            the tenant for this event
     */
    protected AbstractDistributedEvent(final long revision, final String tenant) {
        this.revision = revision;
        this.tenant = tenant;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.hawkbit.server.eventbus.event.NodeAware#setOriginNodeId(java.
     * lang. String)
     */
    @Override
    public void setOriginNodeId(final String originNodeId) {
        this.originNodeId = originNodeId;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.hawkbit.server.eventbus.event.NodeAware#setNodeId(java.lang.
     * String)
     */
    @Override
    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.hawkbit.server.eventbus.event.NodeAware#getOriginNodeId()
     */
    @Override
    public String getOriginNodeId() {
        return this.originNodeId;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.eventbus.event.NodeAware#getNodeId()
     */
    @Override
    public String getNodeId() {
        return this.nodeId;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.eventbus.event.Event#getRevision()
     */
    @Override
    public long getRevision() {
        return revision;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.eventbus.event.EntityEvent#getTenant()
     */
    @Override
    public String getTenant() {
        return tenant;
    }

}
