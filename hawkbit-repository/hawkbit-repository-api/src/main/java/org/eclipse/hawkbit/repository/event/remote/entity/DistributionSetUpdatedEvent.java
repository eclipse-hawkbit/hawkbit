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

import org.eclipse.hawkbit.repository.event.entity.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Defines the remote event for updating a {@link DistributionSet}.
 */
public class DistributionSetUpdatedEvent extends RemoteEntityEvent<DistributionSet> implements EntityUpdatedEvent {

    private static final long serialVersionUID = 1L;

    private boolean complete;

    /**
     * Default constructor.
     */
    public DistributionSetUpdatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     *
     * @param ds Distribution Set
     * @param applicationId the origin application id
     * @param complete <code>true</code> if {@link DistributionSet} is after the
     *         update {@link DistributionSet#isComplete()}
     */
    public DistributionSetUpdatedEvent(final DistributionSet ds, final String applicationId, final boolean complete) {
        super(ds, applicationId);
        this.complete = complete;
    }

    /**
     * @return <code>true</code> if {@link DistributionSet} is after the update
     *         {@link DistributionSet#isComplete()}
     */
    public boolean isComplete() {
        return complete;
    }

}
