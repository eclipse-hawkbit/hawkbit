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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Event which is published in case a {@linkplain RolloutGroup} is created or updated
 */
@NoArgsConstructor(force = true)// for serialization libs like jackson
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractRolloutGroupEvent extends RemoteEntityEvent<RolloutGroup> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long rolloutId;

    protected AbstractRolloutGroupEvent(final RolloutGroup rolloutGroup, final Long rolloutId) {
        super(rolloutGroup);
        this.rolloutId = rolloutId;
    }
}