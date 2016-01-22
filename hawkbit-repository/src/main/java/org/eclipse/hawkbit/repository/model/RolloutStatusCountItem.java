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

public class RolloutStatusCountItem<T extends Object> {

    private final T status;
    private final Long count;

    public RolloutStatusCountItem(final T status, final Long count) {
        this.status = status;
        this.count = count;
    }

    /**
     * @return the status
     */
    public T getStatus() {
        return status;
    }

    /**
     * @return the count
     */
    public Long getCount() {
        return count;
    }
}
