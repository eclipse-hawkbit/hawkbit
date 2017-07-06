/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import org.eclipse.hawkbit.cache.TenantAwareCacheManager;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * A spring {@link TestExecutionListener} which cleansup the repository after
 * each test-method.
 */
public class CleanupTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void afterTestMethod(final TestContext testContext) throws Exception {

        final ApplicationContext applicationContext = testContext.getApplicationContext();
        new JpaTestRepositoryManagement(applicationContext.getBean(TenantAwareCacheManager.class),
                applicationContext.getBean(SystemSecurityContext.class),
                applicationContext.getBean(SystemManagement.class)).clearTestRepository();
    }
}
