/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.qameta.allure.Description;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.firewall.RequestRejectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "hawkbit.server.security.allowedHostNames=localhost",
        "hawkbit.server.security.httpFirewallIgnoredPaths=/index.html" })
@Feature("Integration Test - Security")
@Story("Allowed Host Names")
public class AllowedHostNamesTest extends AbstractSecurityTest {

    @Test
    @Description("Tests whether a RequestRejectedException is thrown when not allowed host is used")
    public void allowedHostNameWithNotAllowedHost() {
        assertThatExceptionOfType(RequestRejectedException.class).isThrownBy(
                () -> mvc.perform(get("/").header(HttpHeaders.HOST, "www.google.com")));
    }

    @Test
    @Description("Tests whether request is redirected when allowed host is used")
    public void allowedHostNameWithAllowedHost() throws Exception {
        mvc.perform(get("/").header(HttpHeaders.HOST, "localhost")).andExpect(status().is3xxRedirection());
    }

    @Test
    @Description("Tests whether request without allowed host name and with ignored path end up with a client error")
    public void notAllowedHostnameWithIgnoredPath() throws Exception {
        mvc.perform(get("/index.html").header(HttpHeaders.HOST, "www.google.com"))
                .andExpect(status().is4xxClientError());
    }
}