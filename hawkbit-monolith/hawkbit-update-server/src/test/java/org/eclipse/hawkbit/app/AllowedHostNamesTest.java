/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "hawkbit.server.security.allowedHostNames=localhost",
        "hawkbit.server.security.httpFirewallIgnoredPaths=/index.html"
})
/**
 * Feature: Integration Test - Security<br/>
 * Story: Allowed Host Names
 */
class AllowedHostNamesTest extends AbstractSecurityTest {

    /**
     * Tests whether a RequestRejectedException is thrown when not allowed host is used
     */
    @Test
    void allowedHostNameWithNotAllowedHost() throws Exception {
        mvc.perform(get("/").header(HttpHeaders.HOST, "www.google.com")).andExpect(status().isBadRequest());
    }

    /**
     * Tests whether request is redirected when allowed host is used
     */
    @Test
    void allowedHostNameWithAllowedHost() throws Exception {
        mvc.perform(get("/").header(HttpHeaders.HOST, "localhost")).andExpect(status().is3xxRedirection());
    }

    /**
     * Tests whether request without allowed host name and with ignored path end up with a client error
     */
    @Test
    void notAllowedHostnameWithIgnoredPath() throws Exception {
        mvc.perform(get("/index.html").header(HttpHeaders.HOST, "www.google.com"))
                .andExpect(status().is4xxClientError());
    }
}