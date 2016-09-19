/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.event.EntityEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.BaseEntityIdEvent;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event for update the targets info.
 */
public class TargetInfoUpdateEvent extends BaseEntityIdEvent implements EntityEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private final Class<? extends Target> entityClass;

    @JsonIgnore
    private transient TargetInfo targetInfo;

    /**
     * Constructor for json serialization.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param entityClass
     *            the entity entityClassName
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    protected TargetInfoUpdateEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entityId") final Long entityId,
            @JsonProperty("entityClass") final Class<? extends Target> entityClass,
            @JsonProperty("originService") final String applicationId) {
        super(entityId, tenant, applicationId);
        this.entityClass = entityClass;
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
        this(targetInfo.getTarget().getTenant(), targetInfo.getTarget().getId(), targetInfo.getTarget().getClass(),
                applicationId);
        this.targetInfo = targetInfo;
    }

    @Override
    @JsonIgnore
    public <E> E getEntity(final Class<E> entityClass) {
        return entityClass.cast(targetInfo);
    }

    @Override
    @JsonIgnore
    public TargetInfo getEntity() {
        if (targetInfo == null) {
            final Target target = EventEntityManagerHolder.getInstance().getEventEntityManager().findEntity(getTenant(),
                    getEntityId(), entityClass);
            targetInfo = target == null ? null : target.getTargetInfo();
        }
        return targetInfo;
    }

}
