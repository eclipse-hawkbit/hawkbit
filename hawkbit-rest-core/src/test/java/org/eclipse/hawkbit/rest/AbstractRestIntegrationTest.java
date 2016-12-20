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
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.eclipse.hawkbit.rest.util.FilterHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;

/**
 * Abstract Test for Rest tests.
 */
@SpringApplicationConfiguration(classes = { RestConfiguration.class, RepositoryApplicationConfiguration.class })
public abstract class AbstractRestIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FilterHttpResponse filterHttpResponse;

    @Override
    protected DefaultMockMvcBuilder createMvcWebAppContext() {
        final DefaultMockMvcBuilder createMvcWebAppContext = super.createMvcWebAppContext();
        return createMvcWebAppContext.addFilter(filterHttpResponse);
    }
}
