/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindowRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

/**
 * Request Body of DistributionSet for assignment operations (ID only).
 */
@NoArgsConstructor
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MgmtDistributionSetAssignment extends MgmtId {

    @Schema(description = "Forcetime in milliseconds", example = "1691065930359")
    private long forcetime;

    @Schema(description = "Importance of the assignment", example = "23")
    private Integer weight;

    @Schema(description = """
            (Available with user consent flow active) Specifies if the confirmation by the device
            is required for this action""", example = "false")
    private Boolean confirmationRequired;

    @Schema(description = "The type of the assignment")
    private MgmtActionType type;

    @Schema(description = "Separation of download and install by defining a maintenance window for the installation")
    private MgmtMaintenanceWindowRequestBody maintenanceWindow;

    /**
     * Constructor
     *
     * @param id ID of object
     */
    @JsonCreator
    public MgmtDistributionSetAssignment(@JsonProperty(required = true, value = "id") final Long id) {
        super(id);
    }
}