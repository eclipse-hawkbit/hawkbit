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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtDynamicRolloutGroupTemplate;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroup;

/**
 * Model for request containing a rollout body e.g. in a POST request of
 * creating a rollout via REST API.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(example = """
        {
          "distributionSetId" : 6,
          "targetFilterQuery" : "id==targets-*",
          "description" : "Rollout for all named targets",
          "amountGroups" : 5,
          "type" : "forced",
          "successCondition" : {
            "condition" : "THRESHOLD",
            "expression" : "50"
          },
          "successAction" : {
            "expression" : "",
            "action" : "NEXTGROUP"
          },
          "name" : "exampleRollout",
          "forcetime" : 1682408571791,
          "errorAction" : {
            "expression" : "",
            "action" : "PAUSE"
          },
          "confirmationRequired" : false,
          "errorCondition" : {
            "condition" : "THRESHOLD",
            "expression" : "80"
          },
          "startAt" : 1682408570791
        }""")
public class MgmtRolloutRestRequestBodyPost extends AbstractMgmtRolloutConditionsEntity {

    @Schema(description = "Target filter query language expression", example = "id==targets-*")
    @JsonProperty(required = true)
    private String targetFilterQuery;

    @Schema(description = "The ID of distribution set of this rollout", example = "6")
    @JsonProperty(required = true)
    private long distributionSetId;

    @Schema(description = "The amount of groups the rollout should split targets into", example = "5", defaultValue = "1")
    private Integer amountGroups;

    @Schema(description = "Force time in milliseconds", example = "1691065781929")
    private Long forcetime;

    @Schema(description = "Start at timestamp of Rollout", example = "1691065780929")
    private Long startAt;

    @Schema(description = "Weight of the resulting Actions", example = "400")
    private Integer weight;

    @Schema(example = "true")
    private boolean dynamic;

    @Schema(description = "Template for dynamic groups (only if dynamic flag is true)")
    private MgmtDynamicRolloutGroupTemplate dynamicGroupTemplate;

    @Schema(description = """
            (Available with user consent flow active) If the confirmation is required for this rollout. Value will be used
            if confirmation options are missing in the rollout group definitions. Confirmation is required per default""",
            example = "false")
    private Boolean confirmationRequired;

    @Schema(description = "The type of this rollout")
    private MgmtActionType type;

    @Schema(description = "The list of group definitions")
    private List<MgmtRolloutGroup> groups;
}