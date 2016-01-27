package org.eclipse.hawkbit.tenancy.configuration.validator;

public interface TenantConfigurationValidator {

    boolean validate(Object tenantConfigurationValue);
}
