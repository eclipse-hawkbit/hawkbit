/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.systemmanagement;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body for system usage report.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSystemTenantServiceUsage {

    @JsonProperty
    private String tenantName;

    @JsonProperty
    private long targets;

    @JsonProperty
    private long artifacts;

    @JsonProperty
    private long actions;

    @JsonProperty
    private long overallArtifactVolumeInBytes;

    @JsonProperty
    private Map<String, String> usageData;

    public void setTenantName(final String tenantName) {
        this.tenantName = tenantName;
    }

    public long getTargets() {
        return targets;
    }

    public void setTargets(final long targets) {
        this.targets = targets;
    }

    public long getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(final long artifacts) {
        this.artifacts = artifacts;
    }

    public long getActions() {
        return actions;
    }

    public void setActions(final long actions) {
        this.actions = actions;
    }

    public long getOverallArtifactVolumeInBytes() {
        return overallArtifactVolumeInBytes;
    }

    public void setOverallArtifactVolumeInBytes(final long overallArtifactVolumeInBytes) {
        this.overallArtifactVolumeInBytes = overallArtifactVolumeInBytes;
    }

    public String getTenantName() {
        return tenantName;
    }

    public Map<String, String> getUsageData() {
        return usageData;
    }

    public void setUsageData(final Map<String, String> usageData) {
        this.usageData = usageData;
    }

}
