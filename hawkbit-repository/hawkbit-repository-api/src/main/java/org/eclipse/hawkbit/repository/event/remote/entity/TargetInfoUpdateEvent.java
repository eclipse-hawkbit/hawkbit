/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.EntityEvent;
import org.eclipse.hawkbit.repository.model.TargetInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event for update the targets info.
 */
public class TargetInfoUpdateEvent extends BaseEntityIdEvent implements EntityEvent {

    private static final long serialVersionUID = 1L;
    private TargetInfo targetInfo;

    /**
     * Constructor for json serialization.
     * 
     * @param entitySource
     *            the entity source within the json entity information
     * @param tenant
     *            the tenant
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    protected TargetInfoUpdateEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entityId") final Long entityId, @JsonProperty("originService") final String applicationId) {
        super(entityId, tenant, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param targetInfo
     *            the target info
     * @param applicationId
     *            the origin application id
     */
    public TargetInfoUpdateEvent(final TargetInfo targetInfo, final String applicationId) {
        this(targetInfo.getTarget().getTenant(), targetInfo.getTarget().getId(), applicationId);
        this.targetInfo = targetInfo;
    }

    @Override
    public <E> E getEntity(final Class<E> entityClass) {
        return entityClass.cast(targetInfo);
    }

    @Override
    public TargetInfo getEntity() {
        return targetInfo;
    }

}
