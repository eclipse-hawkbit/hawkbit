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

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;

import org.eclipse.hawkbit.AbstractIntegrationTestWithMongoDB;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.WithSpringAuthorityRule;
import org.eclipse.hawkbit.report.model.TenantUsage;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("System Management")
public class SystemManagementTest extends AbstractIntegrationTestWithMongoDB {

    @Test
    @Description("Ensures that findTenants returns all tenants and not only restricted to the tenant which currently is logged in")
    public void findTenantsReturnsAllTenantsNotOnlyWhichLoggedIn() throws Exception {
        assertThat(systemManagement.findTenants()).hasSize(1);

        createTestTenantsForSystemStatistics(2, 0, 0, 0);

        assertThat(systemManagement.findTenants()).hasSize(3);
    }

    @Test
    @Description("Checks that the system report calculates correctly the artifact size of all tenants in the system. It ignores deleted software modules with their artifacts.")
    public void systemUsageReportCollectsArtifactsOfAllTenants() throws Exception {
        // Prepare tenants
        createTestTenantsForSystemStatistics(2, 1234, 0, 0);

        // overall data
        assertThat(systemManagement.getSystemUsageStatistics().getOverallArtifacts()).isEqualTo(2);
        assertThat(systemManagement.getSystemUsageStatistics().getOverallArtifactVolumeInBytes()).isEqualTo(1234 * 2);

        // per tenant data
        final List<TenantUsage> tenants = systemManagement.getSystemUsageStatistics().getTenants();
        assertThat(tenants).hasSize(3);
        assertThat(tenants).containsOnly(new TenantUsage("default"),
                new TenantUsage("tenant0").setArtifacts(1).setOverallArtifactVolumeInBytes(1234),
                new TenantUsage("tenant1").setArtifacts(1).setOverallArtifactVolumeInBytes(1234));
    }

    @Test
    @Description("Checks that the system report calculates correctly the targets size of all tenants in the system")
    public void systemUsageReportCollectsTargetsOfAllTenants() throws Exception {
        // Prepare tenants
        createTestTenantsForSystemStatistics(2, 0, 100, 0);

        // overall data
        assertThat(systemManagement.getSystemUsageStatistics().getOverallTargets()).isEqualTo(200);
        assertThat(systemManagement.getSystemUsageStatistics().getOverallActions()).isEqualTo(0);

        // per tenant data
        final List<TenantUsage> tenants = systemManagement.getSystemUsageStatistics().getTenants();
        assertThat(tenants).hasSize(3);
        assertThat(tenants).containsOnly(new TenantUsage("default"), new TenantUsage("tenant0").setTargets(100),
                new TenantUsage("tenant1").setTargets(100));
    }

    @Test
    @Description("Checks that the system report calculates correctly the actions size of all tenants in the system")
    public void systemUsageReportCollectsActionsOfAllTenants() throws Exception {
        // Prepare tenants
        createTestTenantsForSystemStatistics(2, 0, 100, 2);

        // 2 tenants, 100 targets each, 2 deployments per target => 400
        assertThat(systemManagement.getSystemUsageStatistics().getOverallActions()).isEqualTo(400);

        // per tenant data
        final List<TenantUsage> tenants = systemManagement.getSystemUsageStatistics().getTenants();
        assertThat(tenants).hasSize(3);
        assertThat(tenants).containsOnly(new TenantUsage("default"),
                new TenantUsage("tenant0").setTargets(100).setActions(200),
                new TenantUsage("tenant1").setTargets(100).setActions(200));
    }

    private byte[] createTestTenantsForSystemStatistics(final int tenants, final int artifactSize, final int targets, final int updates)
            throws Exception {
        final Random randomgen = new Random();
        final byte random[] = new byte[artifactSize];
        randomgen.nextBytes(random);

        for (int i = 0; i < tenants; i++) {
            final String tenantname = "tenant" + i;
            securityRule.runAs(WithSpringAuthorityRule.withUserAndTenant("bumlux", tenantname), () -> {
                systemManagement.getTenantMetadata(tenantname);
                if (artifactSize > 0) {
                    createTestArtifact(random);
                    createDeletedTestArtifact(random);
                }
                if (targets > 0) {
                    final List<Target> createdTargets = createTestTargets(targets);
                    if (updates > 0) {
                        for (int x = 0; x < updates; x++) {
                            final DistributionSet ds = TestDataUtil.generateDistributionSet("to be deployed" + x,
                                    softwareManagement, distributionSetManagement, true);

                            deploymentManagement.assignDistributionSet(ds, createdTargets);
                        }
                    }
                }

                return null;
            });
        }

        return random;
    }

    private List<Target> createTestTargets(final int targets) {
        return targetManagement
                .createTargets(TestDataUtil.buildTargetFixtures(targets, "testTargetOfTenant", "testTargetOfTenant"));
    }

    private void createTestArtifact(final byte[] random) {
        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(), "file1", false);
    }

    private void createDeletedTestArtifact(final byte[] random) {
        final DistributionSet ds = TestDataUtil.generateDistributionSet("deleted garbage", softwareManagement,
                distributionSetManagement, true);
        ds.getModules().stream().forEach(module -> {
            artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), module.getId(), "file1", false);
            softwareManagement.deleteSoftwareModule(module);
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
