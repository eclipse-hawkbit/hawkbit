/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.rsql;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.POLLING_OVERDUE_TIME;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.POLLING_TIME;

import java.time.Duration;

import org.eclipse.hawkbit.repository.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.PollingTime;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * Adds macro capabilities to RSQL expressions that are used to filter for targets.
 * <p>
 * Some (virtual) properties do not have a representation in the database (in general these properties are time-related, or more explicitly,
 * they deal with time intervals).<br>
 * Such a virtual property needs to be calculated on Java-side before it may be used in a target filter query that is passed to the database.
 * Therefore, a placeholder is used in the RSQL expression that is expanded when the RSQL is parsed
 * <p>
 * A virtual property may either be a system value like the current date (aka <em>now_ts</em>) or a value derived from (tenant-specific) system
 * configuration (e.g. <em>overdue_ts</em>).
 * <p>
 * Known values are:<br>
 * <ul>
 *   <li><em>now_ts</em>: maps to system UTC time in milliseconds since Unix epoch as long value</li>
 *   <li><em>overdue_ts</em>: is a calculated value: <em>overdue_ts = now_ts - pollingInterval - pollingOverdueInterval</em>; pollingInterval
 *                            and pollingOverdueInterval are retrieved from tenant-specific system configuration.</li>
 * </ul>
 */
public class VirtualPropertyResolver {

    private static final PropertyPlaceholderHelper HELPER = new PropertyPlaceholderHelper("${", "}", null, '$', true);

    public String replace(final String input) {
        return HELPER.replacePlaceholders(input, VirtualPropertyResolver::lookup);
    }

    private static String lookup(final String rhs) {
        if ("now_ts".equalsIgnoreCase(rhs)) {
            return String.valueOf(System.currentTimeMillis());
        } else if ("overdue_ts".equalsIgnoreCase(rhs)) {
            return String.valueOf(calculateOverdueTimestamp());
        } else {
            return null;
        }
    }

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
                new PollingTime(getRawStringForKey(POLLING_TIME)).getPollingInterval(),
                DurationHelper.fromString(getRawStringForKey(POLLING_OVERDUE_TIME)));
    }

    private static long calculateOverdueTimestamp(final PollingTime.PollingInterval pollingInterval, final Duration pollingOverdueTime) {
        return System.currentTimeMillis()
                - (pollingInterval.getDeviationPercent() == 0
                ? pollingInterval.getInterval().toMillis()
                : pollingInterval.getInterval().toMillis() * (100 + pollingInterval.getDeviationPercent()) / 100)
                - pollingOverdueTime.toMillis();
    }

    private static String getRawStringForKey(final String key) {
        return SystemSecurityContextHolder.getInstance().getSystemSecurityContext().runAsSystem(
                () -> TenantConfigurationManagementHolder.getInstance().getTenantConfigurationManagement()
                        .getConfigurationValue(key, String.class).getValue());
    }
}