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
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.Action;
import org.jspecify.annotations.Nullable;

/**
 * Defines the remote event of creating a new {@link Action}.
 */
@NoArgsConstructor(force = true) // for serialization libs like jackson
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
public abstract class AbstractActionEvent extends RemoteEntityEvent<Action> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long targetId;
    private final Long rolloutId;
    private final Long rolloutGroupId;

    protected AbstractActionEvent(
            final Action action, @Nullable final Long targetId, @Nullable final Long rolloutId, @Nullable final Long rolloutGroupId) {
        super(action);
        this.targetId = targetId;
        this.rolloutId = rolloutId;
        this.rolloutGroupId = rolloutGroupId;
    }
}