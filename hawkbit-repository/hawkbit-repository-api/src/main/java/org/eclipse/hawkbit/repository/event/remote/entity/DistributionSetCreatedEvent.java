/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.entity.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Defines the the remote of creating a new {@link DistributionSet}.
 */
public class DistributionSetCreatedEvent extends RemoteEntityEvent<DistributionSet> implements EntityCreatedEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public DistributionSetCreatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     *
     * @param distributionSet the created distributionSet
     * @param applicationId the origin application id
     */
    public DistributionSetCreatedEvent(final DistributionSet distributionSet, final String applicationId) {
        super(distributionSet, applicationId);
    }

}
