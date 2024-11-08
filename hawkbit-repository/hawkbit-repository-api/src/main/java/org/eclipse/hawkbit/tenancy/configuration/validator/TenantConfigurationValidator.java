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

import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;

/**
 * base interface for clases which can validate tenant configuration values.
 */
public interface TenantConfigurationValidator {

    /**
     * validates the tenant configuration value
     *
     * @param tenantConfigurationValue value which will be validated.
     * @throws TenantConfigurationValidatorException is thrown, when parameter is invalid.
     */
    default void validate(final Object tenantConfigurationValue) {
        if (tenantConfigurationValue != null
                && validateToClass().isAssignableFrom(tenantConfigurationValue.getClass())) {
            return;
        }
        throw new TenantConfigurationValidatorException(
                "The given configuration value is expected as a " + validateToClass().getSimpleName());
    }

    /**
     * Return the generic class to check the object
     *
     * @return the class to check the value
     */
    default Class<?> validateToClass() {
        return Object.class;
    }
}
