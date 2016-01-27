package org.eclipse.hawkbit.tenancy.configuration.validator;

public class StringValidator implements TenantConfigurationValidator {

    @Override
    public boolean validate(final Object tenantConfigurationValue) {
        if (tenantConfigurationValue instanceof String) {
            return true;
        }

        return false;
    }
}
