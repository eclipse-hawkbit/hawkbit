/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds {@link TenantAware} service and makes it
 * accessible to beans which are not managed by spring, e.g. JPA entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public final class TenantAwareHolder {

    private static final TenantAwareHolder SINGLETON = new TenantAwareHolder();

    private TenantAware tenantAware;

    /**
     * @return the singleton {@link TenantAwareHolder} instance
     */
    public static TenantAwareHolder getInstance() {
        return SINGLETON;
    }

    @Autowired // spring setter injection
    public void setTenantAware(final TenantAware tenantAware) {
        this.tenantAware = tenantAware;
    }

    /**
     * @return the {@link TenantAware} service
     */
    public TenantAware getTenantAware() {
        return tenantAware;
    }
}