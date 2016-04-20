/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.system.rest.resource;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;

import org.eclipse.hawkbit.AbstractIntegrationTestWithMongoDB;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.WithSpringAuthorityRule;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Test;
import org.springframework.http.MediaType;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 *
 *
 */
@Features("Component Tests - System API")
@Stories("System Management Resource")
public class SystemManagementResourceTest extends AbstractIntegrationTestWithMongoDB {

    @Test
    @WithUser(tenantId = "mytenant", authorities = { SpPermission.SYSTEM_ADMIN })
    @Description("Tests that the system is able to collect statistics for the entire system.")
    public void collectSystemStatistics() throws Exception {
        createTestTenantsForSystemStatistics(2, 2000, 100, 2);

        mvc.perform(get("/system/admin/usage").accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$tenantStats.[?(@.tenantName==tenant0)][0].targets", equalTo(100)))
                .andExpect(jsonPath("$tenantStats.[?(@.tenantName==tenant0)][0].overallArtifactVolumeInBytes",
                        equalTo(2000)))
                .andExpect(jsonPath("$tenantStats.[?(@.tenantName==tenant0)][0].artifacts", equalTo(1)))
                .andExpect(jsonPath("$tenantStats.[?(@.tenantName==tenant0)][0].actions", equalTo(200)))
                .andExpect(jsonPath("$overallTargets", equalTo(200)))
                .andExpect(jsonPath("$overallArtifacts", equalTo(2)))
                .andExpect(jsonPath("$overallArtifactVolumeInBytes", equalTo(4000)))
                .andExpect(jsonPath("$overallActions", equalTo(400)))
                .andExpect(jsonPath("$overallTenants", equalTo(4)));
    }

    @Test
    @WithUser(tenantId = "mytenant", authorities = { SpPermission.DELETE_TARGET, SpPermission.DELETE_REPOSITORY,
            SpPermission.CREATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    @Description("Tests that the system is not able to collect statistics for the entire system if the .")
    public void collectSystemStatisticsWithMissingPermissionFails() throws Exception {

        mvc.perform(get("/system/admin/usage").accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUser(tenantId = "mytenant", allSpPermissions = true)
    @Description("Tests that a tenant can be deletd by API.")
    public void deleteTenant() throws Exception {

        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        assertThat(distributionSetManagement.findDistributionSetById(dsA.getId())).isNotNull();

        mvc.perform(delete("/system/admin/tenants/mytenant")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(distributionSetManagement.findDistributionSetById(dsA.getId())).isNull();

    }

    @Test
    @WithUser(tenantId = "mytenant", authorities = { SpPermission.DELETE_TARGET, SpPermission.DELETE_REPOSITORY,
            SpPermission.CREATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    @Description("Tenant deletion is only possible for SYSTEM_ADMINs. Repository or target delete is not sufficient.")
    public void deleteTenantFailsMissingPermission() throws Exception {

        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        assertThat(distributionSetManagement.findDistributionSetById(dsA.getId())).isNotNull();

        mvc.perform(delete("/system/admin/tenants/mytenant")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(distributionSetManagement.findDistributionSetById(dsA.getId())).isNotNull();
    }

    @Test
    public void getCachesReturnStatus200() throws Exception {
        mvc.perform(get("/system/admin/caches")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

    @Test
    public void invalidateCachesReturnStatus200() throws Exception {
        mvc.perform(delete("/system/admin/caches")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

    private byte[] createTestTenantsForSystemStatistics(final int tenants, final int artifactSize, final int targets,
            final int updates) throws Exception {
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

}
