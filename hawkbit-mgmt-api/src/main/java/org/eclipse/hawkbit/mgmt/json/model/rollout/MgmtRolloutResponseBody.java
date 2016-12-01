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

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * @return the rolloutId
     */
    public Long getRolloutId() {
        return rolloutId;
    }

    /**
     * @param rolloutId
     *            the rolloutId to set
     */
    public void setRolloutId(final Long rolloutId) {
        this.rolloutId = rolloutId;
    }

    /**
     * @return the targetFilterQuery
     */
    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    /**
     * @param targetFilterQuery
     *            the targetFilterQuery to set
     */
    public void setTargetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

    /**
     * @return the distributionSetId
     */
    public Long getDistributionSetId() {
        return distributionSetId;
    }

    /**
     * @param distributionSetId
     *            the distributionSetId to set
     */
    public void setDistributionSetId(final Long distributionSetId) {
        this.distributionSetId = distributionSetId;
    }

    /**
     * @param totalTargets
     *            the totalTargets to set
     */
    public void setTotalTargets(final Long totalTargets) {
        this.totalTargets = totalTargets;
    }

    /**
     * @return the totalTargets
     */
    public Long getTotalTargets() {
        return totalTargets;
    }

    /**
     * @return the totalTargetsPerStatus
     */
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
