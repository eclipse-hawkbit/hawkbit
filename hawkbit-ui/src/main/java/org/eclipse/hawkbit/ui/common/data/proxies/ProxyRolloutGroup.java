/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;

/**
 * Proxy for {@link RolloutGroup} with custom properties.
 */
public class ProxyRolloutGroup extends ProxyNamedEntity {

    private static final long serialVersionUID = 1L;

    private String finishedPercentage;

    private Long runningTargetsCount;

    private Long scheduledTargetsCount;

    private Long cancelledTargetsCount;

    private Long errorTargetsCount;

    private Long finishedTargetsCount;

    private Long notStartedTargetsCount;

    private Boolean isActionRecieved = Boolean.FALSE;

    private String totalTargetsCount;

    private RolloutGroupStatus status;

    private transient TotalTargetCountStatus totalTargetCountStatus;

    private RolloutGroupSuccessCondition successCondition;
    private String successConditionExp;
    private RolloutGroupSuccessAction successAction;
    private String successActionExp;
    private RolloutGroupErrorCondition errorCondition;
    private String errorConditionExp;
    private RolloutGroupErrorAction errorAction;
    private String errorActionExp;

    /**
     * Constructor
     */
    public ProxyRolloutGroup() {
    }

    /**
     * Constructor for ProxyRolloutGroup
     *
     * @param id
     *         Rollout group Id
     */
    public ProxyRolloutGroup(final Long id) {
        super(id);
    }

    /**
     * Gets the rollout finished percentage
     *
     * @return finishedPercentage
     */
    public String getFinishedPercentage() {
        return finishedPercentage;
    }

    /**
     * Sets the finishedPercentage
     *
     * @param finishedPercentage
     *          rollout finished percentage
     */
    public void setFinishedPercentage(final String finishedPercentage) {
        this.finishedPercentage = finishedPercentage;
    }

    public Long getRunningTargetsCount() {
        return runningTargetsCount;
    }

    public void setRunningTargetsCount(final Long runningTargetsCount) {
        this.runningTargetsCount = runningTargetsCount;
    }

    public Long getScheduledTargetsCount() {
        return scheduledTargetsCount;
    }

    public void setScheduledTargetsCount(final Long scheduledTargetsCount) {
        this.scheduledTargetsCount = scheduledTargetsCount;
    }

    public Long getCancelledTargetsCount() {
        return cancelledTargetsCount;
    }

    public void setCancelledTargetsCount(final Long cancelledTargetsCount) {
        this.cancelledTargetsCount = cancelledTargetsCount;
    }

    public Long getErrorTargetsCount() {
        return errorTargetsCount;
    }

    public void setErrorTargetsCount(final Long errorTargetsCount) {
        this.errorTargetsCount = errorTargetsCount;
    }

    public Long getFinishedTargetsCount() {
        return finishedTargetsCount;
    }

    public void setFinishedTargetsCount(final Long finishedTargetsCount) {
        this.finishedTargetsCount = finishedTargetsCount;
    }

    public Long getNotStartedTargetsCount() {
        return notStartedTargetsCount;
    }

    public void setNotStartedTargetsCount(final Long notStartedTargetsCount) {
        this.notStartedTargetsCount = notStartedTargetsCount;
    }

    public Boolean getIsActionRecieved() {
        return isActionRecieved;
    }

    public void setIsActionRecieved(final Boolean isActionRecieved) {
        this.isActionRecieved = isActionRecieved;
    }

    /**
     * Gets the count of total targets
     *
     * @return totalTargetsCount
     */
    public String getTotalTargetsCount() {
        return totalTargetsCount;
    }

    /**
     * Sets the totalTargetsCount
     *
     * @param totalTargetsCount
     *          count of total targets
     */
    public void setTotalTargetsCount(final String totalTargetsCount) {
        this.totalTargetsCount = totalTargetsCount;
    }

    /**
     * Gets the status of rollout group assigned
     *
     * @return Rollout group status
     */
    public RolloutGroupStatus getStatus() {
        return status;
    }

    /**
     * Sets the Rollout group status
     *
     * @param status
     *          status of rollout group assigned
     */
    public void setStatus(final RolloutGroupStatus status) {
        this.status = status;
    }

    /**
     * Gets the totalTargetCountStatus
     *
     * @return totalTargetCountStatus
     */
    public TotalTargetCountStatus getTotalTargetCountStatus() {
        return totalTargetCountStatus;
    }

    /**
     * Sets the totalTargetCountStatus
     *
     * @param totalTargetCountStatus
     *          TotalTargetCountStatus
     */
    public void setTotalTargetCountStatus(final TotalTargetCountStatus totalTargetCountStatus) {
        this.totalTargetCountStatus = totalTargetCountStatus;
    }

    public RolloutGroupSuccessCondition getSuccessCondition() {
        return successCondition;
    }

    /**
     * Sets the rollout group successCondition
     *
     * @param successCondition
     *          RolloutGroupSuccessCondition
     */
    public void setSuccessCondition(final RolloutGroupSuccessCondition successCondition) {
        this.successCondition = successCondition;
    }

    /**
     * Gets the successConditionExp
     *
     * @return successConditionExp
     */
    public String getSuccessConditionExp() {
        return successConditionExp;
    }

    /**
     * Sets the successConditionExp
     *
     * @param successConditionExp
     *          RolloutGroup successConditionExp
     */
    public void setSuccessConditionExp(final String successConditionExp) {
        this.successConditionExp = successConditionExp;
    }

    public RolloutGroupSuccessAction getSuccessAction() {
        return successAction;
    }

    public void setSuccessAction(final RolloutGroupSuccessAction successAction) {
        this.successAction = successAction;
    }

    public String getSuccessActionExp() {
        return successActionExp;
    }

    public void setSuccessActionExp(final String successActionExp) {
        this.successActionExp = successActionExp;
    }

    public RolloutGroupErrorCondition getErrorCondition() {
        return errorCondition;
    }

    /**
     * Sets the rollout group errorCondition
     * @param errorCondition
     *          RolloutGroupErrorCondition
     */
    public void setErrorCondition(final RolloutGroupErrorCondition errorCondition) {
        this.errorCondition = errorCondition;
    }

    /**
     * Gets the errorConditionExp
     *
     * @return errorConditionExp
     */
    public String getErrorConditionExp() {
        return errorConditionExp;
    }

    /**
     * Sets the errorConditionExp
     *
     * @param errorConditionExp
     *          RolloutGroup errorConditionExp
     */
    public void setErrorConditionExp(final String errorConditionExp) {
        this.errorConditionExp = errorConditionExp;
    }

    public RolloutGroupErrorAction getErrorAction() {
        return errorAction;
    }

    /**
     * Sets the errorAction
     *
     * @param errorAction
     *          RolloutGroupErrorAction
     */
    public void setErrorAction(final RolloutGroupErrorAction errorAction) {
        this.errorAction = errorAction;
    }

    public String getErrorActionExp() {
        return errorActionExp;
    }

    /**
     * Sets the errorActionExp
     *
     * @param errorActionExp
     *          RolloutGroup errorActionExp
     */
    public void setErrorActionExp(final String errorActionExp) {
        this.errorActionExp = errorActionExp;
    }

}
