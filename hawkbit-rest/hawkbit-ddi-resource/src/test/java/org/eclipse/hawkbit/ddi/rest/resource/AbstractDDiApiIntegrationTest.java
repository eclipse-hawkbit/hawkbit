/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = { DdiApiConfiguration.class, RestConfiguration.class,
        RepositoryApplicationConfiguration.class, TestConfiguration.class, TestSupportBinderAutoConfiguration.class })
@TestPropertySource(locations = "classpath:/ddi-test.properties")
public abstract class AbstractDDiApiIntegrationTest extends AbstractRestIntegrationTest {

}
