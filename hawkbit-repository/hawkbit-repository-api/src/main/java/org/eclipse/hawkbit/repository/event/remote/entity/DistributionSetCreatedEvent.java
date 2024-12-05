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

import java.io.Serial;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.event.entity.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Defines the remote of creating a new {@link DistributionSet}.
 */
@NoArgsConstructor(access = AccessLevel.PUBLIC) // for serialization libs like jackson
public class DistributionSetCreatedEvent extends RemoteEntityEvent<DistributionSet> implements EntityCreatedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

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