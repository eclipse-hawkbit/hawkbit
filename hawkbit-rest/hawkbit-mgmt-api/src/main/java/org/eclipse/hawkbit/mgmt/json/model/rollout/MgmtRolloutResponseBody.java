/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.rollout;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;

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

    private String targetFilterQuery;
    private Long distributionSetId;

    @JsonProperty(value = "id", required = true)
    private Long rolloutId;

    @JsonProperty(required = true)
    private String status;

    @JsonProperty(required = true)
    private Long totalTargets;

    @JsonProperty
    private Map<String, Long> totalTargetsPerStatus;

    @JsonProperty
    private boolean deleted;

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

    public Map<String, Long> getTotalTargetsPerStatus() {
        return totalTargetsPerStatus;
    }

    public void addTotalTargetsPerStatus(final String status, final Long totalTargetCountByStatus) {
        if (totalTargetsPerStatus == null) {
            totalTargetsPerStatus = new HashMap<>();
        }

        totalTargetsPerStatus.put(status, totalTargetCountByStatus);
    }
}
