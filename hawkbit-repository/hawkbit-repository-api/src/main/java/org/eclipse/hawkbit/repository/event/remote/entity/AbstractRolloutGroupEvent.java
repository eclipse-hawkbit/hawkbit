/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import java.util.Objects;

import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Event which is published in case a {@linkplain RolloutGroup} is created or
 * updated
 */
public abstract class AbstractRolloutGroupEvent extends RemoteEntityEvent<RolloutGroup> {
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

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), rolloutId);
    }
}
