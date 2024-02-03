/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.time.Duration;
import java.time.Instant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;

/**
 * Calculates non-persistent timestamps , e.g. the point a time a target is
 * declared as overdue.<br>
 * Therefore tenant specific configuration may be considered.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimestampCalculator {

    /**
     * Calculates the overdue timestamp (<em>overdue_ts</em>) based on the
     * current timestamp and the intervals for polling and poll-overdue:
     * <p>
     * <em>overdue_ts = now_ts - pollingInterval -
     * pollingOverdueInterval</em>;<br>
     * <em>pollingInterval</em> and <em>pollingOverdueInterval</em> are
     * retrieved from tenant-specific system configuration.
     *
     * @return <em>overdue_ts</em> in milliseconds since Unix epoch as long
     *         value
     */
    public static long calculateOverdueTimestamp() {
        return Instant.now().toEpochMilli() - getDurationForKey(TenantConfigurationKey.POLLING_TIME_INTERVAL).toMillis()
                - getDurationForKey(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL).toMillis();
    }

    private static Duration getDurationForKey(final String key) {
        return DurationHelper.formattedStringToDuration(getRawStringForKey(key));
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