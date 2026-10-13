/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.hawkbit.context.AccessContext.asSystem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.test.util.CleanupTestExecutionListener;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Feature: Integration Test - Security<br/>
 * Story: Soft deleted tenant is inaccessible via Mgmt API
 */
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {
        "hawkbit.security.user.acme.tenant=" + MgmtSoftDeletedTenantTest.TENANT,
        "hawkbit.security.user.acme.password={noop}" + MgmtSoftDeletedTenantTest.PASSWORD,
        "hawkbit.security.user.acme.roles[0]=TENANT_ADMIN" })
@TestExecutionListeners(listeners = CleanupTestExecutionListener.class, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
class MgmtSoftDeletedTenantTest extends AbstractSecurityTest {

    static final String TENANT = "ACME";
    static final String PASSWORD = "acme";

    private static final String USER = "acme";
    private static final String CONTROLLER_ID = "softDeletedTenantControllerId";
    private static final String TARGETS = MgmtRestConstants.REST + "/v1/targets";

    @Autowired
    private SystemManagement systemManagement;

    @BeforeEach
    void createTenantWithTarget() throws Exception {
        // the cleanup listener uses SecurityContextSwitch - not registered as bean here (no "test" profile), so initialize it
        SecurityContextSwitch.getInstance().setSystemManagement(systemManagement);
        // the first authenticated request implicitly creates the tenant
        mvc.perform(post(TARGETS)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"controllerId\":\"" + CONTROLLER_ID + "\",\"name\":\"" + CONTROLLER_ID + "\"}]"))
                .andExpect(status().isCreated());
    }

    /**
     * Mgmt request for a soft deleted tenant is unauthorized and returns no targets. The access attempt is logged.
     */
    @Test
    void requestForSoftDeletedTenantIsUnauthorized(final CapturedOutput output) throws Exception {
        asSystem(() -> systemManagement.softDeleteTenant(TENANT, Instant.now().minusSeconds(1), Instant.now().plusSeconds(120)));

        final String body = listTargets().andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(CONTROLLER_ID);
        assertThat(output).contains("Soft deleted tenant access attempted via MGMT");
    }

    /**
     * Mgmt request of a restored tenant is successful.
     */
    @Test
    void requestForRestoredTenantIsSuccessful() throws Exception {
        asSystem(() -> systemManagement.softDeleteTenant(TENANT, Instant.now().minusSeconds(1), Instant.now().plusSeconds(120)));
        listTargets().andExpect(status().isUnauthorized());

        asSystem(() -> systemManagement.restoreTenant(TENANT));

        assertListsTarget();
    }

    /**
     * Tenant with a pending soft delete (configured delay) is still accessible.
     */
    @Test
    void requestForTenantWithPendingSoftDeleteIsSuccessful() throws Exception {
        asSystem(() -> systemManagement.softDeleteTenant(TENANT));

        assertListsTarget();
    }

    /**
     * Tenant becomes inaccessible once its soft delete time passed.
     */
    @Test
    void requestForTenantIsUnauthorizedOnceSoftDeleteTimePassed() throws Exception {
        asSystem(() -> systemManagement.softDeleteTenant(TENANT, Instant.now().plusSeconds(2), Instant.now().plusSeconds(120)));
        assertListsTarget();

        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> listTargets().andExpect(status().isUnauthorized()));
    }

    private void assertListsTarget() throws Exception {
        final String body = listTargets().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).contains(CONTROLLER_ID);
    }

    private ResultActions listTargets() throws Exception {
        return mvc.perform(get(TARGETS).header(HttpHeaders.AUTHORIZATION, basicAuth()));
    }

    private static String basicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString((USER + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    }
}
