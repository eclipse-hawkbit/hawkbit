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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindowRequestBody;

/**
 * Request Body of Target for assignment operations (ID only).
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetAssignmentRequestBody {

    @Schema(description = "The technical identifier of the entity", example = "target4")
    private String id;

    @Schema(description = "Forcetime in milliseconds", example = "1682408575278")
    private long forcetime;

    @Schema(description = "The type of the assignment")
    private MgmtActionType type;

    @Schema(description = "Separation of download and install by defining a maintenance window for the installation")
    private MgmtMaintenanceWindowRequestBody maintenanceWindow;

    @Schema(description = "Importance of the assignment", example = "100")
    private Integer weight;

    @Schema(description = "(Available with user consent flow active) Defines, if the confirmation is required for " +
            "an action. Confirmation is required per default")
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