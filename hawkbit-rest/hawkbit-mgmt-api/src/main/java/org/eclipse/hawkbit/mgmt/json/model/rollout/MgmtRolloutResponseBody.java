/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.rollout;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
    **_links**:
    * **start** - Link to start the rollout in sync mode
    * **pause** - Link to pause a running rollout
    * **triggerNextGroup** - Link for triggering next rollout group on a running rollout
    * **resume** - Link to resume a paused rollout
    * **groups** - Link to retrieve the groups a rollout
    * **approve** - Link to approve a rollout
    * **deny** - Link to deny a rollout
    * **distributionset** - The link to the distribution set
    """, example = """
    {
      "createdBy" : "bumlux",
      "createdAt" : 1682408568812,
      "lastModifiedBy" : "bumlux",
      "lastModifiedAt" : 1682408568812,
      "name" : "exampleRollout",
      "description" : "Rollout for all named targets",
      "targetFilterQuery" : "id==targets-*",
      "distributionSetId" : 6,
      "status" : "creating",
      "totalTargets" : 20,
      "totalTargetsPerStatus" : {
        "running" : 0,
        "notstarted" : 20,
        "scheduled" : 0,
        "cancelled" : 0,
        "finished" : 0,
        "error" : 0
      },
      "totalGroups" : 5,
      "startAt" : 1682408570791,
      "forcetime" : 1682408571791,
      "deleted" : false,
      "type" : "forced",
      "_links" : {
        "start" : {
          "href" : "https://management-api.host.com/rest/v1/rollouts/6/start"
        },
        "pause" : {
          "href" : "https://management-api.host.com/rest/v1/rollouts/6/pause"
        },
        "resume" : {
          "href" : "https://management-api.host.com/rest/v1/rollouts/6/resume"
        },
        "triggerNextGroup" : {
          "href" : "https://management-api.host.com/rest/v1/rollouts/6/triggerNextGroup"
        },
        "approve" : {
          "href" : "https://management-api.host.com/rest/v1/rollouts/6/approve"
        },
        "deny" : {
          "href" : "https://management-api.host.com/rest/v1/rollouts/6/deny"
        },
        "groups" : {
          "href" : "https://management-api.host.com/rest/v1/rollouts/6/deploygroups?offset=0&limit=50"
        },
        "distributionset" : {
          "href" : "https://management-api.host.com/rest/v1/distributionsets/6",
          "name" : "bd3a71cb-6c8f-445c-adbb-e221414dcd96:1.0"
        },
        "self" : {
          "href" : "https://management-api.host.com/rest/v1/rollouts/6"
        }
      },
      "id" : 6
    }""")
public class MgmtRolloutResponseBody extends MgmtNamedEntity {

    @Schema(description = "Target filter query language expression", example = "controllerId==exampleTarget*")
    private String targetFilterQuery;

    @Schema(description = "The ID of distribution set of this rollout", example = "2")
    private Long distributionSetId;

    @JsonProperty(value = "id", required = true)
    @Schema(description = "Rollout id", example = "2")
    private Long rolloutId;

    @JsonProperty(required = true)
    @Schema(description = "The status of this rollout", example = "ready")
    private String status;

    @JsonProperty(required = true)
    @Schema(description = "The total targets of a rollout", example = "20")
    private Long totalTargets;

    @Setter(AccessLevel.NONE)
    @JsonProperty
    @Schema(description = "The total targets per status")
    private Map<String, Long> totalTargetsPerStatus;

    @JsonProperty
    @Schema(description = "The total number of groups created by this rollout", example = "5")
    private Integer totalGroups;

    @JsonProperty
    @Schema(description = "Start at timestamp of Rollout", example = "1691065753136")
    private Long startAt;

    @JsonProperty
    @Schema(description = "Forcetime in milliseconds", example = "1691065762496")
    private Long forcetime;

    @JsonProperty
    @Schema(description = "Deleted flag, used for soft deleted entities", example = "false")
    private boolean deleted;

    @JsonProperty
    @Schema(description = "The type of this rollout")
    private MgmtActionType type;

    @JsonProperty
    @Schema(description = "Weight of the resulting Actions", example = "400")
    private Integer weight;

    @JsonProperty
    @Schema(description = "If this rollout is dynamic or static", example = "true")
    private boolean dynamic;

    @JsonProperty
    @Schema(example = "Approved remark.")
    private String approvalRemark;

    @JsonProperty
    @Schema(example = "exampleUsername")
    private String approveDecidedBy;

    public void addTotalTargetsPerStatus(final String status, final Long totalTargetCountByStatus) {
        if (totalTargetsPerStatus == null) {
            totalTargetsPerStatus = new HashMap<>();
        }

        totalTargetsPerStatus.put(status, totalTargetCountByStatus);
    }
}