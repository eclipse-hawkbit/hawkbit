/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.time.Duration;
import java.time.Instant;

import org.eclipse.hawkbit.repository.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;

/**
 * Calculates non-persistent timestamps , e.g. the point a time a target is
 * declared as overdue.<br>
 * Therefore tenant specific configuration may be considered.
 *
 */
public final class TimestampCalculator {

    private TimestampCalculator() {
    }

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
        return getTenantConfigurationManagement().getConfigurationValue(key, String.class).getValue();
    }

    public static TenantConfigurationManagement getTenantConfigurationManagement() {
        return TenantConfigurationManagementHolder.getInstance().getTenantConfigurationManagement();
    }
}
