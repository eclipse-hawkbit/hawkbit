/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.rolloutgroup;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Model for the rollout group annotated with json-annotations for easier
 * serialization and de-serialization.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(example = """
    {
      "createdBy" : "bumlux",
      "createdAt" : 1682408569768,
      "lastModifiedBy" : "bumlux",
      "lastModifiedAt" : 1682408569795,
      "name" : "group-1",
      "description" : "group-1",
      "successCondition" : {
        "condition" : "THRESHOLD",
        "expression" : "10"
      },
      "successAction" : {
        "action" : "NEXTGROUP",
        "expression" : ""
      },
      "errorCondition" : {
        "condition" : "THRESHOLD",
        "expression" : "50"
      },
      "errorAction" : {
        "action" : "PAUSE",
        "expression" : ""
      },
      "targetFilterQuery" : "",
      "targetPercentage" : 20.0,
      "confirmationRequired" : false,
      "status" : "ready",
      "totalTargets" : 4,
      "totalTargetsPerStatus" : {
        "running" : 0,
        "notstarted" : 4,
        "scheduled" : 0,
        "cancelled" : 0,
        "finished" : 0,
        "error" : 0
      },
      "_links" : {
        "self" : {
          "href" : "https://management-api.host.com/rest/v1/rollouts/17/deploygroups/78"
        }
      },
      "id" : 78
    }""")
public class MgmtRolloutGroupResponseBody extends MgmtRolloutGroup {

    @JsonProperty(value = "id", required = true)
    @Schema(description = "Rollouts id", example = "63")
    private Long rolloutGroupId;

    @JsonProperty(required = true)
    @Schema(description = "The status of this rollout", example = "ready")
    private String status;

    @Schema(description = "The total targets of a rollout", example = "4")
    private int totalTargets;

    @Setter(AccessLevel.NONE)
    @Schema(description = "The total targets per status")
    private Map<String, Long> totalTargetsPerStatus;

    public void addTotalTargetsPerStatus(final String status, final Long totalTargetCountByStatus) {
        if (totalTargetsPerStatus == null) {
            totalTargetsPerStatus = new HashMap<>();
        }

        totalTargetsPerStatus.put(status, totalTargetCountByStatus);
    }
}