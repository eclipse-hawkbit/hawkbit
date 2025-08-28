/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model.report;

import lombok.Data;

/**
 * Bean for holding the system usage stats.
 */
@Data
public class SystemUsageReport {

    private final long overallTargets;
    private final long overallArtifacts;
    private final long overallArtifactVolumeInBytes;
    private final long overallActions;
    private final long overallTenants;

    public SystemUsageReport(
            final long overallTargets, final long overallArtifacts, final long overallActions,
            final long overallArtifactVolumeInBytes, final long overallTenants) {
        this.overallTargets = overallTargets;
        this.overallArtifacts = overallArtifacts;
        this.overallActions = overallActions;
        this.overallArtifactVolumeInBytes = overallArtifactVolumeInBytes;
        this.overallTenants = overallTenants;
    }
}