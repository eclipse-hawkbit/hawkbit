/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.local;

/**
 * Event declaration for the UI to notify the UI that a rollout has been
 * changed.
 * 
 * @author Michael Hirsch
 *
 */
public class RolloutChangeEvent extends DefaultEvent {

    private static final long serialVersionUID = 1L;
    private final Long rolloutId;

    /**
     * @param revision
     *            the revision of the event
     * @param tenant
     *            the tenant of the event
     * @param rolloutId
     *            the ID of the rollout which has been changed
     */
    public RolloutChangeEvent(final long revision, final String tenant, final Long rolloutId) {
        super(revision, tenant);
        this.rolloutId = rolloutId;
    }

    public Long getRolloutId() {
        return rolloutId;
    }
}
