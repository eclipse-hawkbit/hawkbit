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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.hawkbit.im.authentication.SpRole;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;

import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SpringBootTest(properties = {
        "hawkbit.dmf.rabbitmq.enabled=false",
        "hawkbit.server.security.cors.enabled=true",
        "hawkbit.server.security.cors.allowedOrigins="
                + CorsTest.ALLOWED_ORIGIN_FIRST + ","
                + CorsTest.ALLOWED_ORIGIN_SECOND,
        "hawkbit.server.security.cors.exposedHeaders="
                + HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN})
@Feature("Integration Test - Security")
@Story("CORS")
public class CorsTest extends AbstractSecurityTest {

    final static String ALLOWED_ORIGIN_FIRST = "http://test.first.origin";
    final static String ALLOWED_ORIGIN_SECOND = "http://test.second.origin";

    private final static String INVALID_ORIGIN = "http://test.invalid.origin";
    private final static String INVALID_CORS_REQUEST = "Invalid CORS request";

    @Test
    @Description("Ensures that Cors is working.")
    @WithUser(authorities = SpRole.TENANT_ADMIN)
    public void validateCorsRequest() throws Exception {
        performOptionsRequestToRestWithOrigin(ALLOWED_ORIGIN_FIRST).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_FIRST));
        performOptionsRequestToRestWithOrigin(ALLOWED_ORIGIN_SECOND).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_SECOND));

        final String invalidOriginResponseBody = performOptionsRequestToRestWithOrigin(INVALID_ORIGIN)
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).andReturn().getResponse()
                .getContentAsString();
        assertThat(invalidOriginResponseBody).isEqualTo(INVALID_CORS_REQUEST);

        final String invalidCorsUrlResponseBody = performOptionsRequestToUrlWithOrigin(
                MgmtRestConstants.BASE_SYSTEM_MAPPING, ALLOWED_ORIGIN_FIRST).andExpect(status().isForbidden())
                        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).andReturn()
                        .getResponse().getContentAsString();
        assertThat(invalidCorsUrlResponseBody).isEqualTo(INVALID_CORS_REQUEST);
    }

    private ResultActions performOptionsRequestToRestWithOrigin(final String origin) throws Exception {
        return performOptionsRequestToUrlWithOrigin(MgmtRestConstants.BASE_V1_REQUEST_MAPPING, origin);
    }

    private ResultActions performOptionsRequestToUrlWithOrigin(final String url, final String origin) throws Exception {
        return mvc.perform(options(url).header("Access-Control-Request-Method", "GET").header("Origin", origin));
    }
}