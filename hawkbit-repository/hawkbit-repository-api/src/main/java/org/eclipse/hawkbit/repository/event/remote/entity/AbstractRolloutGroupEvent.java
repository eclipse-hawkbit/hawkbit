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
import java.util.Objects;

import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Event which is published in case a {@linkplain RolloutGroup} is created or updated
 */
public abstract class AbstractRolloutGroupEvent extends RemoteEntityEvent<RolloutGroup> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long rolloutId;

    /**
     * Default constructor.
     */
    protected AbstractRolloutGroupEvent() {
        // for serialization libs like jackson
        this.rolloutId = null;
    }

    protected AbstractRolloutGroupEvent(final RolloutGroup rolloutGroup, final Long rolloutId,
            final String applicationId) {
        super(rolloutGroup, applicationId);
        this.rolloutId = rolloutId;
    }

    public Long getRolloutId() {
        return rolloutId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), rolloutId);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        final AbstractRolloutGroupEvent that = (AbstractRolloutGroupEvent) o;
        return Objects.equals(rolloutId, that.rolloutId);
    }
}
