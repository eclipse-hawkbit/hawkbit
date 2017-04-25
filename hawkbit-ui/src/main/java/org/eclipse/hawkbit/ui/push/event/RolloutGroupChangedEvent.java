/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push.event;

/**
 * TenantAwareEvent declaration for the UI to notify the UI that a rollout has
 * been changed.
 * 
 *
 */
public class RolloutGroupChangedEvent extends TenantAwareUiEvent {

    private final Long rolloutId;
    private final Long rolloutGroupId;

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant of the event
     * @param rolloutId
     *            the ID of the rollout which has been changed
     * @param rolloutGroupId
     *            the ID of the rollout group which has been changed
     */
    public RolloutGroupChangedEvent(final String tenant, final Long rolloutId, final Long rolloutGroupId) {
        super(tenant);
        this.rolloutId = rolloutId;
        this.rolloutGroupId = rolloutGroupId;
    }

    public Long getRolloutId() {
        return rolloutId;
    }

    public Long getRolloutGroupId() {
        return rolloutGroupId;
    }

    @Override
    public String toString() {
        return "RolloutGroupChangeEvent [rolloutId=" + rolloutId + ", rolloutGroupId=" + rolloutGroupId
                + ", getTenant()=" + getTenant() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((rolloutGroupId == null) ? 0 : rolloutGroupId.hashCode());
        result = prime * result + ((rolloutId == null) ? 0 : rolloutId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RolloutGroupChangedEvent other = (RolloutGroupChangedEvent) obj;
        if (rolloutGroupId == null) {
            if (other.rolloutGroupId != null) {
                return false;
            }
        } else if (!rolloutGroupId.equals(other.rolloutGroupId)) {
            return false;
        }
        if (rolloutId == null) {
            if (other.rolloutId != null) {
                return false;
            }
        } else if (!rolloutId.equals(other.rolloutId)) {
            return false;
        }
        return true;
    }

}
