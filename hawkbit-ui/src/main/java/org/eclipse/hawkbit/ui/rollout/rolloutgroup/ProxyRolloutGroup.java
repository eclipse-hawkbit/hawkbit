/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgroup;

import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.ui.customrenderers.client.renderers.RolloutRendererData;

/**
 * Proxy rollout group with renderer properties.
 *
 */
public class ProxyRolloutGroup {

    private static final long serialVersionUID = -2745056813306692356L;

    private String createdDate;

    private String modifiedDate;

    private String finishedPercentage;

    private Long runningTargetsCount;

    private Long scheduledTargetsCount;

    private Long cancelledTargetsCount;

    private Long errorTargetsCount;

    private Long finishedTargetsCount;

    private Long notStartedTargetsCount;

    private Boolean isActionRecieved = Boolean.FALSE;

    private String totalTargetsCount;

    private Long id;
    private String name;
    private String description;
    private String createdBy;
    private String lastModifiedBy;
    private RolloutGroupStatus status;
    private TotalTargetCountStatus totalTargetCountStatus;

    private RolloutGroupSuccessCondition successCondition;
    private String successConditionExp;
    private RolloutGroupSuccessAction successAction;
    private String successActionExp;
    private RolloutGroupErrorCondition errorCondition;
    private String errorConditionExp;
    private RolloutGroupErrorAction errorAction;
    private String errorActionExp;

    private RolloutRendererData rolloutRendererData;

    public RolloutRendererData getRolloutRendererData() {
        return rolloutRendererData;
    }

    public void setRolloutRendererData(final RolloutRendererData rendererData) {
        this.rolloutRendererData = rendererData;
    }

    /**
     * @return the createdDate
     */
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     * @param createdDate
     *            the createdDate to set
     */
    public void setCreatedDate(final String createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @return the modifiedDate
     */
    public String getModifiedDate() {
        return modifiedDate;
    }

    /**
     * @param modifiedDate
     *            the modifiedDate to set
     */
    public void setModifiedDate(final String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * @return the finishedPercentage
     */
    public String getFinishedPercentage() {
        return finishedPercentage;
    }

    /**
     * @param finishedPercentage
     *            the finishedPercentage to set
     */
    public void setFinishedPercentage(final String finishedPercentage) {
        this.finishedPercentage = finishedPercentage;
    }

    /**
     * @return the runningTargetsCount
     */
    public Long getRunningTargetsCount() {
        return runningTargetsCount;
    }

    /**
     * @param runningTargetsCount
     *            the runningTargetsCount to set
     */
    public void setRunningTargetsCount(final Long runningTargetsCount) {
        this.runningTargetsCount = runningTargetsCount;
    }

    /**
     * @return the scheduledTargetsCount
     */
    public Long getScheduledTargetsCount() {
        return scheduledTargetsCount;
    }

    /**
     * @param scheduledTargetsCount
     *            the scheduledTargetsCount to set
     */
    public void setScheduledTargetsCount(final Long scheduledTargetsCount) {
        this.scheduledTargetsCount = scheduledTargetsCount;
    }

    /**
     * @return the cancelledTargetsCount
     */
    public Long getCancelledTargetsCount() {
        return cancelledTargetsCount;
    }

    /**
     * @param cancelledTargetsCount
     *            the cancelledTargetsCount to set
     */
    public void setCancelledTargetsCount(final Long cancelledTargetsCount) {
        this.cancelledTargetsCount = cancelledTargetsCount;
    }

    /**
     * @return the errorTargetsCount
     */
    public Long getErrorTargetsCount() {
        return errorTargetsCount;
    }

    /**
     * @param errorTargetsCount
     *            the errorTargetsCount to set
     */
    public void setErrorTargetsCount(final Long errorTargetsCount) {
        this.errorTargetsCount = errorTargetsCount;
    }

    /**
     * @return the finishedTargetsCount
     */
    public Long getFinishedTargetsCount() {
        return finishedTargetsCount;
    }

    /**
     * @param finishedTargetsCount
     *            the finishedTargetsCount to set
     */
    public void setFinishedTargetsCount(final Long finishedTargetsCount) {
        this.finishedTargetsCount = finishedTargetsCount;
    }

    /**
     * @return the notStartedTargetsCount
     */
    public Long getNotStartedTargetsCount() {
        return notStartedTargetsCount;
    }

    /**
     * @param notStartedTargetsCount
     *            the notStartedTargetsCount to set
     */
    public void setNotStartedTargetsCount(final Long notStartedTargetsCount) {
        this.notStartedTargetsCount = notStartedTargetsCount;
    }

    /**
     * @return the isActionRecieved
     */
    public Boolean getIsActionRecieved() {
        return isActionRecieved;
    }

    /**
     * @param isActionRecieved
     *            the isActionRecieved to set
     */
    public void setIsActionRecieved(final Boolean isActionRecieved) {
        this.isActionRecieved = isActionRecieved;
    }

    /**
     * @return the totalTargetsCount
     */
    public String getTotalTargetsCount() {
        return totalTargetsCount;
    }

    /**
     * @param totalTargetsCount
     *            the totalTargetsCount to set
     */
    public void setTotalTargetsCount(final String totalTargetsCount) {
        this.totalTargetsCount = totalTargetsCount;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public RolloutGroupStatus getStatus() {
        return status;
    }

    public void setStatus(final RolloutGroupStatus status) {
        this.status = status;
    }

    public TotalTargetCountStatus getTotalTargetCountStatus() {
        return totalTargetCountStatus;
    }

    public void setTotalTargetCountStatus(final TotalTargetCountStatus totalTargetCountStatus) {
        this.totalTargetCountStatus = totalTargetCountStatus;
    }

    public RolloutGroupSuccessCondition getSuccessCondition() {
        return successCondition;
    }

    public void setSuccessCondition(final RolloutGroupSuccessCondition successCondition) {
        this.successCondition = successCondition;
    }

    public String getSuccessConditionExp() {
        return successConditionExp;
    }

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

    public void setErrorCondition(final RolloutGroupErrorCondition errorCondition) {
        this.errorCondition = errorCondition;
    }

    public String getErrorConditionExp() {
        return errorConditionExp;
    }

    public void setErrorConditionExp(final String errorConditionExp) {
        this.errorConditionExp = errorConditionExp;
    }

    public RolloutGroupErrorAction getErrorAction() {
        return errorAction;
    }

    public void setErrorAction(final RolloutGroupErrorAction errorAction) {
        this.errorAction = errorAction;
    }

    public String getErrorActionExp() {
        return errorActionExp;
    }

    public void setErrorActionExp(final String errorActionExp) {
        this.errorActionExp = errorActionExp;
    }

}
