/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds properties for {@link Action}
 */
@NoArgsConstructor // for serialization libs like jackson
@Data
public class ActionProperties implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Action.ActionType actionType;
    private String tenant;
    private boolean maintenanceWindowAvailable;

    private Action.Status status;

    /**
     * Constructor
     *
     * @param action the action to populate the properties from
     */
    public ActionProperties(final Action action) {
        this.id = action.getId();
        this.actionType = action.getActionType();
        this.tenant = action.getTenant();
        this.maintenanceWindowAvailable = action.isMaintenanceWindowAvailable();
        this.status = action.getStatus();
    }

    @JsonIgnore
    public boolean isWaitingConfirmation() {
        return status == Action.Status.WAIT_FOR_CONFIRMATION;
    }
}