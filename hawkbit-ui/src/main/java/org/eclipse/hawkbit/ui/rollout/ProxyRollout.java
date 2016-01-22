/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * Proxy rollout with suctome properties.
 *
 */
public class ProxyRollout extends Rollout {

    private static final long serialVersionUID = 4539849939617681918L;

    private String distributionSetNameVersion;

    // TODO currently not used .Cross check if target filter query to be
    // displayed
    private String targetFilterQueryName;

    private String createdDate;

    private String modifiedDate;

    private String createdBy;

    private String lastModifiedBy;

    private Integer numberOfGroups;

    private Long runningTargetsCount;

    private Long scheduledTargetsCount;

    private Long cancelledTargetsCount;

    private Long errorTargetsCount;

    private Long finishedTargetsCount;

    private Long notStartedTargetsCount;

    private Boolean isActionRecieved;

    /**
     * @return the distributionSetNameVersion
     */
    public String getDistributionSetNameVersion() {
        return distributionSetNameVersion;
    }

    /**
     * @param distributionSetNameVersion
     *            the distributionSetNameVersion to set
     */
    public void setDistributionSetNameVersion(final String distributionSetNameVersion) {
        this.distributionSetNameVersion = distributionSetNameVersion;
    }

    /**
     * @return the targetFilterQueryName
     */
    public String getTargetFilterQueryName() {
        return targetFilterQueryName;
    }

    /**
     * @param targetFilterQueryName
     *            the targetFilterQueryName to set
     */
    public void setTargetFilterQueryName(final String targetFilterQueryName) {
        this.targetFilterQueryName = targetFilterQueryName;
    }

    /**
     * @return the numberOfGroups
     */
    public int getNumberOfGroups() {
        return numberOfGroups;
    }

    /**
     * @param numberOfGroups
     *            the numberOfGroups to set
     */
    public void setNumberOfGroups(final Integer numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
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
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy
     *            the createdBy to set
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the lastModifiedBy
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * @param lastModifiedBy
     *            the lastModifiedBy to set
     */
    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
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

}
