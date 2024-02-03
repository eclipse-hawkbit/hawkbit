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
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.model.Action.Status;

/**
 * Target with action status.
 */
@Data
public class TargetWithActionStatus implements Identifiable<Long> {

    private Target target;

    private Status status;

    private Integer lastActionStatusCode;

    public TargetWithActionStatus(final Target target) {
        this.target = target;
    }

    public TargetWithActionStatus(final Target target, final Status status) {
        this.status = status;
        this.target = target;
    }

    public TargetWithActionStatus(final Target target, final Status status, final Integer lastActionStatusCode) {
        this.status = status;
        this.target = target;
        this.lastActionStatusCode = lastActionStatusCode;
    }

    @Override
    public Long getId() {
        return target.getId();
    }
}
