/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.systemmanagement;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Body for system statistics.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSystemStatisticsRest {

    private long overallTargets;
    private long overallArtifacts;
    private long overallArtifactVolumeInBytes;
    private long overallActions;
    private long overallTenants;

    private List<MgmtSystemTenantServiceUsage> tenantStats;

    public long getOverallTargets() {
        return overallTargets;
    }

    public MgmtSystemStatisticsRest setOverallTargets(final long overallTargets) {
        this.overallTargets = overallTargets;
        return this;
    }

    public long getOverallArtifacts() {
        return overallArtifacts;
    }

    public MgmtSystemStatisticsRest setOverallArtifacts(final long overallArtifacts) {
        this.overallArtifacts = overallArtifacts;
        return this;
    }

    public long getOverallArtifactVolumeInBytes() {
        return overallArtifactVolumeInBytes;
    }

    public MgmtSystemStatisticsRest setOverallArtifactVolumeInBytes(final long overallArtifactVolumeInBytes) {
        this.overallArtifactVolumeInBytes = overallArtifactVolumeInBytes;
        return this;
    }

    public long getOverallActions() {
        return overallActions;
    }

    public MgmtSystemStatisticsRest setOverallActions(final long overallActions) {
        this.overallActions = overallActions;
        return this;
    }

    public long getOverallTenants() {
        return overallTenants;
    }

    public MgmtSystemStatisticsRest setOverallTenants(final long overallTenants) {
        this.overallTenants = overallTenants;
        return this;
    }

    public void setTenantStats(final List<MgmtSystemTenantServiceUsage> tenantStats) {
        this.tenantStats = tenantStats;
    }

    public List<MgmtSystemTenantServiceUsage> getTenantStats() {
        return tenantStats;
    }

}
