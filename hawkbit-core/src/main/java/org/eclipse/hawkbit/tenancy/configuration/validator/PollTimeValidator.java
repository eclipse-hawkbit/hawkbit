package org.eclipse.hawkbit.tenancy.configuration.validator;

import java.time.Duration;
import java.time.format.DateTimeParseException;

import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.springframework.beans.factory.annotation.Autowired;

public class PollTimeValidator implements TenantConfigurationValidator {

    // private final ControllerPollProperties properties;

    private final DurationHelper durationHelper = new DurationHelper();

    private final Duration minDuration;

    private final Duration maxDuration;

    @Autowired
    public PollTimeValidator(final ControllerPollProperties properties) {
        // this.properties = properties;

        minDuration = durationHelper.formattedStringToDuration(properties.getMinPollingTime());
        maxDuration = durationHelper.formattedStringToDuration(properties.getMaxPollingTime());
    }

    @Override
    public boolean validate(final Object tenantConfigurationObject) {
        if (!(tenantConfigurationObject instanceof String)) {
            return false;
        }

        final String tenantConfigurationString = (String) tenantConfigurationObject;

        try {
            final Duration tenantConfigurationValue = durationHelper
                    .formattedStringToDuration(tenantConfigurationString);

            return durationHelper.durationRangeValidator(minDuration, maxDuration)
                    .isWithinRange(tenantConfigurationValue);

        } catch (final DateTimeParseException ex) {
            return false;
        }
    }
}
