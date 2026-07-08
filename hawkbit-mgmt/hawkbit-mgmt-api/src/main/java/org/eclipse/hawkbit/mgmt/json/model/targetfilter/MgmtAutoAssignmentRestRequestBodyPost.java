/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.mgmt.json.model.targetfilter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

/**
 * Model for requests containing an auto assignment body e.g. in a POST request
 * of creating an auto assignment via REST API
 */
@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
@Schema(example = """
        {
            "distributionSetId" : 6,
            "targetFilterQueryId" : 3,
            "startAt" : 1682408570791,
            "actionType" : "forced",
            "confirmationRequired" : true,
            "weight" : 400
        }""")
public class MgmtAutoAssignmentRestRequestBodyPost {
    @JsonProperty(required = true)
    @Schema(description = "The ID of the target filter query", example = "3")
    private Long targetFilterQueryId;

    @JsonProperty(required = true)
    @Schema(description = "The ID of the distribution set", example = "6")
    private Long distributionSetId;

    @Schema(description = "Start at timestamp of the auto assignment", example = "1691065753136")
    private Long startAt;

    @Schema(description = "The type of the Action", example = "forced")
    private MgmtActionType actionType;

    @Schema(description = "Weight of the resulting Action", example = "400")
    private Integer weight;

    @Schema(example = "false")
    private Boolean confirmationRequired;
}
