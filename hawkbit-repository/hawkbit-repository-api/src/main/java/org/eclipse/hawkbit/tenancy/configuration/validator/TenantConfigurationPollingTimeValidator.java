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

import static org.eclipse.hawkbit.tenancy.configuration.DurationHelper.fromString;

import java.time.Duration;

import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;
import org.eclipse.hawkbit.tenancy.configuration.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.PollingTime;

/**
 * This class is used to validate, that the property is a String and that it is in the correct polling time format.
 */
public class TenantConfigurationPollingTimeValidator extends TenantConfigurationStringValidator {

    private final Duration minPollingInterval;
    private final Duration maxPollingInterval;

    /**
     * This constructor is called by {@link org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties} using
     * ApplicationContext.getAutowireCapableBeanFactory().createBean(Class) to validate the polling duration configuration.
     * This insures the wiring of the properties is done correctly.
     *
     * @param properties property accessor for poll configuration
     */
    public TenantConfigurationPollingTimeValidator(final ControllerPollProperties properties) {
        this.minPollingInterval = fromString(properties.getMinPollingTime());
        this.maxPollingInterval = fromString(properties.getMaxPollingTime());
    }

    @Override
    public void validate(final Object tenantConfigurationObject) {
        super.validate(tenantConfigurationObject);
        final String tenantConfigurationString = (String) tenantConfigurationObject;

        // validate parsable
        final PollingTime pollingTime = new PollingTime(tenantConfigurationString);
        // validate polling interval in range
        validateInRange(pollingTime.getPollingInterval().getInterval());
        for (final PollingTime.Override override : pollingTime.getOverrides()) {
            validateInRange(override.pollingInterval().getInterval());
        }
    }

    private void validateInRange(final Duration pollingInterval) {
        if (pollingInterval.compareTo(minPollingInterval) < 0) {
            throw new TenantConfigurationValidatorException(String.format(
                    "The polling interval is smaller then minimum polling interval. The allowed range is [%s, %s].",
                    DurationHelper.toString(minPollingInterval), DurationHelper.toString(maxPollingInterval)));
        }
        if (pollingInterval.compareTo(maxPollingInterval) > 0) {
            throw new TenantConfigurationValidatorException(String.format(
                    "The polling interval is bigger then maximum polling interval. The allowed range is [%s, %s].",
                    DurationHelper.toString(minPollingInterval), DurationHelper.toString(maxPollingInterval)));
        }
    }
}