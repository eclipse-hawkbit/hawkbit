/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindowRequestBody;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body of Target for assignment operations (ID only).
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetAssignmentRequestBody {

    private String id;
    private long forcetime;
    private MgmtActionType type;
    private MgmtMaintenanceWindowRequestBody maintenanceWindow;
    private Integer weight;
    private Boolean confirmationRequired;

    /**
     * JsonCreator Constructor
     * 
     * @param id Mandatory ID of the target that should be assigned
     */
    @JsonCreator
    public MgmtTargetAssignmentRequestBody(@JsonProperty(required = true, value = "id") final String id) {
        this.id = id;
    }
}
