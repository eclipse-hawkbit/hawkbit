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

import java.util.List;

import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.cache.TenantAwareCacheManager;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * A spring {@link TestExecutionListener} which cleans up the repository after each test-method.
 */
@Slf4j
public class CleanupTestExecutionListener extends AbstractTestExecutionListener {

    private static final Pageable PAGE = PageRequest.of(0, 400, Sort.by(Sort.Direction.ASC, "id"));

    @Override
    public void afterTestMethod(@NotNull final TestContext testContext) throws Exception {
        SecurityContextSwitch.callAsPrivileged(() -> {
            final ApplicationContext applicationContext = testContext.getApplicationContext();
            clearTestRepository(
                    applicationContext.getBean(TenantAwareCacheManager.class),
                    applicationContext.getBean(SystemSecurityContext.class),
                    applicationContext.getBean(SystemManagement.class));
            return null;
        });
    }

    private void clearTestRepository(
            final TenantAwareCacheManager cacheManager,
            final SystemSecurityContext systemSecurityContext,
            final SystemManagement systemManagement) {

        final List<String> tenants = systemSecurityContext.runAsSystem(() -> systemManagement.findTenants(PAGE).getContent());
        tenants.forEach(tenant -> {
            try {
                systemSecurityContext.runAsSystem(() -> {
                    systemManagement.deleteTenant(tenant);
                    return null;
                });
            } catch (final Exception e) {
                log.error("Error while delete tenant", e);
            }
        });
        cacheManager.getDirectCacheNames().forEach(name -> cacheManager.getDirectCache(name).clear());
    }
}