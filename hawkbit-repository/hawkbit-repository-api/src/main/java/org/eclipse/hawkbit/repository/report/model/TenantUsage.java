/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.report.model;

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

    /**
     * Constructor.
     *
     * @param tenantName
     */
    public TenantUsage(final String tenantName) {
        super();
        this.tenantName = tenantName;
    }

    /**
     * @return name of the tenant
     */
    public String getTenantName() {
        return tenantName;
    }

    /**
     * @return number of targets of the tenant
     */
    public long getTargets() {
        return targets;
    }

    /**
     * @param targets
     *            of the tenant
     * @return updated tenant stats element
     */
    public TenantUsage setTargets(final long targets) {
        this.targets = targets;
        return this;
    }

    /**
     * @return number of undeleted artifacts of the tenant
     */
    public long getArtifacts() {
        return artifacts;
    }

    /**
     * @param artifacts
     *            of tenant
     * @return updated tenant stats element
     */
    public TenantUsage setArtifacts(final long artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    /**
     * @return current overallArtifactVolumeInBytes
     */
    public long getOverallArtifactVolumeInBytes() {
        return overallArtifactVolumeInBytes;
    }

    /**
     * @param overallArtifactVolumeInBytes
     *            of the tenant in bytes
     * @return updated tenant stats element
     */
    public TenantUsage setOverallArtifactVolumeInBytes(final long overallArtifactVolumeInBytes) {
        this.overallArtifactVolumeInBytes = overallArtifactVolumeInBytes;
        return this;
    }

    /**
     * @return number of actions of tenant
     */
    public long getActions() {
        return actions;
    }

    /**
     * @param actions
     *            of the tenant
     * @return updated tenant stats element
     */
    public TenantUsage setActions(final long actions) {
        this.actions = actions;
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
                + ", actions=" + actions + ", overallArtifactVolumeInBytes=" + overallArtifactVolumeInBytes + "]";
    }

}
