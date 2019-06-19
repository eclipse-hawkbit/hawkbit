/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"hawkbit.dmf.rabbitmq.enabled=false", "hawkbit.server.security.cors.enabled=true", 
        "hawkbit.server.security.cors.allowedOrigins=http://test.origin,http://test.second.origin"})
@Feature("Integration Test - Security")
@Story("CORS")
public class CorsTest {

    @Autowired
    private WebApplicationContext context;

    public MockMvc mvc;

    @Before
    public void setup() {
        final DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity()).dispatchOptions(true);
        mvc = builder.build();
    }

    @WithUserDetails("admin")
    @Test
    @Description("Ensures that Cors is working.")
    public void validateCorsRequest() throws Exception {
        performRequestWithOrigin("http://test.origin").andExpect(status().isOk());
        performRequestWithOrigin("http://test.second.origin").andExpect(status().isOk());

        final String responseBody = performRequestWithOrigin("http://test.invalid.origin")
                .andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString();
        assertThat(responseBody).isEqualTo("Invalid CORS request");
    }

    private ResultActions performRequestWithOrigin(final String origin) throws Exception {
        return mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)
                .header("Access-Control-Request-Method", "GET").header("Origin", origin));
    }
}