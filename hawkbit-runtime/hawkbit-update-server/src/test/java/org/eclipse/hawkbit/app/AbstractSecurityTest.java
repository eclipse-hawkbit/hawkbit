/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import org.eclipse.hawkbit.repository.test.util.MsSqlTestDatabase;
import org.eclipse.hawkbit.repository.test.util.MySqlTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = { "hawkbit.dmf.rabbitmq.enabled=false" })
@TestExecutionListeners(listeners = { MySqlTestDatabase.class,
        MsSqlTestDatabase.class }, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
@DirtiesContext
public abstract class AbstractSecurityTest {

    @Autowired
    private WebApplicationContext context;

    protected MockMvc mvc;

    @BeforeEach
    public void setup() {
        final DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity()).dispatchOptions(true);
        mvc = builder.build();
    }

}