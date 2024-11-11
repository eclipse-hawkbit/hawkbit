/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * Abstract Test for Rest tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ContextConfiguration(classes = {
        RestConfiguration.class, RepositoryApplicationConfiguration.class, TestConfiguration.class })
@Import(TestChannelBinderConfiguration.class)
@WebAppConfiguration
@AutoConfigureMockMvc
public abstract class AbstractRestIntegrationTest extends AbstractIntegrationTest {

    protected MockMvc mvc;
    @Autowired
    protected WebApplicationContext webApplicationContext;
    @Autowired
    private CharacterEncodingFilter characterEncodingFilter;

    @BeforeEach
    public void before() throws Exception {
        mvc = createMvcWebAppContext(webApplicationContext).build();
    }

    protected DefaultMockMvcBuilder createMvcWebAppContext(final WebApplicationContext context) {
        final DefaultMockMvcBuilder createMvcWebAppContext = MockMvcBuilders.webAppContextSetup(context);

        // CharacterEncodingFilter is needed for the encoding properties to be imported properly
        createMvcWebAppContext.addFilter(characterEncodingFilter);
        createMvcWebAppContext.addFilter(
                new RestConfiguration.ExcludePathAwareShallowETagFilter("/rest/v1/softwaremodules/{smId}/artifacts/{artId}/download",
                        "/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/**",
                        "/api/v1/downloadserver/**"));

        return createMvcWebAppContext;
    }
}
