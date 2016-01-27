package org.eclipse.hawkbit.repository;

import static org.fest.assertions.api.Assertions.assertThat;

import org.eclipse.hawkbit.AbstractIntegrationTestWithMongoDB;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Tenant Configuration Management")
public class TenantConfigurationManagementTest extends AbstractIntegrationTestWithMongoDB {

    @Test
    @Description("Tests that tenant specific configuration can be persisted and in case the tenant does not have specific configuration the default from environment is used instead.")
    public void storeTenantSpecificConfiguration() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final String envPropertyDefault = environment.getProperty(configKey.getDefaultKeyName());
        assertThat(envPropertyDefault).isNotNull();

        // get the configuration from the system management
        final String defaultConfigValue = tenantConfigurationManagement.getConfigurationValue(configKey, String.class)
                .getValue();
        assertThat(envPropertyDefault).isEqualTo(defaultConfigValue);

        // update the tenant specific configuration
        final String newConfigurationValue = "thisIsAnotherValueForPolling";
        assertThat(newConfigurationValue).isNotEqualTo(defaultConfigValue);
        tenantConfigurationManagement
                .addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), newConfigurationValue));

        // verify that new configuration value is used
        final String updatedConfigurationValue = tenantConfigurationManagement
                .getConfigurationValue(configKey, String.class).getValue();
        assertThat(updatedConfigurationValue).isEqualTo(newConfigurationValue);
        assertThat(tenantConfigurationManagement.getTenantConfigurations()).hasSize(1);
    }

    @Test
    @Description("Tests that the tenant specific configuration can be updated")
    public void updateTenantSpecifcConfiguration() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final String value1 = "firstValue";
        final String value2 = "secondValue";

        // add value first
        tenantConfigurationManagement.addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), value1));
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue())
                .isEqualTo(value1);

        // update to value second
        tenantConfigurationManagement.addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), value2));
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue())
                .isEqualTo(value2);
    }

    @Test
    @Description("Tests that the configuration value can be converted from String to Integer automatically")
    public void tenantConfigurationValueConversion() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final Integer value1 = 123;
        tenantConfigurationManagement
                .addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), String.valueOf(value1)));
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, Integer.class).getValue())
                .isEqualTo(value1);
    }

    @Test(expected = ConversionFailedException.class)
    @Description("Tests that the get configuration throws exception in case the value cannot be automatically converted from String to Integer")
    public void wrongTenantConfigurationValueConversionThrowsException() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final String value1 = "thisIsNotANumber";
        // add value as String
        tenantConfigurationManagement
                .addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), String.valueOf(value1)));
        // try to get it as Integer
        tenantConfigurationManagement.getConfigurationValue(configKey, Integer.class);
    }

    @Test
    @Description("Tests that a deletion of a tenant specific configuration deletes it from the database.")
    public void deleteConfigurationReturnNullConfiguration() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY;

        // gateway token does not have default value so no configuration value
        // is should be available
        final String defaultConfigValue = tenantConfigurationManagement.getConfigurationValue(configKey, String.class)
                .getValue();
        assertThat(defaultConfigValue).isNull();

        // update the tenant specific configuration
        final String newConfigurationValue = "thisIsAnotherValueForPolling";
        assertThat(newConfigurationValue).isNotEqualTo(defaultConfigValue);
        tenantConfigurationManagement
                .addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), newConfigurationValue));

        // verify that new configuration value is used
        final String updatedConfigurationValue = tenantConfigurationManagement
                .getConfigurationValue(configKey, String.class).getValue();
        assertThat(updatedConfigurationValue).isEqualTo(newConfigurationValue);

        // delete the tenant specific configuration
        tenantConfigurationManagement.deleteConfiguration(configKey);
        // ensure that now gateway token is set again, because is deleted and
        // must be null now
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue()).isNull();
    }
}
