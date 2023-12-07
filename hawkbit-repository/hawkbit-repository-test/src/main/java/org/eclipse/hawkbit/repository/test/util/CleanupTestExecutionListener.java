/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
        SecurityContextSwitch.runAsPrivileged(() -> {
            final ApplicationContext applicationContext = testContext.getApplicationContext();
            new JpaTestRepositoryManagement(applicationContext.getBean(TenantAwareCacheManager.class),
                    applicationContext.getBean(SystemSecurityContext.class),
                    applicationContext.getBean(SystemManagement.class)).clearTestRepository();
            return null;
        });
    }
}
