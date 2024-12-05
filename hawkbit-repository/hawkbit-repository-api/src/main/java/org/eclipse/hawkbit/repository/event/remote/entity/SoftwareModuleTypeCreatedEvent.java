/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
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
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Defines the remote event of creating a new {@link SoftwareModuleType}.
 */
@NoArgsConstructor(access = AccessLevel.PUBLIC) // for serialization libs like jackson
public class SoftwareModuleTypeCreatedEvent extends RemoteEntityEvent<SoftwareModuleType> implements EntityCreatedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param baseEntity the SoftwareModuleType
     * @param applicationId the origin application id
     */
    public SoftwareModuleTypeCreatedEvent(final SoftwareModuleType baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }
}