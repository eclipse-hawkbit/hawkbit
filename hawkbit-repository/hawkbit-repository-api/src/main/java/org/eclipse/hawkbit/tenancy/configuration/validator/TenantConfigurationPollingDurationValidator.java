/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy.configuration.validator;

import java.time.Duration;
import java.time.format.DateTimeParseException;

import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;
import org.eclipse.hawkbit.tenancy.configuration.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is used to validate, that the property is a String and that it is in the correct duration format.
 */
public class TenantConfigurationPollingDurationValidator implements TenantConfigurationValidator {

    private final Duration minDuration;
    private final Duration maxDuration;

    /**
     * This constructor is called by {@link org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties} using
     * ApplicationContext.getAutowireCapableBeanFactory().createBean(Class) to validate the polling duration configuration.
     * This insures the wiring of the properties is done correctly.
     *
     * @param properties property accessor for poll configuration
     */
    public TenantConfigurationPollingDurationValidator(final ControllerPollProperties properties) {
        minDuration = DurationHelper.formattedStringToDuration(properties.getMinPollingTime());
        maxDuration = DurationHelper.formattedStringToDuration(properties.getMaxPollingTime());
    }

    @Override
    // Exception squid:S1166 - Hide origin exception
    @SuppressWarnings({ "squid:S1166" })
    public void validate(final Object tenantConfigurationObject) {
        TenantConfigurationValidator.super.validate(tenantConfigurationObject);
        final String tenantConfigurationString = (String) tenantConfigurationObject;

        final Duration tenantConfigurationValue;
        try {
            tenantConfigurationValue = DurationHelper.formattedStringToDuration(tenantConfigurationString);
        } catch (final DateTimeParseException ex) {
            throw new TenantConfigurationValidatorException(
                    String.format("The given configuration value is expected as a string in the format %s.",
                            DurationHelper.DURATION_FORMAT));
        }

        if (!DurationHelper.durationRangeValidator(minDuration, maxDuration).isWithinRange(tenantConfigurationValue)) {
            throw new TenantConfigurationValidatorException(
                    String.format("The given configuration value is not in the allowed range from %s to %s.",
                            DurationHelper.durationToFormattedString(minDuration),
                            DurationHelper.durationToFormattedString(maxDuration)));
        }
    }

    @Override
    public Class<?> validateToClass() {
        return String.class;
    }
}