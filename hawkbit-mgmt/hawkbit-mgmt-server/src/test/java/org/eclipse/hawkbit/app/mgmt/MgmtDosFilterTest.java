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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.hawkbit.rest.security.DosFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Feature: Integration Test - Security<br/>
 * Story: Management API DoS filter runs before the authentication
 */
@TestPropertySource(properties = {
        "hawkbit.server.security.dos.filter.max-read=" + MgmtDosFilterTest.MAX_READ,
        "hawkbit.server.security.dos.filter.whitelist=" })
class MgmtDosFilterTest extends AbstractSecurityTest {

    static final int MAX_READ = 5;

    @Autowired
    private WebApplicationContext context;
    @Autowired
    @Qualifier("dosFilterREST")
    private FilterRegistrationBean<DosFilter> dosFilterRegistration;

    @Override
    @BeforeEach
    public void setup() {
        // the servlet filters aren't applied by MockMvc - add the DoS filter as registered: before the security filter chain
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(dosFilterRegistration.getFilter(), "/rest/*")
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    /**
     * The basic authentication rejects invalid credentials without continuing the chain - they shall be throttled anyway
     */
    @Test
    void invalidCredentialsAreThrottled() throws Exception {
        for (int i = 0; i < MAX_READ; i++) {
            mvc.perform(get("/rest/v1/targets").header("Authorization", "Basic aW52YWxpZDppbnZhbGlk")) // invalid:invalid
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/rest/v1/targets").header("Authorization", "Basic aW52YWxpZDppbnZhbGlk"))
                .andExpect(status().isTooManyRequests());
    }
}
