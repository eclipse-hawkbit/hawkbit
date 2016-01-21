/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.concurrent.Callable;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.WithSpringAuthorityRule;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;

import ru.yandex.qatools.allure.annotations.Description;

/**
 *
 *
 *
 */
public class SystemManagementTest extends AbstractIntegrationTest {

    @Test
    @Description("Ensures that findTenants returns all tenants and not only restricted to the tenant which currently is logged in")
    public void findTenantsReturnsAllTenantsNotOnlyWhichLoggedIn() throws Exception {
        final String knownTenant1 = "knownTenant1";
        final String knownTenant2 = "knownTenant2";
        securityRule.runAs(WithSpringAuthorityRule.withUserAndTenant("bumlux", knownTenant1), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                systemManagement.getTenantMetadata(knownTenant1);
                return null;
            }
        });

        securityRule.runAs(WithSpringAuthorityRule.withUserAndTenant("bumlux", knownTenant2), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                systemManagement.getTenantMetadata(knownTenant2);
                return null;
            }
        });
    }

    @Test
    @Description("Tests that tenant specific configuration can be persisted and in case the tenant does not have specific configuration the default from environment is used instead.")
    public void storeTenantSpecificConfiguration() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final String envPropertyDefault = environment.getProperty(configKey.getDefaultKeyName());
        assertThat(envPropertyDefault).isNotNull();

        // get the configuration from the system management
        final String defaultConfigValue = systemManagement.getConfigurationValue(configKey, String.class);
        assertThat(envPropertyDefault).isEqualTo(defaultConfigValue);

        // update the tenant specific configuration
        final String newConfigurationValue = "thisIsAnotherValueForPolling";
        assertThat(newConfigurationValue).isNotEqualTo(defaultConfigValue);
        systemManagement
                .addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), newConfigurationValue));

        // verify that new configuration value is used
        final String updatedConfigurationValue = systemManagement.getConfigurationValue(configKey, String.class);
        assertThat(updatedConfigurationValue).isEqualTo(newConfigurationValue);
        assertThat(systemManagement.getTenantConfigurations()).hasSize(1);
    }

    @Test
    @Description("Tests that the tenant specific configuration can be updated")
    public void updateTenantSpecifcConfiguration() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final String value1 = "firstValue";
        final String value2 = "secondValue";

        // add value first
        systemManagement.addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), value1));
        assertThat(systemManagement.getConfigurationValue(configKey, String.class)).isEqualTo(value1);

        // update to value second
        systemManagement.addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), value2));
        assertThat(systemManagement.getConfigurationValue(configKey, String.class)).isEqualTo(value2);
    }

    @Test
    @Description("Tests that the configuration value can be converted from String to Integer automatically")
    public void tenantConfigurationValueConversion() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final Integer value1 = 123;
        systemManagement
                .addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), String.valueOf(value1)));
        assertThat(systemManagement.getConfigurationValue(configKey, Integer.class)).isEqualTo(value1);
    }

    @Test(expected = ConversionFailedException.class)
    @Description("Tests that the get configuration throws exception in case the value cannot be automatically converted from String to Integer")
    public void wrongTenantConfigurationValueConversionThrowsException() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED;
        final String value1 = "thisIsNotANumber";
        // add value as String
        systemManagement
                .addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), String.valueOf(value1)));
        // try to get it as Integer
        systemManagement.getConfigurationValue(configKey, Integer.class);
    }

    @Test
    @Description("Tests that a deletion of a tenant specific configuration deletes it from the database.")
    public void deleteConfigurationReturnNullConfiguration() {
        final TenantConfigurationKey configKey = TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY;

        // gateway token does not have default value so no configuration value
        // is should be available
        final String defaultConfigValue = systemManagement.getConfigurationValue(configKey, String.class);
        assertThat(defaultConfigValue).isNull();

        // update the tenant specific configuration
        final String newConfigurationValue = "thisIsAnotherValueForPolling";
        assertThat(newConfigurationValue).isNotEqualTo(defaultConfigValue);
        systemManagement
                .addOrUpdateConfiguration(new TenantConfiguration(configKey.getKeyName(), newConfigurationValue));

        // verify that new configuration value is used
        final String updatedConfigurationValue = systemManagement.getConfigurationValue(configKey, String.class);
        assertThat(updatedConfigurationValue).isEqualTo(newConfigurationValue);

        // delete the tenant specific configuration
        systemManagement.deleteConfiguration(configKey);
        // ensure that now gateway token is set again, because is deleted and
        // must be null now
        assertThat(systemManagement.getConfigurationValue(configKey, String.class)).isNull();
    }
}
