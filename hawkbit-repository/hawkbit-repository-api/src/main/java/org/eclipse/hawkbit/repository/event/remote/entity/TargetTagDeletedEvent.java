/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.TargetTag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the remote event of update a {@link TargetTag}.
 *
 */
public class TargetTagDeletedEvent extends TenantAwareBaseEntityEvent<TargetTag> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for json serialization.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param entityClassName
     *            the entity entityClassName
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    protected TargetTagDeletedEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entityId") final Long entityId,
            @JsonProperty("entityClassName") final String entityClassName,
            @JsonProperty("originService") final String applicationId) {
        super(tenant, entityId, entityClassName, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param tag
     *            the tag which is deleted
     * @param applicationId
     *            the origin application id
     */
    public TargetTagDeletedEvent(final TargetTag tag, final String applicationId) {
        super(tag, applicationId);
    }
}
