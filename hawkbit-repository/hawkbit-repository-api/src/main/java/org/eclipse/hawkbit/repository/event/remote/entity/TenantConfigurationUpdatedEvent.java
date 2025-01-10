/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
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
import org.eclipse.hawkbit.repository.event.entity.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;

/**
 * Defines the remote event of updating a {@link TenantConfiguration}.
 */
@NoArgsConstructor // for serialization libs like jackson
public class TenantConfigurationUpdatedEvent extends RemoteEntityEvent<TenantConfiguration> implements EntityUpdatedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param baseEntity the tenantConfiguration
     * @param applicationId the origin application id
     */
    public TenantConfigurationUpdatedEvent(final TenantConfiguration baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }
}