/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import org.eclipse.hawkbit.repository.model.TargetInfo;

/**
 * Event for update the targets info.
 */
public class TargetInfoUpdateEvent implements EntityEvent {

    private final long revision;
    private final TargetInfo targetInfo;
    private final String tenant;
    private String originNodeId;
    private String nodeId;

    /**
     * Constructor.
     * 
     * @param targetInfo
     *            the target info entity
     */
    public TargetInfoUpdateEvent(final TargetInfo targetInfo) {
        this.targetInfo = targetInfo;
        this.tenant = targetInfo.getTarget().getTenant();
        this.revision = -1;
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
    public <E> E getEntity(final Class<E> entityClass) {
        return entityClass.cast(targetInfo);
    }

    @Override
    public TargetInfo getEntity() {
        return targetInfo;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

}
