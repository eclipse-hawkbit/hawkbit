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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Feature: Integration Test - Security<br/>
 * Story: CORS
 */
@SpringBootTest(
        properties = {
                "hawkbit.server.security.cors.enabled=true",
                "hawkbit.server.security.cors.allowedOrigins=" +
                        CorsTest.ALLOWED_ORIGIN_FIRST + "," +
                        CorsTest.ALLOWED_ORIGIN_SECOND,
                "hawkbit.server.security.cors.exposedHeaders=" +
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN })
class CorsTest extends AbstractSecurityTest {

    static final String ALLOWED_ORIGIN_FIRST = "http://test.first.origin";
    static final String ALLOWED_ORIGIN_SECOND = "http://test.second.origin";

    private static final String INVALID_ORIGIN = "http://test.invalid.origin";
    private static final String INVALID_CORS_REQUEST = "Invalid CORS request";

    /**
     * Ensures that Cors is working.
     */
    @Test
    @WithUser(authorities = SpRole.TENANT_ADMIN, autoCreateTenant = false)
    void validateCorsRequest() throws Exception {
        performOptionsRequestToRestWithOrigin(ALLOWED_ORIGIN_FIRST).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_FIRST));
        performOptionsRequestToRestWithOrigin(ALLOWED_ORIGIN_SECOND).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_SECOND));

        final String invalidOriginResponseBody = performOptionsRequestToRestWithOrigin(INVALID_ORIGIN)
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).andReturn().getResponse()
                .getContentAsString();
        assertThat(invalidOriginResponseBody).isEqualTo(INVALID_CORS_REQUEST);

        performOptionsRequestToUrlWithOrigin("/some_uri", ALLOWED_ORIGIN_FIRST)
                // TODO (Spring Boot 4 Migration): Since 4.0 at lest test returns 200 for not found - not going via CORS
                // before it was 403 + INVALID_CORS_REQUEST
//                .andExpect(status().isForbidden()).andExpect(content().string(INVALID_CORS_REQUEST)) // Spring Boot 3
                .andExpect(content().string(""))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).andReturn()
                .getResponse().getContentAsString();
    }

    private ResultActions performOptionsRequestToRestWithOrigin(final String origin) throws Exception {
        return performOptionsRequestToUrlWithOrigin(MgmtRestConstants.REST_V1, origin);
    }

    private ResultActions performOptionsRequestToUrlWithOrigin(final String url, final String origin) throws Exception {
        return mvc.perform(options(url).header("Access-Control-Request-Method", "GET").header("Origin", origin));
    }
}