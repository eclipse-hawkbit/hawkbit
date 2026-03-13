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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.event.entity.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Defines the remote event for updating a {@link DistributionSet}.
 */
@NoArgsConstructor // for serialization libs like jackson
@Data
@EqualsAndHashCode(callSuper = true)
public class DistributionSetUpdatedEvent extends RemoteEntityEvent<DistributionSet> implements EntityUpdatedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    public DistributionSetUpdatedEvent(final DistributionSet distributionSet) {
        super(distributionSet);
    }
}