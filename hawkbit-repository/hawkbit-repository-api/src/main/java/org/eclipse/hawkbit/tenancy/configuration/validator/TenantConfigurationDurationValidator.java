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

/**
 * This class is used to validate, that the property is a String and that it is in the correct duration format.
 */
public class TenantConfigurationDurationValidator extends TenantConfigurationStringValidator {

    // Exception squid:S1166 - Hide origin exception
    @SuppressWarnings({ "squid:S1166" })
    @Override
    public void validate(final Object tenantConfigurationObject) {
        super.validate(tenantConfigurationObject);

        final String tenantConfigurationString = (String) tenantConfigurationObject;
        final Duration duration = fromString(tenantConfigurationString);
        if (duration.isNegative()) {
            throw new TenantConfigurationValidatorException("The given configuration value is not in the allowed to be negative.");
        }
    }
}