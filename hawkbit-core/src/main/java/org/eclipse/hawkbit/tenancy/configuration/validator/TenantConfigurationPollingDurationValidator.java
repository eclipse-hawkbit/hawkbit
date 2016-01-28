package org.eclipse.hawkbit.tenancy.configuration.validator;

import java.time.Duration;
import java.time.format.DateTimeParseException;

import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.validator.exceptions.TenantConfigurationValidatorException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * specific tenant configuration validator, which validates that the given value
 * is a String in the correct duration format..
 */
public class TenantConfigurationPollingDurationValidator implements TenantConfigurationValidator {

    // private final ControllerPollProperties properties;

    private final DurationHelper durationHelper = new DurationHelper();

    private final Duration minDuration;

    private final Duration maxDuration;

    /**
     * @param properties
     *            property accessor for poll configuration
     */
    @Autowired
    public TenantConfigurationPollingDurationValidator(final ControllerPollProperties properties) {
        // this.properties = properties;

        minDuration = durationHelper.formattedStringToDuration(properties.getMinPollingTime());
        maxDuration = durationHelper.formattedStringToDuration(properties.getMaxPollingTime());
    }

    @Override
    public void validate(final Object tenantConfigurationObject) {
        if (!(tenantConfigurationObject instanceof String)) {
            throw new TenantConfigurationValidatorException("The given configuration value is expected as a string.");
        }

        final String tenantConfigurationString = (String) tenantConfigurationObject;

        final Duration tenantConfigurationValue;
        try {
            tenantConfigurationValue = durationHelper.formattedStringToDuration(tenantConfigurationString);

        } catch (final DateTimeParseException ex) {
            throw new TenantConfigurationValidatorException(
                    String.format("The given configuration value is expected as a string in the format %s.",
                            DurationHelper.DURATION_FORMAT));
        }

        if (!durationHelper.durationRangeValidator(minDuration, maxDuration).isWithinRange(tenantConfigurationValue)) {
            throw new TenantConfigurationValidatorException(
                    String.format("The given configuration value is not in the allowed range from %s to %s.",
                            durationHelper.durationToFormattedString(minDuration),
                            durationHelper.durationToFormattedString(maxDuration)));
        }
    }
}
