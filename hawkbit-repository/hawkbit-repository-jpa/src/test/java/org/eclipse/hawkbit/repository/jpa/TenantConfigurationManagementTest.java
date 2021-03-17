/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.exception.InvalidTenantConfigurationKeyException;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidatorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("Tenant Configuration Management")
public class TenantConfigurationManagementTest extends AbstractJpaIntegrationTest implements EnvironmentAware {

    private Environment environment = null;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    @Test
    @Description("Tests that tenant specific configuration can be persisted and in case the tenant does not have specific configuration the default from environment is used instead.")
    public void storeTenantSpecificConfigurationAsString() {
        final String envPropertyDefault = environment
                .getProperty("hawkbit.server.ddi.security.authentication.gatewaytoken.key");
        assertThat(envPropertyDefault).isNotNull();

        // get the configuration from the system management
        final TenantConfigurationValue<String> defaultConfigValue = tenantConfigurationManagement.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class);

        assertThat(defaultConfigValue.isGlobal()).isEqualTo(true);
        assertThat(defaultConfigValue.getValue()).isEqualTo(envPropertyDefault);

        // update the tenant specific configuration
        final String newConfigurationValue = "thisIsAnotherTokenName";
        assertThat(newConfigurationValue).isNotEqualTo(defaultConfigValue.getValue());
        tenantConfigurationManagement.addOrUpdateConfiguration(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, newConfigurationValue);

        // verify that new configuration value is used
        final TenantConfigurationValue<String> updatedConfigurationValue = tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY,
                        String.class);

        assertThat(updatedConfigurationValue.isGlobal()).isEqualTo(false);
        assertThat(updatedConfigurationValue.getValue()).isEqualTo(newConfigurationValue);
        // assertThat(tenantConfigurationManagement.getTenantConfigurations()).hasSize(1);
    }

    @Test
    @Description("Tests that the tenant specific configuration can be updated")
    public void updateTenantSpecifcConfiguration() {
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY;
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
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final Boolean value1 = true;
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value1);
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, Boolean.class).getValue())
                .isEqualTo(value1);
        final Boolean value2 = false;
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value2);
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, Boolean.class).getValue())
                .isEqualTo(value2);
    }

    @Test
    @Description("Tests that the get configuration throws exception in case the value cannot be automatically converted from String to Boolean")
    public void wrongTenantConfigurationValueTypeThrowsException() {
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final String value1 = "thisIsNotABoolean";

        // add value as String
        try {
            tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value1);
            fail("should not have worked as string is not a boolean");
        } catch (final TenantConfigurationValidatorException e) {

        }
    }

    @Test
    @Description("Tests that a deletion of a tenant specific configuration deletes it from the database.")
    public void deleteConfigurationReturnNullConfiguration() {
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY;

        // gateway token does not have default value so no configuration value
        // should be available
        final String defaultConfigValue = tenantConfigurationManagement.getConfigurationValue(configKey, String.class)
                .getValue();
        assertThat(defaultConfigValue).isEmpty();

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
        // must be empty now
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue()).isEmpty();
    }

    @Test
    @Description("Test that an Exception is thrown, when an integer is stored  but a string expected.")
    public void storesIntegerWhenStringIsExpected() {
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY;
        final Integer wrongDataype = 123;
        try {
            tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongDataype);
            fail("should not have worked as integer is not a string");
        } catch (final TenantConfigurationValidatorException e) {

        }
    }

    @Test
    @Description("Test that an Exception is thrown, when an integer is stored but a boolean expected.")
    public void storesIntegerWhenBooleanIsExpected() {
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED;
        final Integer wrongDataype = 123;
        try {
            tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongDataype);
            fail("should not have worked as integer is not a boolean");
        } catch (final TenantConfigurationValidatorException e) {

        }
    }

    @Test
    @Description("Test that an Exception is thrown, when an integer is stored as PollingTime.")
    public void storesIntegerWhenPollingIntervalIsExpected() {
        final String configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;
        final Integer wrongDataype = 123;
        try {
            tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongDataype);
            fail("should not have worked as integer is not a time field");
        } catch (final TenantConfigurationValidatorException e) {

        }
    }

    @Test
    @Description("Test that an Exception is thrown, when an invalid formatted string is stored as PollingTime.")
    public void storesWrongFormattedStringAsPollingInterval() {
        final String configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;
        final String wrongFormatted = "wrongFormatted";
        try {
            tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongFormatted);
            fail("should not have worked as string is not a time field");
        } catch (final TenantConfigurationValidatorException e) {

        }
    }

    @Test
    @Description("Test that an Exception is thrown, when an invalid formatted string is stored as PollingTime.")
    public void storesTooSmallDurationAsPollingInterval() {
        final String configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;

        final String tooSmallDuration = DurationHelper
                .durationToFormattedString(DurationHelper.getDurationByTimeValues(0, 0, 1));
        try {
            tenantConfigurationManagement.addOrUpdateConfiguration(configKey, tooSmallDuration);
            fail("should not have worked as string has an invalid format");
        } catch (final TenantConfigurationValidatorException e) {

        }
    }

    @Test
    @Description("Stores a correct formatted PollignTime and reads it again.")
    public void storesCorrectDurationAsPollingInterval() {
        final String configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;

        final Duration duration = DurationHelper.getDurationByTimeValues(1, 2, 0);
        assertThat(duration).isEqualTo(Duration.ofHours(1).plusMinutes(2));

        tenantConfigurationManagement.addOrUpdateConfiguration(configKey,
                DurationHelper.durationToFormattedString(duration));

        final String storedDurationString = tenantConfigurationManagement.getConfigurationValue(configKey, String.class)
                .getValue();
        assertThat(duration).isEqualTo(DurationHelper.formattedStringToDuration(storedDurationString));
    }

    @Test
    @Description("Request a config value in a wrong Value")
    public void requestConfigValueWithWrongType() {
        try {
            tenantConfigurationManagement.getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL,
                    Serializable.class);
            Assertions.fail("");
        } catch (final TenantConfigurationValidatorException e) {

        }
    }

    @Test
    @Description("Verifies that every TenenatConfiguraationKeyName exists only once")
    public void verifyThatAllKeysAreDifferent() {
        final Map<String, Void> keynames = new HashMap<>();

        tenantConfigurationProperties.getConfigurationKeys().forEach(key -> {

            if (keynames.containsKey(key.getKeyName())) {
                throw new IllegalStateException("The key names are not unique");
            }

            keynames.put(key.getKeyName(), null);
        });
    }

    @Test
    @Description("Get TenantConfigurationKeyByName")
    public void getTenantConfigurationKeyByName() {
        final String configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;

        assertThat(tenantConfigurationProperties.fromKeyName(configKey).getKeyName()).isEqualTo(configKey);
    }

    @Test
    @Description("Tenant configuration which is not declared throws exception")
    public void storeTenantConfigurationWhichIsNotDeclaredThrowsException() {
        final String configKeyWhichDoesNotExists = "configKeyWhichDoesNotExists";
        try {
            tenantConfigurationManagement.addOrUpdateConfiguration(configKeyWhichDoesNotExists, "value");
            fail("Expected InvalidTenantConfigurationKeyException for tenant configuration key which is not declared");
        } catch (final InvalidTenantConfigurationKeyException e) {
            // expected exception
        }
    }
}
