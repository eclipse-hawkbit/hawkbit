package org.eclipse.hawkbit.repository.jpa;

import java.time.Duration;
import java.time.Instant;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.jpa.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.springframework.stereotype.Service;

@Service
public class TimestampCalculator {

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
    public long calculateOverdueTimestamp() {
        long overdueTs = Instant.now().toEpochMilli() //
                - getDurationForKey(TenantConfigurationKey.POLLING_TIME_INTERVAL).toMillis() //
                - getDurationForKey(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL).toMillis();
        return overdueTs;
    }

    private Duration getDurationForKey(TenantConfigurationKey key) {
        return DurationHelper.formattedStringToDuration(getRawStringForKey(key));
    }

    private String getRawStringForKey(TenantConfigurationKey key) {
        return getTenantConfigurationManagement().getConfigurationValue(key, String.class).getValue();
    }

    protected TenantConfigurationManagement getTenantConfigurationManagement() {
        return TenantConfigurationManagementHolder.getInstance().getTenantConfigurationManagement();
    }
}
