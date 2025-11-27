/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app.ddi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
@TestPropertySource(properties = { "spring.flyway.enabled=true", "hawkbit.acm.access-controller.enabled=false" })
class PreAuthorizeEnabledTest extends AbstractSecurityTest {

    /**
     * Tests whether request fail if a role is forbidden for the user
     */
    @Test
    @WithUser(authorities = { SpPermission.READ_TARGET }, autoCreateTenant = false)
    void failIfNoRole() throws Exception {
        mvc.perform(get("/DEFAULT/controller/v1/controllerId"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    /**
     * Tests whether request succeed if a role is granted for the user
     */
    @Test
    @WithUser(authorities = { SpRole.CONTROLLER_ROLE }, autoCreateTenant = false)
    void successIfHasRole() throws Exception {
        mvc.perform(get("/DEFAULT/controller/v1/controllerId"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value()));
    }
}