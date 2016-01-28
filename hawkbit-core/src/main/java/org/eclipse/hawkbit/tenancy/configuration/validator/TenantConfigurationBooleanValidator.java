package org.eclipse.hawkbit.tenancy.configuration.validator;

/**
 * specific tenant configuration validator, which validates that the given value is a booleans.
 */
public class TenantConfigurationBooleanValidator implements TenantConfigurationValidator {

    @Override
    public void validate(final Object tenantConfigurationValue) {
        if (tenantConfigurationValue instanceof Boolean) {
            return;
        }
        throw new TenantConfigurationValidatorException("The given configuration value is expected as a boolean.");
    }

}
