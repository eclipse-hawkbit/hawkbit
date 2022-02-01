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

import org.eclipse.hawkbit.repository.model.Action;

/**
 * Defines the remote event of creating a new {@link Action}.
 */
public abstract class AbstractActionEvent extends RemoteEntityEvent<Action> {
    private static final long serialVersionUID = 1L;

    private final Long targetId;
    private final Long rolloutId;
    private final Long rolloutGroupId;

    /**
     * Default constructor.
     */
    protected AbstractActionEvent() {
        // for serialization libs like jackson
        this.targetId = null;
        this.rolloutId = null;
        this.rolloutGroupId = null;
    }

    /**
     * Constructor
     * 
     * @param action
     *            the created action
     * @param targetId
     *            targetId identifier (optional)
     * @param rolloutId
     *            rollout identifier (optional)
     * @param rolloutGroupId
     *            rollout group identifier (optional)
     * @param applicationId
     *            the origin application id
     */
    protected AbstractActionEvent(final Action action, final Long targetId, final Long rolloutId,
            final Long rolloutGroupId, final String applicationId) {
        super(action, applicationId);
        this.targetId = targetId;
        this.rolloutId = rolloutId;
        this.rolloutGroupId = rolloutGroupId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Long getRolloutId() {
        return rolloutId;
    }

    public Long getRolloutGroupId() {
        return rolloutGroupId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        final AbstractActionEvent that = (AbstractActionEvent) o;
        return Objects.equals(targetId, that.targetId) && Objects.equals(rolloutId, that.rolloutId)
                && Objects.equals(rolloutGroupId, that.rolloutGroupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetId, rolloutId, rolloutGroupId);
    }
}
