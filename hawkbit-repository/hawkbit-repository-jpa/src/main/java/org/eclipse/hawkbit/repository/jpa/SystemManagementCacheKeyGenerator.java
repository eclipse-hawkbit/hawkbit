/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.lang.reflect.Method;

import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link CurrentTenantCacheKeyGenerator}.
 *
 */
@Service
public class SystemManagementCacheKeyGenerator implements CurrentTenantCacheKeyGenerator {

    @Autowired
    private TenantAware tenantAware;

    private final ThreadLocal<String> createInitialTenant = new ThreadLocal<>();

    /**
     * A implementation of the {@link KeyGenerator} to generate a key based on
     * either the {@code createInitialTenant} thread local and the
     * {@link TenantAware}, but in case we are in a tenant creation with its
     * default types we need to use the tenant the current tenant which is
     * currently created and not the one currently in the {@link TenantAware}.
     *
     */
    public class CurrentTenantKeyGenerator implements KeyGenerator {
        @Override
        // Exception squid:S923 - override
        @SuppressWarnings({ "squid:S923" })
        public Object generate(final Object target, final Method method, final Object... params) {
            final String initialTenantCreation = createInitialTenant.get();
            if (initialTenantCreation == null) {
                return SimpleKeyGenerator.generateKey(tenantAware.getCurrentTenant().toUpperCase(),
                        tenantAware.getCurrentTenant().toUpperCase());
            }
            return SimpleKeyGenerator.generateKey(initialTenantCreation.toUpperCase(),
                    initialTenantCreation.toUpperCase());
        }
    }

    @Override
    @Bean
    public KeyGenerator currentTenantKeyGenerator() {
        return new CurrentTenantKeyGenerator();
    }

    ThreadLocal<String> getCreateInitialTenant() {
        return createInitialTenant;
    }

}
