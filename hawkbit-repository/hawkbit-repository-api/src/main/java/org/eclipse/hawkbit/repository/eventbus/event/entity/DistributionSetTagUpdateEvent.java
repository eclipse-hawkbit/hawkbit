/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event.entity;

import org.eclipse.hawkbit.repository.model.DistributionSetTag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the {@link AbstractBaseEntityEvent} for update a
 * {@link DistributionSetTag}.
 *
 */
public class DistributionSetTagUpdateEvent extends TenantAwareBaseEntityEvent<DistributionSetTag> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected DistributionSetTagUpdateEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource) {
        super(entitySource);
    }

    /**
     * Constructor.
     * 
     * @param tag
     *            the tag which is updated
     */
    public DistributionSetTagUpdateEvent(final DistributionSetTag tag, final String originService) {
        super(tag, originService);
    }
}
