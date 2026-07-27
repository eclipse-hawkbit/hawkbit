/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
import org.eclipse.hawkbit.repository.model.AutoAssignment;

/**
 * Defines the remote event of creating a new {@link AutoAssignment}.
 */
@NoArgsConstructor
public class AutoAssignmentCreatedEvent extends RemoteEntityEvent<AutoAssignment> implements EntityCreatedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    public AutoAssignmentCreatedEvent(final AutoAssignment autoAssignment) {
        super(autoAssignment);
    }
}