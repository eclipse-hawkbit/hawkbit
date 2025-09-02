/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import lombok.Data;

/**
 * Represents rollout or rollout group statuses and count of targets in each
 * status.
 */
@Data
public class TotalTargetCountActionStatus {

    private final Action.Status status;
    private final Long count;
    private final Long id;

    public TotalTargetCountActionStatus(final Long id, final Action.Status status, final Long count) {
        this.status = status;
        this.count = count;
        this.id = id;
    }

    public TotalTargetCountActionStatus(final Action.Status status, final Long count) {
        this(null, status, count);
    }
}