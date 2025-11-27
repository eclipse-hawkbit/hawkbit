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

import static org.eclipse.hawkbit.context.AccessContext.asSystem;

import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager.CacheEvictEvent;
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
        SecurityContextSwitch.asPrivileged(() -> {
            final ApplicationContext applicationContext = testContext.getApplicationContext();
            clearTestRepository(applicationContext.getBean(SystemManagement.class));
            return null;
        });
    }

    private void clearTestRepository(final SystemManagement systemManagement) {
        asSystem(() -> systemManagement.forEachTenantAsSystem(tenant -> {
            try {
                asSystem(() -> systemManagement.deleteTenant(tenant));
            } catch (final Exception e) {
                log.error("Error while delete tenant", e);
            }
        }));
        // evict global cache
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new CacheEvictEvent.Default(null, null, null));
    }
}