/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.jpa.JpaRepositoryConfiguration;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.repository.test.matcher.EventVerifier;
import org.eclipse.hawkbit.repository.test.util.CleanupTestExecutionListener;
import org.eclipse.hawkbit.repository.test.util.SharedSqlTestDatabaseExtension;
import org.eclipse.hawkbit.repository.test.util.TestLoggerExtension;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Test for {@link MgmtBasicAuthResource}.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: Basic authentication Userinfo Resource
 */
@ActiveProfiles({ "test" })
@ExtendWith({ TestLoggerExtension.class, SharedSqlTestDatabaseExtension.class })
@SpringBootTest
// destroy the context after each test class because otherwise we get problem when context is
// refreshed we e.g. get two instances of CacheManager which leads to very strange test failures.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// Cleaning repository will fire "delete" events. We won't count them to the test execution. So, the order execution between EventVerifier and
// Cleanup is important!
@TestExecutionListeners(
        listeners = { EventVerifier.class, CleanupTestExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@WebAppConfiguration
@AutoConfigureMockMvc
@ContextConfiguration(
        classes = { MgmtApiConfiguration.class, RestConfiguration.class, JpaRepositoryConfiguration.class, TestConfiguration.class })
class MgmtBasicAuthResourceTest {

    private static final String DEFAULT_TENANT = "DEFAULT";
    private static final String TEST_USER = "testUser";

    @Autowired
    protected WebApplicationContext webApplicationContext;
    @Autowired
    MockMvc defaultMock;

    /**
     * Test of userinfo api with basic authentication validation
     */
    @Test
    @WithUser(principal = TEST_USER, authorities = {"READ", "WRITE", "DELETE"})
    void validateBasicAuthWithUserDetails() throws Exception {
        withSecurityMock().perform(get(MgmtRestConstants.AUTH_V1_REQUEST_MAPPING))
                .andDo(MockMvcResultPrinter.print())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
                .andExpect(jsonPath("$.tenant", equalTo(DEFAULT_TENANT)))
                .andExpect(jsonPath("$.username", equalTo(TEST_USER)))
                .andExpect(jsonPath("$.permissions.size()", is(3)))
                .andExpect(jsonPath("$.permissions", hasItems("READ", "WRITE", "DELETE")));
    }

    /**
     * Test of userinfo api with invalid basic authentication fails
     */
    @Test
    void validateBasicAuthFailsWithInvalidCredentials() throws Exception {
        defaultMock.perform(get(MgmtRestConstants.AUTH_V1_REQUEST_MAPPING)
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuth("wrongUser", "wrongSecret")))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnauthorized());
    }

    private String getBasicAuth(final String username, final String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    private MockMvc withSecurityMock() {
        return createMvcWebAppContext(webApplicationContext).build();
    }

    private DefaultMockMvcBuilder createMvcWebAppContext(final WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context);
    }
}