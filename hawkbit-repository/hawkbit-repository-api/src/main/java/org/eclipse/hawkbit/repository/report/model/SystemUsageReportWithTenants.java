/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.report.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bean for holding the system usage stats including tenant specific data.
 *
 */
public class SystemUsageReportWithTenants extends SystemUsageReport {

    private final List<TenantUsage> tenants = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param overallTargets
     *            of the system
     * @param overallArtifacts
     *            of the system
     * @param overallActions
     *            of the system
     * @param overallArtifactVolumeInBytes
     *            of the system
     */
    public SystemUsageReportWithTenants(final long overallTargets, final long overallArtifacts,
            final long overallActions, final long overallArtifactVolumeInBytes, final long overallTenants) {
        super(overallTargets, overallArtifacts, overallActions, overallArtifactVolumeInBytes, overallTenants);
    }

    /**
     * @param tenantUsage
     *            of one tenant
     * @return updated bean
     */
    public SystemUsageReportWithTenants addTenantData(final TenantUsage tenantUsage) {
        tenants.add(tenantUsage);
        return this;
    }

    /**
     * @return tenant data
     */
    public List<TenantUsage> getTenants() {
        return Collections.unmodifiableList(tenants);
    }

}
