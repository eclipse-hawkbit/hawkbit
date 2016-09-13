/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.springframework.cloud.bus.event.bulk;

import java.util.List;

import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.springframework.cloud.bus.event.entity.GenericEventEntity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A bulk event which contains one or many new target tags after creating.
 */
public class TargetTagCreatedBulkEvent extends BaseEntityBulkEvent<TargetTag> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected TargetTagCreatedBulkEvent(
            @JsonProperty("entitySource") final GenericEventEntity<List<Long>> entitySource) {
        super(entitySource);
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant
     * @param entities
     *            the new ds tags
     */
    public TargetTagCreatedBulkEvent(final String tenant, final List<TargetTag> entities, final String originService) {
        super(tenant, entities, DistributionSetTag.class, originService);
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant.
     * @param entity
     *            the new ds tag
     */
    public TargetTagCreatedBulkEvent(final TargetTag entity, final String originService) {
        super(entity, originService);
    }

    /**
     * TODO REMOVE Constructor
     */
    public TargetTagCreatedBulkEvent(final String tenant, final List<TargetTag> entities) {
        super(tenant, entities, DistributionSetTag.class, "REMOVE");
    }

    /**
     * TODO REMOVE Constructor
     */
    public TargetTagCreatedBulkEvent(final String tenant, final TargetTag entity) {
        this(entity, "REMOVE");
    }

}
