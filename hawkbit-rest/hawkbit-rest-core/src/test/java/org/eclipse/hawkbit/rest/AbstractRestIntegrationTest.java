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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.hawkbit.repository.jpa.JpaRepositoryConfiguration;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.jpa.domain.Specification;
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
@ContextConfiguration(classes = { RestConfiguration.class, JpaRepositoryConfiguration.class, TestConfiguration.class })
@WebAppConfiguration
@AutoConfigureMockMvc
public abstract class AbstractRestIntegrationTest extends AbstractIntegrationTest {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected MockMvc mvc;
    @Autowired
    protected WebApplicationContext webApplicationContext;
    @Autowired
    private CharacterEncodingFilter characterEncodingFilter;

    @BeforeEach
    public void before() {
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

    protected static String toJson(final Object obj) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    protected static Specification<JpaAction> byDistributionSetId(final Long distributionSetId) {
        return (root, query, cb) -> cb.equal(root.get(JpaAction_.distributionSet).get(AbstractJpaBaseEntity_.id), distributionSetId);
    }
}