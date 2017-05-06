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

import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;

/**
 * Proxy for {@link Target}.
 *
 */
public class ProxyTarget implements Serializable {
    private static final long serialVersionUID = -8891449133620645310L;
    private String controllerId;
    private URI address;
    private Long lastTargetQuery;
    private Long installationDate;

    private Long id;

    private TargetUpdateStatus updateStatus = TargetUpdateStatus.UNKNOWN;

    private DistributionSet installedDistributionSet;

    private DistributionSet assignedDistributionSet;

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

    public String getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(final String createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(final String createdDate) {
        this.createdDate = createdDate;
    }

    public String getModifiedByUser() {
        return modifiedByUser;
    }

    public void setModifiedByUser(final String modifiedByUser) {
        this.modifiedByUser = modifiedByUser;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(final String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getAssignedDistNameVersion() {
        return assignedDistNameVersion;
    }

    public void setAssignedDistNameVersion(final String assignedDistNameVersion) {
        this.assignedDistNameVersion = assignedDistNameVersion;
    }

    public String getInstalledDistNameVersion() {
        return installedDistNameVersion;
    }

    public void setInstalledDistNameVersion(final String installedDistNameVersion) {
        this.installedDistNameVersion = installedDistNameVersion;
    }

    public String getControllerId() {
        return controllerId;
    }

    public void setControllerId(final String controllerId) {
        this.controllerId = controllerId;
    }

    public URI getAddress() {
        return address;
    }

    public void setAddress(final URI address) {
        this.address = address;
    }

    public Long getLastTargetQuery() {
        return lastTargetQuery;
    }

    public void setLastTargetQuery(final Long lastTargetQuery) {
        this.lastTargetQuery = lastTargetQuery;
    }

    public Long getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(final Long installationDate) {
        this.installationDate = installationDate;
    }

    public TargetUpdateStatus getUpdateStatus() {
        return updateStatus;
    }

    public void setUpdateStatus(final TargetUpdateStatus updateStatus) {
        this.updateStatus = updateStatus;
    }

    public DistributionSet getInstalledDistributionSet() {
        return installedDistributionSet;
    }

    public void setInstalledDistributionSet(final DistributionSet installedDistributionSet) {
        this.installedDistributionSet = installedDistributionSet;
    }

    public DistributionSet getAssignedDistributionSet() {
        return assignedDistributionSet;
    }

    public void setAssignedDistributionSet(final DistributionSet assignedDistributionSet) {
        this.assignedDistributionSet = assignedDistributionSet;
    }

    public String getPollStatusToolTip() {
        return pollStatusToolTip;
    }

    public void setPollStatusToolTip(final String pollStatusToolTip) {
        this.pollStatusToolTip = pollStatusToolTip;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

}
