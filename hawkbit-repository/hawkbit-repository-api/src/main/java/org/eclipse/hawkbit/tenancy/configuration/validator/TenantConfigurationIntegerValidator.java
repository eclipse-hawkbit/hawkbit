/**
 * Copyright (c) 2018 Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.tenancy.configuration.validator;

/**
 * Specific tenant configuration validator, which validates that the given value is an Integer.
 */
public class TenantConfigurationIntegerValidator implements TenantConfigurationValidator {

    @Override
    public Class<?> validateToClass() {
        return Integer.class;
    }
}