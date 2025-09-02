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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.EqualsAndHashCode;

/**
 * Bean for holding the system usage stats including tenant specific data.
 */
@EqualsAndHashCode(callSuper = true)
public class SystemUsageReportWithTenants extends SystemUsageReport {

    private final List<TenantUsage> tenants = new ArrayList<>();

    public SystemUsageReportWithTenants(
            final long overallTargets, final long overallArtifacts,
            final long overallActions, final long overallArtifactVolumeInBytes, final long overallTenants) {
        super(overallTargets, overallArtifacts, overallActions, overallArtifactVolumeInBytes, overallTenants);
    }

    public SystemUsageReportWithTenants addTenantData(final TenantUsage tenantUsage) {
        tenants.add(tenantUsage);
        return this;
    }

    public List<TenantUsage> getTenants() {
        return Collections.unmodifiableList(tenants);
    }
}