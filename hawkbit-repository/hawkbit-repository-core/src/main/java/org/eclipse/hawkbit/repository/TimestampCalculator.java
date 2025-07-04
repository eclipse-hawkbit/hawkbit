/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.time.Duration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.PollingTime;
import org.eclipse.hawkbit.tenancy.configuration.PollingTime.PollingInterval;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;

/**
 * Calculates non-persistent timestamps , e.g. the point a time a target is
 * declared as overdue.<br>
 * Therefore tenant specific configuration may be considered.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimestampCalculator {

    /**
     * Calculates the overdue timestamp (<em>overdue_ts</em>) based on the current timestamp and the intervals for polling and poll-overdue:
     * <p/>
     * <em>overdue_ts = now_ts - pollingInterval - pollingOverdueInterval</em>;<br>
     * <em>pollingInterval</em> and <em>pollingOverdueInterval</em> are retrieved from tenant-specific system configuration.
     * <p/>
     * Note: this method checks against the default polling time interval. I.e. overrides are not considered.
     *
     * @return <em>overdue_ts</em> in milliseconds since Unix epoch as long value
     */
    public static long calculateOverdueTimestamp() {
        return calculateOverdueTimestamp(
                new PollingTime(getRawStringForKey(TenantConfigurationKey.POLLING_TIME)).getPollingInterval(),
                DurationHelper.fromString(getRawStringForKey(TenantConfigurationKey.POLLING_OVERDUE_TIME)));
    }

    private static long calculateOverdueTimestamp(final PollingInterval pollingInterval, final Duration pollingOverdueTime) {
        return System.currentTimeMillis()
                - (pollingInterval.getDeviationPercent() == 0
                    ? pollingInterval.getInterval().toMillis()
                    : pollingInterval.getInterval().toMillis() * (100 + pollingInterval.getDeviationPercent()) / 100)
                - pollingOverdueTime.toMillis();
    }

    private static String getRawStringForKey(final String key) {
        return getSystemSecurityContext().runAsSystem(
                () -> getTenantConfigurationManagement().getConfigurationValue(key, String.class).getValue());
    }

    private static SystemSecurityContext getSystemSecurityContext() {
        return SystemSecurityContextHolder.getInstance().getSystemSecurityContext();
    }

    private static TenantConfigurationManagement getTenantConfigurationManagement() {
        return TenantConfigurationManagementHolder.getInstance().getTenantConfigurationManagement();
    }
}