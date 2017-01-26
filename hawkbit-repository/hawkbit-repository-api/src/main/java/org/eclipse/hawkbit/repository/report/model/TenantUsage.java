/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.report.model;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * System usage stats element for a tenant.
 *
 */
public class TenantUsage {

    private final String tenantName;
    private long targets;
    private long artifacts;
    private long actions;
    private long overallArtifactVolumeInBytes;
    private Map<String, String> usageData;

    /**
     * Constructor.
     *
     * @param tenantName
     */
    public TenantUsage(final String tenantName) {
        super();
        this.tenantName = tenantName;
    }

    public String getTenantName() {
        return tenantName;
    }

    public long getTargets() {
        return targets;
    }

    public TenantUsage setTargets(final long targets) {
        this.targets = targets;
        return this;
    }

    public long getArtifacts() {
        return artifacts;
    }

    public Map<String, String> getUsageData() {
        if (usageData == null) {
            return Maps.newHashMap();
        }
        return usageData;
    }

    public TenantUsage setArtifacts(final long artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    public long getOverallArtifactVolumeInBytes() {
        return overallArtifactVolumeInBytes;
    }

    public TenantUsage setOverallArtifactVolumeInBytes(final long overallArtifactVolumeInBytes) {
        this.overallArtifactVolumeInBytes = overallArtifactVolumeInBytes;
        return this;
    }

    public long getActions() {
        return actions;
    }

    public TenantUsage setActions(final long actions) {
        this.actions = actions;
        return this;
    }

    /**
     * Add a key and value as usage data to the system usage stats.
     * 
     * @param key
     *            the key to set
     * @param value
     *            the value to set
     * @return tenant stats element with new usage added
     */
    public TenantUsage addUsageData(final String key, final String value) {
        getUsageData().put(key, value);
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (actions ^ (actions >>> 32));
        result = prime * result + (int) (artifacts ^ (artifacts >>> 32));
        result = prime * result + (int) (overallArtifactVolumeInBytes ^ (overallArtifactVolumeInBytes >>> 32));
        result = prime * result + (int) (targets ^ (targets >>> 32));
        result = prime * result + ((tenantName == null) ? 0 : tenantName.hashCode());
        result = prime * result + ((usageData == null) ? 0 : usageData.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TenantUsage)) {
            return false;
        }
        final TenantUsage other = (TenantUsage) obj;
        if (actions != other.actions) {
            return false;
        }
        if (artifacts != other.artifacts) {
            return false;
        }
        if (overallArtifactVolumeInBytes != other.overallArtifactVolumeInBytes) {
            return false;
        }
        if (targets != other.targets) {
            return false;
        }
        if (!this.getUsageData().equals(other.getUsageData())) {
            return false;
        }
        if (tenantName == null) {
            if (other.tenantName != null) {
                return false;
            }
        } else if (!tenantName.equals(other.tenantName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TenantUsage [tenantName=" + tenantName + ", targets=" + targets + ", artifacts=" + artifacts
                + ", actions=" + actions + ", overallArtifactVolumeInBytes=" + overallArtifactVolumeInBytes
                + ", usageData=" + usageData + "]";
    }

}
