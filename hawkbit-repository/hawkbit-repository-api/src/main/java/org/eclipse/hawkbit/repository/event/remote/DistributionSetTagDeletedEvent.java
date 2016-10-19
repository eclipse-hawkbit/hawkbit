/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.model.DistributionSetTag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the the remote event of delete a {@link DistributionSetTag}.
 */
public class DistributionSetTagDeletedEvent extends RemoteIdEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for json serialization.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    public DistributionSetTagDeletedEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entityId") final Long entityId, @JsonProperty("originService") final String applicationId) {
        super(entityId, tenant, applicationId);
    }
}
