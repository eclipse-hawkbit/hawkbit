/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import java.io.Serializable;
import java.net.URI;
import java.security.SecureRandom;

import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;

/**
 * Proxy for {@link Target}.
 *
 */
/**
 * @author kaizimmerm
 *
 */
public class ProxyTarget implements Serializable {
    private static final long serialVersionUID = -8891449133620645310L;
    private String controllerId;
    private URI address = null;
    private Long lastTargetQuery = null;
    private Long installationDate;

    private Long id;

    private TargetUpdateStatus updateStatus = TargetUpdateStatus.UNKNOWN;

    private DistributionSet installedDistributionSet;

    private DistributionSet assignedDistributionSet;

    private TargetIdName targetIdName;

    private String assignedDistNameVersion;

    private String installedDistNameVersion;

    private String pollStatusToolTip;

    private String createdByUser;

    private String createdDate;

    private String lastModifiedDate;

    private String modifiedByUser;

    private Status status;

    private String name;

    private String description;

    private Long createdAt;

    private TargetInfo targetInfo;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }

    public TargetInfo getTargetInfo() {
        return targetInfo;
    }

    public void setTargetInfo(final TargetInfo targetInfo) {
        this.targetInfo = targetInfo;
    }

    /**
     * @param controllerId
     */
    public ProxyTarget() {
        final Integer generatedId = new SecureRandom().nextInt(Integer.MAX_VALUE) - Integer.MAX_VALUE;
        targetIdName = new TargetIdName(generatedId, generatedId.toString(), generatedId.toString());
    }

    /**
     * @return the createdByUser
     */
    public String getCreatedByUser() {
        return createdByUser;
    }

    /**
     * @param createdByUser
     *            the createdByUser to set
     */
    public void setCreatedByUser(final String createdByUser) {
        this.createdByUser = createdByUser;
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
     * @return the modifiedByUser
     */
    public String getModifiedByUser() {
        return modifiedByUser;
    }

    /**
     * @param modifiedByUser
     *            the modifiedByUser to set
     */
    public void setModifiedByUser(final String modifiedByUser) {
        this.modifiedByUser = modifiedByUser;
    }

    /**
     * @return the lastModifiedDate
     */
    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * @param lastModifiedDate
     *            the lastModifiedDate to set
     */
    public void setLastModifiedDate(final String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * @return the assignedDistNameVersion
     */
    public String getAssignedDistNameVersion() {
        return assignedDistNameVersion;
    }

    /**
     * @param assignedDistNameVersion
     *            the assignedDistNameVersion to set
     */
    public void setAssignedDistNameVersion(final String assignedDistNameVersion) {
        this.assignedDistNameVersion = assignedDistNameVersion;
    }

    /**
     * @return the installedDistNameVersion
     */
    public String getInstalledDistNameVersion() {
        return installedDistNameVersion;
    }

    /**
     * @param installedDistNameVersion
     *            the installedDistNameVersion to set
     */
    public void setInstalledDistNameVersion(final String installedDistNameVersion) {
        this.installedDistNameVersion = installedDistNameVersion;
    }

    /**
     * GET - ID.
     * 
     * @return String as ID.
     */
    public String getControllerId() {
        return controllerId;
    }

    /**
     * SET - ID.
     * 
     * @param controllerId
     *            as ID
     */
    public void setControllerId(final String controllerId) {
        this.controllerId = controllerId;
    }

    /**
     * @return the ipAddress
     */
    public URI getAddress() {
        return address;
    }

    /**
     * @param ipAddress
     *            the ipAddress to set
     */
    public void setAddress(final URI address) {
        this.address = address;
    }

    /**
     * @return the lastTargetQuery
     */
    public Long getLastTargetQuery() {
        return lastTargetQuery;
    }

    /**
     * @param lastTargetQuery
     *            the lastTargetQuery to set
     */
    public void setLastTargetQuery(final Long lastTargetQuery) {
        this.lastTargetQuery = lastTargetQuery;
    }

    /**
     * @return the installationDate
     */
    public Long getInstallationDate() {
        return installationDate;
    }

    /**
     * @param installationDate
     *            the installationDate to set
     */
    public void setInstallationDate(final Long installationDate) {
        this.installationDate = installationDate;
    }

    /**
     * @return the updateStatus
     */
    public TargetUpdateStatus getUpdateStatus() {
        return updateStatus;
    }

    /**
     * @param updateStatus
     *            the updateStatus to set
     */
    public void setUpdateStatus(final TargetUpdateStatus updateStatus) {
        this.updateStatus = updateStatus;
    }

    /**
     * @return the installedDistributionSet
     */
    public DistributionSet getInstalledDistributionSet() {
        return installedDistributionSet;
    }

    /**
     * @param installedDistributionSet
     *            the installedDistributionSet to set
     */
    public void setInstalledDistributionSet(final DistributionSet installedDistributionSet) {
        this.installedDistributionSet = installedDistributionSet;
    }

    /**
     * @return the assignedDistributionSet
     */
    public DistributionSet getAssignedDistributionSet() {
        return assignedDistributionSet;
    }

    /**
     * @param assignedDistributionSet
     *            the assignedDistributionSet to set
     */
    public void setAssignedDistributionSet(final DistributionSet assignedDistributionSet) {
        this.assignedDistributionSet = assignedDistributionSet;
    }

    /**
     * @return the targetIdName
     */
    public TargetIdName getTargetIdName() {
        return this.targetIdName;
    }

    /**
     * @param targetIdName
     *            the targetIdName to set
     */
    public void setTargetIdName(final TargetIdName targetIdName) {
        this.targetIdName = targetIdName;
    }

    /**
     * @return the pollStatusToolTip
     */
    public String getPollStatusToolTip() {
        return pollStatusToolTip;
    }

    /**
     * @param pollStatusToolTip
     *            the pollStatusToolTip to set
     */
    public void setPollStatusToolTip(final String pollStatusToolTip) {
        this.pollStatusToolTip = pollStatusToolTip;
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final Status status) {
        this.status = status;
    }

}
