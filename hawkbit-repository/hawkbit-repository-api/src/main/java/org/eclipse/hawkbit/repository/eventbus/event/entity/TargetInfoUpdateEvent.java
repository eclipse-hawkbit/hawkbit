/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event.entity;

import org.eclipse.hawkbit.repository.model.TargetInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event for update the targets info.
 */
public class TargetInfoUpdateEvent extends BaseEntityEvent<TargetInfo, Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected TargetInfoUpdateEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource) {
        super(entitySource);
    }

    /**
     * @param tenant
     *            the tenant for this event
     * @param targetId
     *            the ID of the target which has been deleted
     */
    public TargetInfoUpdateEvent(final TargetInfo targetInfo, final String originService) {
        super(targetInfo.getTarget().getTenant(), targetInfo.getTarget().getId(), TargetInfo.class, originService);
    }

    /**
     * TODO: REMOVE!
     */
    public TargetInfoUpdateEvent(final TargetInfo targetInfo) {
        super(targetInfo.getTarget().getTenant(), targetInfo.getTarget().getId(), TargetInfo.class, "REMOVE");
    }

}
