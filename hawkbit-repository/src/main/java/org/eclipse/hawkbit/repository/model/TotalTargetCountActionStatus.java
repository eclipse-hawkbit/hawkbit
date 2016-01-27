/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * Represents rollout or rollout group statuses and count of targets in each
 * status.
 * 
 */
public class TotalTargetCountActionStatus {

    private final Action.Status status;
    private final Long count;
    private Long id;

    public TotalTargetCountActionStatus(final Long id, final Action.Status status, final Long count) {
        this.status = status;
        this.count = count;
        this.id = id;
    }

    public TotalTargetCountActionStatus(final Action.Status status, final Long count) {
        this.status = status;
        this.count = count;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the status
     */
    public Action.Status getStatus() {
        return status;
    }

    /**
     * @return the count
     */
    public Long getCount() {
        return count;
    }
}
