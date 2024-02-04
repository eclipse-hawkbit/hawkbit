/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindowRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body of DistributionSet for assignment operations (ID only).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MgmtDistributionSetAssignment extends MgmtId {

    @Schema(example = "1691065930359")
    private long forcetime;
    @JsonProperty(required = false)
    @Schema(example = "23")
    private Integer weight;
    @JsonProperty(required = false)
    @Schema(example = "false")
    private Boolean confirmationRequired;
    private MgmtActionType type;
    private MgmtMaintenanceWindowRequestBody maintenanceWindow;

    /**
     * Constructor
     * 
     * @param id
     *            ID of object
     */
    @JsonCreator
    public MgmtDistributionSetAssignment(@JsonProperty(required = true, value = "id") final Long id) {
        super(id);
    }
}