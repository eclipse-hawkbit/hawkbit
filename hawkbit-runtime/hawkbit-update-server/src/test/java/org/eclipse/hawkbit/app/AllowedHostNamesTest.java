/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.firewall.RequestRejectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SpringBootTest(properties = { "hawkbit.server.security.allowedHostNames=localhost" })
@Feature("Integration Test - Security")
@Story("Allowed Host Names")
public class AllowedHostNamesTest extends AbstractSecurityTest {

    @Test
    public void allowedHostNameWithNotAllowedHost() throws Exception {
        try {
            mvc.perform(get("/").header(HttpHeaders.HOST, "www.google.com"));
        } catch (final RequestRejectedException e) {
            // do nothing as this exception is expected
        }
    }

    @Test
    public void allowedHostNameWithAllowedHost() throws Exception {
        mvc.perform(get("/").header(HttpHeaders.HOST, "localhost")).andExpect(status().is3xxRedirection());
    }
} 