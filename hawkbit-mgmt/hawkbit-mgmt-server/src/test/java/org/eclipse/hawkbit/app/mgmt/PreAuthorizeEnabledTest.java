/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/**
 * Feature: Integration Test - Security<br/>
 * Story: PreAuthorized enabled
 */
@TestPropertySource(properties = "hawkbit.acm.access-controller.enabled=true")
class PreAuthorizeEnabledTest extends AbstractSecurityTest {

    /**
     * Tests whether request succeed if a role is granted for the user
     */
    @Test
    @WithUser(authorities = { SpPermission.READ_DISTRIBUTION_SET }, autoCreateTenant = false)
    void successIfHasRole() throws Exception {
        mvc.perform(get("/rest/v1/distributionsets")).andExpect(result ->
                assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value()));
    }

    /**
     * Tests whether request fail if a role is forbidden for the user
     */
    @Test
    @WithUser(authorities = { SpPermission.READ_TARGET }, autoCreateTenant = false)
    void failIfNoRole() throws Exception {
        mvc.perform(get("/rest/v1/distributionsets")).andExpect(result ->
                assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    /**
     * Tests whether request returns distribution set if a role with scope is granted for the user
     */
    @Test
    @WithUser(authorities = { "CREATE_DISTRIBUTION_SET", SpPermission.READ_DISTRIBUTION_SET + "/name==DsOne" }, autoCreateTenant = false)
    void successIfHasRoleWithScope() throws Exception {
        createDsOne("successIfHasRoleWithScope");
        mvc.perform(get("/rest/v1/distributionsets")).andExpect(result -> {
            assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(result.getResponse().getContentAsString()).contains("DsOne");
        });
    }

    /**
     * Tests whether request doesn't return distribution set if a role with scope doesn't grant access
     */
    @Test
    @WithUser(authorities = { "CREATE_DISTRIBUTION_SET", SpPermission.READ_DISTRIBUTION_SET + "/name==DsOne2" }, autoCreateTenant = false)
    void failIfHasNoForbiddingScope() throws Exception {
        createDsOne("failIfHasNoForbiddingScope");
        mvc.perform(get("/rest/v1/distributionsets")).andExpect(result -> {
            assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(result.getResponse().getContentAsString()).doesNotContain("DsOne");
        });
    }

    /**
     * Tests whether request succeed if a role is granted for the user
     */
    @Test
    @WithUser(authorities = { SpRole.TENANT_ADMIN }, autoCreateTenant = false)
    void successIfHasTenantAdminRole() throws Exception {
        mvc.perform(get("/rest/v1/distributionsets")).andExpect(result ->
                assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value()));
    }

    /**
     * Tests whether read tenant config request fail if a tenant config (or read) is not granted for the user
     */
    @Test
    @WithUser(authorities = { SpPermission.READ_TARGET }, autoCreateTenant = false)
    void onlyDSIfNoTenantConfig() throws Exception {
        mvc.perform(get("/rest/v1/system/configs")).andExpect(result -> {
            // returns default DS type because of READ_TARGET
            assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(new ObjectMapper().reader().readValue(result.getResponse().getContentAsString(), HashMap.class)).hasSize(1);
        });
    }

    /**
     * Tests whether read tenant config request succeed if a tenant config (not read explicitly) is granted for the user
     */
    @Test
    @WithUser(authorities = { SpPermission.TENANT_CONFIGURATION }, autoCreateTenant = false)
    void successIfHasTenantConfig() throws Exception {
        mvc.perform(get("/rest/v1/system/configs")).andExpect(result ->
                assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value()));
    }

    private void createDsOne(final String version) throws Exception {
        mvc.perform(post("/rest/v1/distributionsets")
                        .header("Content-Type", "application/json")
                        .content("""
                                [
                                  {
                                    "name": "DsOne",
                                    "version": "${version}"
                                  }
                                ]""".replace("${version}", version)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value()));
    }
}