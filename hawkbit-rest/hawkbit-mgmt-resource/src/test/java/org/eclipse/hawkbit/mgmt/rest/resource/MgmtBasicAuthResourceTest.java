/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for {@link MgmtBasicAuthResource}.
 *
 */
@Feature("Component Tests - Management API")
@Story("Basic auth Userinfo Resource")
public class MgmtBasicAuthResourceTest extends AbstractManagementApiIntegrationTest{

    private static final String TEST_USER = "testUser";
    private static final String DEFAULT = "default";

    // Need another mockMvc to bypass the default Basic auth security
    @Autowired
    MockMvc mockMvcWithSecurity;

    @Test
    @Description("Test of userinfo api with basic auth validation")
    @WithUser(principal = TEST_USER)
    public void validateBasicAuthWithUserDetails() throws Exception {
        mvc.perform(get(MgmtRestConstants.AUTH_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
                .andExpect(jsonPath("$.username", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.tenant", equalTo(DEFAULT)));
    }

    @Test
    @Description("Test of userinfo api with invalid basic auth fails")
    public void validateBasicAuthFailsWithInvalidCredentials() throws Exception {
        mockMvcWithSecurity.perform(get(MgmtRestConstants.AUTH_V1_REQUEST_MAPPING)
                .header(HttpHeaders.AUTHORIZATION, getBasicAuth("wrongUser", "wrongSecret")))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnauthorized());
    }

    private String getBasicAuth(final String username, final String password) {
        return "Basic " + Base64Utils.encodeToString((username + ":" + password).getBytes());
    }
}
