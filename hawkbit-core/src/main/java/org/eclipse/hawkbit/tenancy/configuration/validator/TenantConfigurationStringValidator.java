package org.eclipse.hawkbit.tenancy.configuration.validator;

/**
 * specific tenant configuration validator, which validates Strings.
 */
public class TenantConfigurationStringValidator implements TenantConfigurationValidator {

    @Override
    public void validate(final Object tenantConfigurationValue) {
        if (tenantConfigurationValue instanceof String) {
            return;
        }
        throw new TenantConfigurationValidatorException("The given configuration value is expected as a String.");
    }
}
