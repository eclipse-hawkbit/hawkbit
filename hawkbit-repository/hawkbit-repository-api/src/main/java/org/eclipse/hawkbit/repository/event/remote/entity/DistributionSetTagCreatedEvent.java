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
import org.eclipse.hawkbit.repository.model.DistributionSetTag;

/**
 * Defines the {@link RemoteEntityEvent} for creation of a new
 * {@link DistributionSetTag}.
 */
public class DistributionSetTagCreatedEvent extends RemoteEntityEvent<DistributionSetTag>
        implements EntityCreatedEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public DistributionSetTagCreatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     *
     * @param tag the tag which is deleted
     * @param applicationId the origin application id
     */
    public DistributionSetTagCreatedEvent(final DistributionSetTag tag, final String applicationId) {
        super(tag, applicationId);
    }
}
