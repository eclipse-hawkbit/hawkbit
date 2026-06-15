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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.event.entity.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.TargetTag;

/**
 * Defines the remote event for the creation of a new {@link TargetTag}.
 */
@NoArgsConstructor // for serialization libs like jackson
public class TargetTagCreatedEvent extends RemoteEntityEvent<TargetTag> implements EntityCreatedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    public TargetTagCreatedEvent(final TargetTag targetTag) {
        super(targetTag);
    }
}