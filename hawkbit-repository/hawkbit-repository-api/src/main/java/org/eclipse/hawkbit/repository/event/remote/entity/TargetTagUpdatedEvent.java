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
import org.eclipse.hawkbit.repository.model.TargetTag;

/**
 * Defines the remote event for updating a {@link TargetTag}.
 */
public class TargetTagUpdatedEvent extends RemoteEntityEvent<TargetTag> implements EntityUpdatedEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public TargetTagUpdatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     *
     * @param tag the tag which is updated
     * @param applicationId the origin application id
     */
    public TargetTagUpdatedEvent(final TargetTag tag, final String applicationId) {
        super(tag, applicationId);
    }
}
