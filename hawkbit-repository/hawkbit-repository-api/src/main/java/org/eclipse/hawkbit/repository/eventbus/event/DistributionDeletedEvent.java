/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import org.eclipse.hawkbit.eventbus.event.AbstractDistributedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;


/**
 * * Defines the {@link AbstractDistributedEvent} for deletion of {@link DistributionSet}.

 *
 */
public class DistributionDeletedEvent extends AbstractDistributedEvent{
	private static final long serialVersionUID = -3308850381757843098L;
	final Long[] distributionSetIDs;
    /**
     * @param tenant
     *            the tenant for this event
     * @param distributionSetId
     *            the ID of the target which has been deleted
     */
    public DistributionDeletedEvent(final String tenant, final Long...distributionIds) {
        super(-1, tenant);
        this.distributionSetIDs = distributionIds;
    }

    public Long[] getDistributionSetIDs() {
		return distributionSetIDs;
	}
}
