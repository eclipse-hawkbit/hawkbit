/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.tenancy;

/**
 * Interface for components that are aware of the application's current tenant.
 *
 *
 *
 *
 */
public interface TenantAware {

    /**
     * Implementation might retrieve the current tenant from a session or
     * thread-local.
     *
     * @return the current tenant
     */
    String getCurrentTenant();

    /**
     * Gives the possibility to run a certain code under a specific given
     * {@code tenant}. Only the given {@link TenantRunner} is executed under the
     * specific tenant e.g. under control of an {@link ThreadLocal}. After the
     * {@link TenantRunner} it must be ensured that the original tenant before
     * this invocation is reset.
     *
     * @param tenant
     *            the tenant which the specific code should run
     * @param tenantRunner
     *            the runner which is implemented to run this specific code
     *            under the given tenant
     * @return the return type of the {@link TenantRunner}
     * @throws any
     *             kind of {@link RuntimeException}
     */
    <T> T runAsTenant(final String tenant, TenantRunner<T> tenantRunner);

    /**
     * An {@link TenantRunner} interface which allows to run specific code under
     * a given tenant by using the
     * {@link TenantAware#runAsTenant(String, TenantRunner)}.
     *
     *
     *
     *
     * @param <T>
     *            the return type of the runner
     */
    @FunctionalInterface
    interface TenantRunner<T> {
        /**
         * Called to run specific code and a given tenant.
         *
         * @return the return of the code block running under a certain tenant
         */
        T run();
    }
}
