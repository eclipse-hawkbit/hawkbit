/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.exception.InvalidTenantConfigurationKeyException;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.Test;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Tenant Configuration Management
 */
class TenantConfigurationManagementTest extends AbstractJpaIntegrationTest implements EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    /**
     * Tests that tenant specific configuration can be persisted and in case the tenant does not have specific configuration the default from environment is used instead.
     */
    @Test    void storeTenantSpecificConfigurationAsString() {
        final String envPropertyDefault = environment.getProperty("hawkbit.server.ddi.security.authentication.gatewaytoken.key");
        assertThat(envPropertyDefault).isNotNull();

        // get the configuration from the system management
        final TenantConfigurationValue<String> defaultConfigValue = tenantConfigurationManagement.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class);

        assertThat(defaultConfigValue.isGlobal()).isTrue();
        assertThat(defaultConfigValue.getValue()).isEqualTo(envPropertyDefault);

        // update the tenant specific configuration
        final String newConfigurationValue = "thisIsAnotherTokenName";
        assertThat(newConfigurationValue).isNotEqualTo(defaultConfigValue.getValue());
        tenantConfigurationManagement.addOrUpdateConfiguration(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, newConfigurationValue);

        // verify that new configuration value is used
        final TenantConfigurationValue<String> updatedConfigurationValue = tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class);

        assertThat(updatedConfigurationValue.isGlobal()).isFalse();
        assertThat(updatedConfigurationValue.getValue()).isEqualTo(newConfigurationValue);
        // assertThat(tenantConfigurationManagement.getTenantConfigurations()).hasSize(1);
    }

    /**
     * Tests that the tenant specific configuration can be updated
     */
    @Test    void updateTenantSpecificConfiguration() {
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY;
        final String value1 = "firstValue";
        final String value2 = "secondValue";

        // add value first
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value1);
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue()).isEqualTo(value1);

        // update to value second
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value2);
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue()).isEqualTo(value2);
    }

    /**
     * Tests that the tenant specific configuration can be batch updated
     */
    @Test    void batchUpdateTenantSpecificConfiguration() {
        Map<String, Serializable> configuration = new HashMap<>() {{
            put(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, "token_123");
            put(TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED, true);
        }};

        // add value first
        tenantConfigurationManagement.addOrUpdateConfiguration(configuration);
        assertThat(tenantConfigurationManagement.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class).getValue())
                .isEqualTo("token_123");
        assertThat(
                tenantConfigurationManagement.getConfigurationValue(TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED, Boolean.class).getValue())
                .isTrue();
    }

    /**
     * Tests that the configuration value can be converted from String to Integer automatically
     */
    @Test    void storeAndUpdateTenantSpecificConfigurationAsBoolean() {
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final Boolean value1 = true;
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value1);
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, Boolean.class).getValue()).isEqualTo(value1);
        final Boolean value2 = false;
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value2);
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, Boolean.class).getValue()).isEqualTo(value2);
    }

    /**
     * Tests that the get configuration throws exception in case the value cannot be automatically converted from String to Boolean
     */
    @Test    void wrongTenantConfigurationValueTypeThrowsException() {
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final String value1 = "thisIsNotABoolean";

        // add value as String
        assertThatThrownBy(() -> tenantConfigurationManagement.addOrUpdateConfiguration(configKey, value1))
                .as("Should not have worked as value is not a boolean")
                .isInstanceOf(TenantConfigurationValidatorException.class);
    }

    /**
     * Tests that the get configuration throws exception in case the value is the wrong type
     */
    @Test    void batchWrongTenantConfigurationValueTypeThrowsException() {
        final Map<String, Serializable> configuration = new HashMap<>() {{
            put(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, "token_123");
            put(TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED, true);
            put(TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED, "wrong");
        }};

        try {
            tenantConfigurationManagement.addOrUpdateConfiguration(configuration);
            fail("should not have worked as type is wrong");
        } catch (final TenantConfigurationValidatorException e) {
            assertThat(
                    tenantConfigurationManagement.getConfigurationValue(
                            TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class).getValue())
                    .isNotEqualTo("token_123");
            assertThat(
                    tenantConfigurationManagement.getConfigurationValue(
                            TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED, Boolean.class).getValue())
                    .isNotEqualTo(true);
        }
    }

    /**
     * Tests that a deletion of a tenant specific configuration deletes it from the database.
     */
    @Test    void deleteConfigurationReturnNullConfiguration() {
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY;

        // gateway token does not have default value so no configuration value should be available
        final String defaultConfigValue = tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue();
        assertThat(defaultConfigValue).isEmpty();

        // update the tenant specific configuration
        final String newConfigurationValue = "thisIsAnotherValueForPolling";
        assertThat(newConfigurationValue).isNotEqualTo(defaultConfigValue);
        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, newConfigurationValue);

        // verify that new configuration value is used
        final String updatedConfigurationValue = tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue();
        assertThat(updatedConfigurationValue).isEqualTo(newConfigurationValue);

        // delete the tenant specific configuration
        tenantConfigurationManagement.deleteConfiguration(configKey);
        // ensure that now gateway token is set again, because is deleted and
        // must be empty now
        assertThat(tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue()).isEmpty();
    }

    /**
     * Test that an Exception is thrown, when an integer is stored  but a string expected.
     */
    @Test    void storesIntegerWhenStringIsExpected() {
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY;
        final Integer wrongDatType = 123;
        assertThatThrownBy(() -> tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongDatType))
                .as("Should not have worked as integer is not a string")
                .isInstanceOf(TenantConfigurationValidatorException.class);
    }

    /**
     * Test that an Exception is thrown, when an integer is stored but a boolean expected.
     */
    @Test    void storesIntegerWhenBooleanIsExpected() {
        final String configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED;
        final Integer wrongDataType = 123;
        assertThatThrownBy(() -> tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongDataType))
                .as("Should not have worked as integer is not a boolean")
                .isInstanceOf(TenantConfigurationValidatorException.class);
    }

    /**
     * Test that an Exception is thrown, when an integer is stored as PollingTime.
     */
    @Test    void storesIntegerWhenPollingIntervalIsExpected() {
        final String configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;
        final Integer wrongDataType = 123;
        assertThatThrownBy(() -> tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongDataType))
                .as("Should not have worked as integer is not a time field")
                .isInstanceOf(TenantConfigurationValidatorException.class);
    }

    /**
     * Test that an Exception is thrown, when an invalid formatted string is stored as PollingTime.
     */
    @Test    void storesWrongFormattedStringAsPollingInterval() {
        final String configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;
        final String wrongFormatted = "wrongFormatted";
        assertThatThrownBy(() -> tenantConfigurationManagement.addOrUpdateConfiguration(configKey, wrongFormatted))
                .as("should not have worked as string is not a time field")
                .isInstanceOf(TenantConfigurationValidatorException.class);
    }

    /**
     * Test that an Exception is thrown, when an invalid formatted string is stored as PollingTime.
     */
    @Test    void storesTooSmallDurationAsPollingInterval() {
        final String configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;

        final String tooSmallDuration = DurationHelper
                .durationToFormattedString(DurationHelper.getDurationByTimeValues(0, 0, 1));
        assertThatThrownBy(() -> tenantConfigurationManagement.addOrUpdateConfiguration(configKey, tooSmallDuration))
                .as("Should not have worked as string has an invalid format")
                .isInstanceOf(TenantConfigurationValidatorException.class);
    }

    /**
     * Stores a correct formatted PollignTime and reads it again.
     */
    @Test    void storesCorrectDurationAsPollingInterval() {
        final String configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;

        final Duration duration = DurationHelper.getDurationByTimeValues(1, 2, 0);
        assertThat(duration).isEqualTo(Duration.ofHours(1).plusMinutes(2));

        tenantConfigurationManagement.addOrUpdateConfiguration(configKey, DurationHelper.durationToFormattedString(duration));

        final String storedDurationString = tenantConfigurationManagement.getConfigurationValue(configKey, String.class).getValue();
        assertThat(duration).isEqualTo(DurationHelper.formattedStringToDuration(storedDurationString));
    }

    /**
     * Request a config value in a wrong Value
     */
    @Test    void requestConfigValueWithWrongType() {
        assertThatThrownBy(() -> tenantConfigurationManagement.getConfigurationValue(
                TenantConfigurationKey.POLLING_TIME_INTERVAL, Serializable.class))
                .isInstanceOf(TenantConfigurationValidatorException.class);
    }

    /**
     * Verifies that every TenenatConfiguraationKeyName exists only once
     */
    @Test    void verifyThatAllKeysAreDifferent() {
        final Map<String, Void> keyNames = new HashMap<>();
        tenantConfigurationProperties.getConfigurationKeys().forEach(key -> {
            assertThat(keyNames)
                    .as("The key names are not unique")
                    .doesNotContainKey(key.getKeyName());
            keyNames.put(key.getKeyName(), null);
        });
    }

    /**
     * Get TenantConfigurationKeyByName
     */
    @Test    void getTenantConfigurationKeyByName() {
        final String configKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;
        assertThat(tenantConfigurationProperties.fromKeyName(configKey).getKeyName()).isEqualTo(configKey);
    }

    /**
     * Tenant configuration which is not declared throws exception
     */
    @Test    void storeTenantConfigurationWhichIsNotDeclaredThrowsException() {
        final String configKeyWhichDoesNotExists = "configKeyWhichDoesNotExists";
        assertThatThrownBy(() -> tenantConfigurationManagement.addOrUpdateConfiguration(configKeyWhichDoesNotExists, "value"))
                .as("Expected InvalidTenantConfigurationKeyException for tenant configuration key which is not declared")
                .isInstanceOf(InvalidTenantConfigurationKeyException.class);
    }
}