/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A {@link TenantAware} implemenation which retrieves the ID of the tenant from
 * the {@link SecurityContext#getAuthentication()}
 * {@link Authentication#getDetails()} which holds the
 * {@link TenantAwareAuthenticationDetails} object.
 *
 *
 *
 *
 */
public class SecurityContextTenantAware implements TenantAware {

    private static final ThreadLocal<String> TENANT_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<AtomicInteger> RUN_AS_DEPTH = new ThreadLocal<>();

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.tenancy.TenantAware#getCurrentTenantId()
     */
    @Override
    public String getCurrentTenant() {
        if (TENANT_THREAD_LOCAL.get() != null) {
            return TENANT_THREAD_LOCAL.get();
        }
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            final Object authDetails = context.getAuthentication().getDetails();
            if (authDetails instanceof TenantAwareAuthenticationDetails) {
                return ((TenantAwareAuthenticationDetails) authDetails).getTenant();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.tenancy.TenantAware#runAsTenant(java.lang.String,
     * java.util.concurrent.Callable)
     */
    @Override
    public <T> T runAsTenant(final String tenant, final TenantRunner<T> callable) {
        AtomicInteger runAsDepth = RUN_AS_DEPTH.get();
        if (runAsDepth == null) {
            runAsDepth = new AtomicInteger(1);
            RUN_AS_DEPTH.set(runAsDepth);
        } else {
            runAsDepth.incrementAndGet();
        }
        TENANT_THREAD_LOCAL.set(tenant);
        try {
            return callable.run();
        } finally {
            if (runAsDepth.decrementAndGet() <= 0) {
                RUN_AS_DEPTH.remove();
                TENANT_THREAD_LOCAL.remove();
            }
        }
    }
}
