/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.tenancy.configuration.validator;

/**
 * base interface for clases which can validate tenant configuration values.
 *
 */
public interface TenantConfigurationValidator {

    /**
     * validates the tenant configuration value
     * 
     * @param tenantConfigurationValue
     *            value which will be validated.
     * @throws TenantConfigurationValidatorException
     *             is thrown, when parameter is invalid.
     */
    void validate(Object tenantConfigurationValue) throws TenantConfigurationValidatorException;
}
