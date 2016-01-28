package org.eclipse.hawkbit.repository;

import static org.fest.assertions.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.AbstractIntegrationTestWithMongoDB;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidatorException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Tenant Configuration Management")
public class TenantConfigurationManagementTest extends AbstractIntegrationTestWithMongoDB {

    final DurationHelper durationHelper = new DurationHelper();

    @Test
    @Description("Tests that tenant specific configuration can be persisted and in case the tenant does not have specific configuration the default from environment is used instead.")
    public void storeTenantSpecificConfigurationAsString() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME;
        final String envPropertyDefault = environment.getProperty(configKey.getDefaultKeyName());
        assertThat(envPropertyDefault).isNotNull();

        // get the configuration from the system management
        final TenantConfigurationValue<String> defaultConfigValue = tenantConfigurationManagement
                .getConfigurationValue(configKey, String.class);

        assertThat(defaultConfigValue.isGlobal()).isEqualTo(true);
        assertThat(defaultConfigValue.getValue()).isEqualTo(envPropertyDefault);

        // update the tenant specific configuration
        final String newConfigurationValue = "thisIsAnotherTokenName";
        assertThat(newConfigurationValue).isNotEqualTo(defaultConfigValue.getValue());
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, newConfigurationValue);

        // verify that new configuration value is used
        final TenantConfigurationValue<String> updatedConfigurationValue = tenantConfigurationManagement
                .getConfigurationValue(configKey, String.class);

        assertThat(updatedConfigurationValue.isGlobal()).isEqualTo(false);
        assertThat(updatedConfigurationValue.getValue()).isEqualTo(newConfigurationValue);
        // assertThat(tenantConfigurationManagement.getTenantConfigurations()).hasSize(1);
    }

    @Test
    @Description("Tests that the tenant specific configuration can be updated")
    public void updateTenantSpecifcConfiguration() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME;
        final String value1 = "firstValue";
        final String value2 = "secondValue";

        // add value first
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value1);
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue())
                .isEqualTo(value1);

        // update to value second
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value2);
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue())
                .isEqualTo(value2);
    }

    @Test
    @Description("Tests that the configuration value can be converted from String to Integer automatically")
    public void storeAndUpdateTenantSpecificConfigurationAsBoolean() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final Boolean value1 = true;
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value1);
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, Boolean.class).getValue())
                .isEqualTo(value1);
        final Boolean value2 = false;
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value2);
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, Boolean.class).getValue())
                .isEqualTo(value2);
    }

    @Test(expected = ConversionFailedException.class)
    @Description("Tests that the get configuration throws exception in case the value cannot be automatically converted from String to Boolean")
    public void wrongTenantConfigurationValueTypeThrowsException() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final String value1 = "thisIsNotABoolean";
        // add value as String
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value1);
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
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, newConfigurationValue);

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

    @Test(expected = TenantConfigurationValidatorException.class)
    @Description("Test that an Exception is thrown, when an integer is stored  but a string expected.")
    public void storesIntegerWhenStringIsExpected() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME;
        final Integer wrongDataype = 123;
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongDataype);
    }

    @Test(expected = TenantConfigurationValidatorException.class)
    @Description("Test that an Exception is thrown, when an integer is stored but a boolean expected.")
    public void storesIntegerWhenBooleanIsExpected() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED;
        final Integer wrongDataype = 123;
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongDataype);
    }

    @Test(expected = TenantConfigurationValidatorException.class)
    @Description("Test that an Exception is thrown, when an integer is stored as PollingTime.")
    public void storesIntegerWhenPollingIntervalIsExpected() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;
        final Integer wrongDataype = 123;
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongDataype);
    }

    @Test(expected = TenantConfigurationValidatorException.class)
    @Description("Test that an Exception is thrown, when an invalid formatted string is stored as PollingTime.")
    public void storesWrongFormattedStringAsPollingInterval() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;
        final String wrongFormatted = "wrongFormatted";
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongFormatted);
    }

    @Test(expected = TenantConfigurationValidatorException.class)
    @Description("Test that an Exception is thrown, when an invalid formatted string is stored as PollingTime.")
    public void storesTooSmallDurationAsPollingInterval() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;

        final String tooSmallDuration = durationHelper
                .durationToFormattedString(durationHelper.getDurationByTimeValues(0, 0, 1));
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, tooSmallDuration);
    }

    @Test
    @Description("Stores a correct formatted PollignTime and reads it again.")
    public void storesCorrectDurationAsPollingInterval() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;

        final Duration duration = durationHelper.getDurationByTimeValues(1, 2, 0);
        assertThat(duration).isEqualTo(Duration.ofHours(1).plusMinutes(2));

        tenantConfigurationManagement.addOrUpdateConfiguration(configKey,
                durationHelper.durationToFormattedString(duration));

        final String storedDurationString = tenantConfigurationManagement.getConfigurationValue(configKey, String.class)
                .getValue();
        assertThat(duration).isEqualTo(durationHelper.formattedStringToDuration(storedDurationString));
    }

    @Test
    @Description("Request a config value in a wrong Value")
    public void requestConfigValueWithWrongType() {
        try {
            tenantConfigurationManagement.getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL,
                    Object.class);
            Assert.fail("");
        } catch (final TenantConfigurationValidatorException e) {

        }
    }

    @Test
    @Description("Verifies that every TenenatConfiguraationKeyName exists only once")
    public void verifyThatAllKeysAreDifferent() {
        final Map<String, Void> keynames = new HashMap<String, Void>();

        Arrays.stream(TenantConfigurationKey.values()).forEach(key -> {

            if (keynames.containsKey(key.getKeyName())) {
                throw new IllegalStateException("The key names are not unique");
            }

            keynames.put(key.getKeyName(), null);
        });
    }

    @Test
    @Description("Get TenantConfigurationKeyByName")
    public void getTenantConfigurationKeyByName() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;

        assertThat(TenantConfigurationKey.fromKeyName(configKey.getKeyName())).isEqualTo(configKey);
    }

}
