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
public class RolloutGroupChangeEvent extends TenantAwareUiEvent {

    private static final long serialVersionUID = 1L;
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
    public RolloutGroupChangeEvent(final String tenant, final Long rolloutId, final Long rolloutGroupId) {
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
}
