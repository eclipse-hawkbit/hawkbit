/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.util.Map;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus.Status;

/**
 * Proxy for {@link Rollout} with custom properties.
 */
public class ProxyRollout extends ProxyNamedEntity {

    private static final long serialVersionUID = 1L;

    private String distributionSetNameVersion;

    private Integer numberOfGroups;

    private long totalTargets;

    private String targetFilterQuery;

    private Long forcedTime;

    private RolloutStatus status;

    private Map<Status, Long> statusTotalCountMap;

    private String approvalDecidedBy;

    private String approvalRemark;

    private Long startAt;

    private ActionType actionType;

    private Long distributionSetId;

    /**
     *  Constructor
     */
    public ProxyRollout() {
    }

    /**
     * Constructor for ProxyRollout
     *
     * @param id
     *         Rollout entity Id
     */
    public ProxyRollout(final Long id) {
        super(id);
    }

    /**
     * Gets the rollout actionType
     *
     * @return actionType
     */
    public ActionType getActionType() {
        return actionType;
    }

    /**
     * Sets the actionType
     *
     * @param actionType
     *          Rollout actionType
     */
    public void setActionType(final ActionType actionType) {
        this.actionType = actionType;
    }

    /**
     * Get the distributionSetNameVersion
     *
     * @return distributionSetNameVersion
     */
    public String getDistributionSetNameVersion() {
        return distributionSetNameVersion;
    }

    /**
     * Sets the distributionSetNameVersion
     * @param distributionSetNameVersion
     *          Name and version of distribution set
     */
    public void setDistributionSetNameVersion(final String distributionSetNameVersion) {
        this.distributionSetNameVersion = distributionSetNameVersion;
    }

    /**
     * Gets the numberOfGroups
     *
     * @return numberOfGroups
     */
    public Integer getNumberOfGroups() {
        return numberOfGroups;
    }

    /**
     * Sets the numberOfGroups
     *
     * @param numberOfGroups
     *          Number of rollout groups
     */
    public void setNumberOfGroups(final Integer numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    /**
     * Gets the forcedTime
     *
     * @return forcedTime
     */
    public Long getForcedTime() {
        return forcedTime;
    }

    /**
     * Sets the forcedTime
     *
     * @param forcedTime
     *          Forced time
     */
    public void setForcedTime(final Long forcedTime) {
        this.forcedTime = forcedTime;
    }

    /**
     * Gets the rolloutStatus
     *
     * @return Rollout current status
     */
    public RolloutStatus getStatus() {
        return status;
    }

    /**
     * Sets the rollout status
     *
     * @param status
     *          Rollout current status
     */
    public void setStatus(final RolloutStatus status) {
        this.status = status;
    }

    /**
     * Gets the approval DecidedBy for rollout
     *
     * @return approvalDecidedBy
     */
    public String getApprovalDecidedBy() {
        return approvalDecidedBy;
    }

    /**
     * Sets the approvalDecidedBy
     *
     * @param approvalDecidedBy
     *          approval DecidedBy for rollout
     */
    public void setApprovalDecidedBy(final String approvalDecidedBy) {
        this.approvalDecidedBy = approvalDecidedBy;
    }

    /**
     * Gets the approvalRemark
     *
     * @return approvalRemark
     */
    public String getApprovalRemark() {
        return approvalRemark;
    }

    /**
     * Sets the approvalRemark
     *
     * @param approvalRemark
     *          Remark for approval
     */
    public void setApprovalRemark(final String approvalRemark) {
        this.approvalRemark = approvalRemark;
    }

    /**
     * Gets the targetFilterQuery
     *
     * @return targetFilterQuery
     */
    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    /**
     * Sets the targetFilterQuery
     *
     * @param targetFilterQuery
     *          Target filter query
     */
    public void setTargetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

    /**
     * Gets the total targets
     *
     * @return totalTargets
     */
    public long getTotalTargets() {
        return totalTargets;
    }

    /**
     * Sets the totalTargets
     *
     * @param totalTargets
     *          Total targets
     */
    public void setTotalTargets(final long totalTargets) {
        this.totalTargets = totalTargets;
    }

    /**
     * Gets the time rollout starts at
     *
     * @return startAt
     */
    public Long getStartAt() {
        return startAt;
    }

    /**
     * Sets the startAt
     *
     * @param startAt
     *          time rollout starts at
     */
    public void setStartAt(final Long startAt) {
        this.startAt = startAt;
    }

    /**
     * Gets the Id of distribution set
     *
     * @return distributionSetId
     */
    public Long getDistributionSetId() {
        return distributionSetId;
    }

    /**
     * Sets the distributionSetId
     *
     * @param distributionSetId
 *              Id of distribution set
     */
    public void setDistributionSetId(final Long distributionSetId) {
        this.distributionSetId = distributionSetId;
    }

    /**
     * Gets the total count of all the rollout status
     *
     * @return statusTotalCountMap
     */
    public Map<Status, Long> getStatusTotalCountMap() {
        return statusTotalCountMap;
    }

    /**
     * Sets the statusTotalCountMap
     *
     * @param statusTotalCountMap
     *          total count of all the rollout status
     */
    public void setStatusTotalCountMap(final Map<Status, Long> statusTotalCountMap) {
        this.statusTotalCountMap = statusTotalCountMap;
    }
}
