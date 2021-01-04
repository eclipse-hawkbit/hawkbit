/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest;

import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.eclipse.hawkbit.rest.filter.ExcludePathAwareShallowETagFilter;
import org.eclipse.hawkbit.rest.util.FilterHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
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
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, RepositoryApplicationConfiguration.class,
        TestConfiguration.class, TestSupportBinderAutoConfiguration.class })
@AutoConfigureMockMvc
public abstract class AbstractRestIntegrationTest extends AbstractIntegrationTest {

    protected MockMvc mvc;

    @Autowired
    private FilterHttpResponse filterHttpResponse;

    @Autowired
    private CharacterEncodingFilter characterEncodingFilter;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @BeforeEach
    public void before() throws Exception {
        mvc = createMvcWebAppContext(webApplicationContext).build();
    }

    protected DefaultMockMvcBuilder createMvcWebAppContext(final WebApplicationContext context) {
        final DefaultMockMvcBuilder createMvcWebAppContext = MockMvcBuilders.webAppContextSetup(context);

        // CharacterEncodingFilter is needed for the encoding properties to be imported properly
        createMvcWebAppContext.addFilter(characterEncodingFilter);
        createMvcWebAppContext.addFilter(
                new ExcludePathAwareShallowETagFilter("/rest/v1/softwaremodules/{smId}/artifacts/{artId}/download",
                        "/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/**",
                        "/api/v1/downloadserver/**"));
        createMvcWebAppContext.addFilter(filterHttpResponse);

        return createMvcWebAppContext;
    }
}
