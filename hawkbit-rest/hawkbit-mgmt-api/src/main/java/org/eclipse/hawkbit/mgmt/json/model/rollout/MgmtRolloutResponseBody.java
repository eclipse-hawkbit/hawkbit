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
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Long getRolloutId() {
        return rolloutId;
    }

    public void setRolloutId(final Long rolloutId) {
        this.rolloutId = rolloutId;
    }

    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    public void setTargetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

    public Long getDistributionSetId() {
        return distributionSetId;
    }

    public void setDistributionSetId(final Long distributionSetId) {
        this.distributionSetId = distributionSetId;
    }

    public void setTotalTargets(final Long totalTargets) {
        this.totalTargets = totalTargets;
    }

    public Long getTotalTargets() {
        return totalTargets;
    }

    public void setStartAt(final Long startAt) {
        this.startAt = startAt;
    }

    public Long getStartAt() {
        return startAt;
    }

    public void setForcetime(final Long forcetime) {
        this.forcetime = forcetime;
    }

    public Long getForcetime() {
        return forcetime;
    }

    public Map<String, Long> getTotalTargetsPerStatus() {
        return totalTargetsPerStatus;
    }

    public void addTotalTargetsPerStatus(final String status, final Long totalTargetCountByStatus) {
        if (totalTargetsPerStatus == null) {
            totalTargetsPerStatus = new HashMap<>();
        }

        totalTargetsPerStatus.put(status, totalTargetCountByStatus);
    }

    public void setType(final MgmtActionType type) {
        this.type = type;
    }

    public MgmtActionType getType() {
        return type;
    }

    public void setWeight(final Integer weight) {
        this.weight = weight;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setDynamic(final boolean dynamic) {
        this.dynamic = dynamic;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setTotalGroups(final Integer totalGroups) {
        this.totalGroups = totalGroups;
    }

    public Integer getTotalGroups() {
        return totalGroups;
    }

    public void setApprovalRemark(final String approvalRemark) {
        this.approvalRemark = approvalRemark;
    }

    public String getApprovalRemark() {
        return approvalRemark;
    }

    public void setApproveDecidedBy(final String approveDecidedBy) {
        this.approveDecidedBy = approveDecidedBy;
    }

    public String getApproveDecidedBy(final String approveDecidedBy) {
        return approveDecidedBy;
    }
}
