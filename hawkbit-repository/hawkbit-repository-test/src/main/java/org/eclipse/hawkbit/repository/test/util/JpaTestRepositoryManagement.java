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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.cache.TenantAwareCacheManager;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

@Slf4j
public class JpaTestRepositoryManagement {

    private static final Pageable PAGE = PageRequest.of(0, 400, Sort.by(Direction.ASC, "id"));

    private final TenantAwareCacheManager cacheManager;

    private final SystemSecurityContext systemSecurityContext;

    private final SystemManagement systemManagement;

    /**
     * Constructor.
     *
     * @param cacheManager the cachemanager
     * @param systemSecurityContext the systemSecurityContext
     * @param systemManagement the systemManagement
     */
    public JpaTestRepositoryManagement(final TenantAwareCacheManager cacheManager,
            final SystemSecurityContext systemSecurityContext, final SystemManagement systemManagement) {
        this.cacheManager = cacheManager;
        this.systemSecurityContext = systemSecurityContext;
        this.systemManagement = systemManagement;
    }

    public void clearTestRepository() {
        deleteAllRepos();
        cacheManager.getDirectCacheNames().forEach(name -> cacheManager.getDirectCache(name).clear());
    }

    public void deleteAllRepos() {
        final List<String> tenants = systemSecurityContext
                .runAsSystem(() -> systemManagement.findTenants(PAGE).getContent());
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
    }
}
