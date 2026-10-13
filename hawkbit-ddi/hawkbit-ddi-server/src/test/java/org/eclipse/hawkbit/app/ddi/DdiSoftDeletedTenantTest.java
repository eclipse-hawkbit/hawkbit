/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app.ddi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.context.AccessContext.asSystem;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withTenantAndUserAndAllPermissions;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_TARGET_SECURITY_TOKEN_ENABLED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.CleanupTestExecutionListener;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Feature: Integration Test - Security<br/>
 * Story: Soft deleted tenant is inaccessible via DDI API
 */
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("test")
@Import(TestdataFactory.class)
@TestExecutionListeners(listeners = CleanupTestExecutionListener.class, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
class DdiSoftDeletedTenantTest extends AbstractSecurityTest {

    private static final String TENANT = "ACME";
    private static final String CONTROLLER_ID = "softDeletedTenantControllerId";
    private static final String CONTROLLER_BASE = "/{tenant}/controller/v1/{controllerId}";
    private static final String DOWNLOAD = CONTROLLER_BASE + "/softwaremodules/{smId}/artifacts/{filename}";
    private static final String FILENAME = "soft.bin";

    @Autowired
    private SystemManagement systemManagement;
    @Autowired
    private TenantConfigurationManagement tenantConfigurationManagement;
    @Autowired
    private DeploymentManagement deploymentManagement;
    @Autowired
    private TestdataFactory testdataFactory;

    private String securityToken;
    private long softwareModuleId;

    @BeforeEach
    void createTenantWithAssignedArtifact() {
        SecurityContextSwitch.runAs(withTenantAndUserAndAllPermissions(TENANT, "admin"), () -> {
            tenantConfigurationManagement.addOrUpdateConfiguration(AUTHENTICATION_TARGET_SECURITY_TOKEN_ENABLED, true);
            final Target target = testdataFactory.createTarget(CONTROLLER_ID);
            securityToken = target.getSecurityToken();
            final DistributionSet distributionSet = testdataFactory.createDistributionSet("soft");
            final SoftwareModule softwareModule = distributionSet.getModules().iterator().next();
            softwareModuleId = softwareModule.getId();
            final Artifact artifact = testdataFactory.createArtifact("soft delete artifact", softwareModuleId, FILENAME);
            assertThat(artifact).isNotNull();
            deploymentManagement.assignDistributionSets(
                    List.of(DeploymentRequest.builder(CONTROLLER_ID, distributionSet.getId()).build()), null);
        });
    }

    /**
     * Device poll for a soft deleted tenant is unauthorized. The access attempt is logged.
     */
    @Test
    void pollForSoftDeletedTenantIsUnauthorized(final CapturedOutput output) throws Exception {
        poll().andExpect(status().isOk());

        asSystem(() -> systemManagement.softDeleteTenant(TENANT, Instant.now().minusSeconds(1), Instant.now().plusSeconds(120)));

        poll().andExpect(status().isUnauthorized());
        assertThat(output).contains("Soft deleted tenant access attempted via DDI");
    }

    /**
     * Artifact download for a soft deleted tenant is unauthorized.
     */
    @Test
    void downloadForSoftDeletedTenantIsUnauthorized() throws Exception {
        download().andExpect(status().isOk());

        asSystem(() -> systemManagement.softDeleteTenant(TENANT, Instant.now().minusSeconds(1), Instant.now().plusSeconds(120)));

        download().andExpect(status().isUnauthorized());
    }

    private ResultActions poll() throws Exception {
        return mvc.perform(get(CONTROLLER_BASE, TENANT, CONTROLLER_ID).header(HttpHeaders.AUTHORIZATION, targetToken()));
    }

    private ResultActions download() throws Exception {
        return mvc.perform(get(DOWNLOAD, TENANT, CONTROLLER_ID, softwareModuleId, FILENAME).header(HttpHeaders.AUTHORIZATION, targetToken()));
    }

    private String targetToken() {
        return "TargetToken " + securityToken;
    }
}
