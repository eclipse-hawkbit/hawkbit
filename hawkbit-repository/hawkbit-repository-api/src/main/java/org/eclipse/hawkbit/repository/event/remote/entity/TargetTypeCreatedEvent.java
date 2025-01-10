/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import java.io.Serial;

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.event.entity.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.TargetType;

/**
 * Defines the remote event of creating a new {@link TargetType}.
 */
@NoArgsConstructor // for serialization libs like jackson
public class TargetTypeCreatedEvent extends RemoteEntityEvent<TargetType> implements EntityCreatedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param baseEntity the TargetType
     * @param applicationId the origin application id
     */
    public TargetTypeCreatedEvent(final TargetType baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }
}