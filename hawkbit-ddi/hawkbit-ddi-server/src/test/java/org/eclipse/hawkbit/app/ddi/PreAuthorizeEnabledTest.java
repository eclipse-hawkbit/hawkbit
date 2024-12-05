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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

@Feature("Integration Test - Security")
@Story("PreAuthorized enabled")
@TestPropertySource(properties = { "spring.flyway.enabled=true" })
public class PreAuthorizeEnabledTest extends AbstractSecurityTest {

    @Test
    @Description("Tests whether request fail if a role is forbidden for the user")
    @WithUser(authorities = { SpPermission.READ_TARGET }, autoCreateTenant = false)
    public void failIfNoRole() throws Exception {
        mvc.perform(get("/DEFAULT/controller/v1/controllerId"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    @Description("Tests whether request succeed if a role is granted for the user")
    @WithUser(authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE }, autoCreateTenant = false)
    public void successIfHasRole() throws Exception {
        mvc.perform(get("/DEFAULT/controller/v1/controllerId"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value()));
    }
}