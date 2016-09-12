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
 */
public abstract class AbstractDistributedEvent implements DistributedEvent {

    private static final long serialVersionUID = 1L;

    private final long revision;
    private String originNodeId;
    private String nodeId;
    private final String tenant;

    /**
     * Constructor.
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

    @Override
    public void setOriginNodeId(final String originNodeId) {
        this.originNodeId = originNodeId;
    }

    @Override
    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;

    }

    @Override
    public String getOriginNodeId() {
        return this.originNodeId;
    }

    @Override
    public String getNodeId() {
        return this.nodeId;
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
