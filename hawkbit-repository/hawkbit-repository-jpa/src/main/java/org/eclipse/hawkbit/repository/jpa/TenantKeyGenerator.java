/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
