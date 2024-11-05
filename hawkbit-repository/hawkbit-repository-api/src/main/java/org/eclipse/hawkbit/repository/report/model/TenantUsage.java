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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * System usage stats element for a tenant.
 */
@Data
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
     * @param tenantName name of the tenant
     */
    public TenantUsage(final String tenantName) {
        this.tenantName = tenantName;
    }

    public Map<String, String> getUsageData() {
        return Collections.unmodifiableMap(getLazyUsageData());
    }

    /**
     * Add a key and value as usage data to the system usage stats.
     *
     * @param key the key to set
     * @param value the value to set
     * @return tenant stats element with new usage added
     */
    public TenantUsage addUsageData(final String key, final String value) {
        getLazyUsageData().put(key, value);
        return this;
    }

    private Map<String, String> getLazyUsageData() {
        if (usageData == null) {
            usageData = new HashMap<>();
        }
        return usageData;
    }
}