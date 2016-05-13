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
import org.springframework.stereotype.Service;

/**
 * {@link KeyGenerator} for tenant related caches.
 *
 */
@Service
public class TenantKeyGenerator implements KeyGenerator {

    @Autowired
    private TenantAware tenantAware;

    @Override
    public Object generate(final Object target, final Method method, final Object... params) {
        return tenantAware.getCurrentTenant().toUpperCase();
    }

}
