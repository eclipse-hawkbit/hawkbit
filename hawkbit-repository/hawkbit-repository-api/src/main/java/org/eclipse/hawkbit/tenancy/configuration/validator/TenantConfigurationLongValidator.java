/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.tenancy.configuration.validator;

/**
 * Specific tenant configuration validator, which validates that the given value
 * is a {@link Long}.
 */
public class TenantConfigurationLongValidator implements TenantConfigurationValidator {

    @Override
    public Class<?> validateToClass() {
        return Long.class;
    }
}
