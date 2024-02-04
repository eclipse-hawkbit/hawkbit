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
public class MgmtRolloutResponseBody extends MgmtNamedEntity {

    @Schema(example = "controllerId==exampleTarget*")
    private String targetFilterQuery;
    @Schema(example = "2")
    private Long distributionSetId;
    @JsonProperty(value = "id", required = true)
    @Schema(example = "2")
    private Long rolloutId;
    @JsonProperty(required = true)
    @Schema(example = "ready")
    private String status;
    @JsonProperty(required = true)
    @Schema(example = "20")
    private Long totalTargets;
    @Setter(AccessLevel.NONE)
    @JsonProperty
    private Map<String, Long> totalTargetsPerStatus;
    @JsonProperty
    @Schema(example = "5")
    private Integer totalGroups;
    @JsonProperty
    @Schema(example = "1691065753136")
    private Long startAt;
    @JsonProperty
    @Schema(example = "1691065762496")
    private Long forcetime;
    @JsonProperty
    @Schema(example = "false")
    private boolean deleted;
    @JsonProperty
    private MgmtActionType type;
    @JsonProperty
    @Schema(example = "400")
    private Integer weight;
    @JsonProperty
    @Schema(example = "true")
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
