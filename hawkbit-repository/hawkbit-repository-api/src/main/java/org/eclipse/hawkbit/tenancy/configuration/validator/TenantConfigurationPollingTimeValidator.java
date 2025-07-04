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

import static org.eclipse.hawkbit.tenancy.configuration.DurationHelper.durationToFormattedString;
import static org.eclipse.hawkbit.tenancy.configuration.DurationHelper.formattedStringToDuration;

import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;
import org.eclipse.hawkbit.tenancy.configuration.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper.DurationRangeValidator;
import org.eclipse.hawkbit.tenancy.configuration.PollingTime;

/**
 * This class is used to validate, that the property is a String and that it is in the correct polling time format.
 */
public class TenantConfigurationPollingTimeValidator implements TenantConfigurationValidator {

    private final DurationRangeValidator rangeValidator;

    /**
     * This constructor is called by {@link org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties} using
     * ApplicationContext.getAutowireCapableBeanFactory().createBean(Class) to validate the polling duration configuration.
     * This insures the wiring of the properties is done correctly.
     *
     * @param properties property accessor for poll configuration
     */
    public TenantConfigurationPollingTimeValidator(final ControllerPollProperties properties) {
        rangeValidator = DurationHelper.durationRangeValidator(
                formattedStringToDuration(properties.getMinPollingTime()), formattedStringToDuration(properties.getMaxPollingTime()));
    }

    @Override
    public void validate(final Object tenantConfigurationObject) {
        TenantConfigurationValidator.super.validate(tenantConfigurationObject);
        final String tenantConfigurationString = (String) tenantConfigurationObject;

        final PollingTime pollingTime = new PollingTime(tenantConfigurationString);

        if (!rangeValidator.isWithinRange(pollingTime.getPollingInterval().getInterval())) {
            throw new TenantConfigurationValidatorException(String.format(
                    "The given poling interval is not in the allowed range from %s to %s.",
                    durationToFormattedString(rangeValidator.getMin()), durationToFormattedString(rangeValidator.getMax())));
        }

        for (final PollingTime.Override override : pollingTime.getOverrides()) {
            if (!rangeValidator.isWithinRange(override.pollingInterval().getInterval())) {
                throw new TenantConfigurationValidatorException(String.format(
                        "An override polling interval is not in the allowed range from %s to %s.",
                        durationToFormattedString(rangeValidator.getMin()), durationToFormattedString(rangeValidator.getMax())));
            }
        }
    }

    @Override
    public Class<?> validateToClass() {
        return String.class;
    }
}