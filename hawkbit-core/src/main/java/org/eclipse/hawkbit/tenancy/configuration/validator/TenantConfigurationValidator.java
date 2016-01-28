package org.eclipse.hawkbit.tenancy.configuration.validator;

import org.eclipse.hawkbit.tenancy.configuration.validator.exceptions.TenantConfigurationValidatorException;

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
