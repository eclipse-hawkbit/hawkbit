package org.eclipse.hawkbit.tenancy.configuration.validator;

public class BooleanValidator implements TenantConfigurationValidator {

    @Override
    public boolean validate(final Object tenantConfigurationValue) {
        if (tenantConfigurationValue instanceof Boolean) {
            return true;
        }
        return false;
    }

}
