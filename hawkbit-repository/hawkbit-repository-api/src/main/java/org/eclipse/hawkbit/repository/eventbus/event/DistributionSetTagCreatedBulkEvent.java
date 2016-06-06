/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import java.util.List;

import org.eclipse.hawkbit.repository.model.DistributionSetTag;

/**
 * * A bulk event which contains one or many new ds tag after creating.
 */
public class DistributionSetTagCreatedBulkEvent extends AbstractEntityBulkEvent<DistributionSetTag> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant
     * @param entities
     *            the new ds tags
     */
    public DistributionSetTagCreatedBulkEvent(final String tenant, final List<DistributionSetTag> entities) {
        super(tenant, entities);
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant.
     * @param entity
     *            the new ds tag
     */
    public DistributionSetTagCreatedBulkEvent(final String tenant, final DistributionSetTag entity) {
        super(tenant, entity);
    }

}
