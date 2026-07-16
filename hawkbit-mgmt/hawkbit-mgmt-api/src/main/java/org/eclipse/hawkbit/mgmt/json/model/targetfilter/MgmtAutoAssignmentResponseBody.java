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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
        **_links**:
        * **start** - Link to start the auto assignment
        * **pause** - Link to pause a running auto assignment
        * **resume** - Link to resume a paused auto assignment
        * **approve** - Link to approve an auto assignment
        * **deny** - Link to deny an auto assignment
        * **distributionset** - The link to the distribution set
        """, example = """
        {
          "createdBy" : "bumlux",
          "createdAt" : 1682408568812,
          "lastModifiedBy" : "bumlux",
          "lastModifiedAt" : 1682408568812,
          "name" : "filterName",
          "description" : "Auto assignment for all pending targets",
          "id" : 3,
          "targetFilterQuery" : "name==*",
          "distributionSetId" : 6,
          "status" : "waiting_for_approval",
          "startAt" : 1682408570791,
          "actionType" : "forced",
          "confirmationRequired" : true,
          "weight" : 400,
          "_links" : {
            "start" : {
              "href" : "https://management-api.host.com/rest/v1/autoassignments/3/start"
            },
            "pause" : {
              "href" : "https://management-api.host.com/rest/v1/autoassignments/3/pause"
            },
            "resume" : {
              "href" : "https://management-api.host.com/rest/v1/autoassignments/3/resume"
            },
            "approve" : {
              "href" : "https://management-api.host.com/rest/v1/autoassignments/3/approve"
            },
            "deny" : {
              "href" : "https://management-api.host.com/rest/v1/autoassignments/3/deny"
            },
            "distributionset" : {
              "href" : "https://management-api.host.com/rest/v1/distributionsets/6",
              "name" : "bd3a71cb-6c8f-445c-adbb-e221414dcd96:1.0"
            },
            "self" : {
              "href" : "https://management-api.host.com/rest/v1/autoassignments/3"
            }
          }
        }""")
public class MgmtAutoAssignmentResponseBody extends MgmtNamedEntity {

    @JsonProperty(required = true)
    @Schema(description = "The ID of the auto assignment. Currently equal to the ID of the target filter query it "
            + "originates from, but may become an independent identifier in the future", example = "3")
    private long id;

    @Schema(description = "The target filter query (RSQL) of the auto assignment", example = "name==*")
    private String targetFilterQuery;

    @JsonProperty(required = true)
    @Schema(description = "The ID of the distribution set", example = "6")
    private long distributionSetId;

    @JsonProperty(required = true)
    @Schema(description = "The status of this auto assignment", example = "waiting_for_approval")
    private String status;

    @Schema(description = "Start at timestamp of the auto assignment", example = "1691065753136")
    private Long startAt;

    @Schema(description = "The type of the Action", example = "forced")
    private MgmtActionType actionType;

    @Schema(description = "Weight of the resulting Action", example = "400")
    private Integer weight;

    @Schema(example = "false")
    private boolean confirmationRequired;

    @Schema(example = "exampleUsername")
    private String approvalDecidedBy;

    @Schema(example = "Approval remark")
    private String approvalRemark;
}