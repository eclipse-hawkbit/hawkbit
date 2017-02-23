/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.im.authentication;

import java.io.Serializable;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * An authentication details object
 * {@link AbstractAuthenticationToken#getDetails()} which is stored in the
 * spring security authentication token details to transport the principal and
 * tenant in the security context session.
 */
public class TenantAwareAuthenticationDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String tenant;
    private final boolean controller;

    /**
     * @param tenant
     *            the current tenant
     * @param controller
     *            boolean flag to indicate if this authenticated token is a
     *            controller authentication. {@code true} in case of
     *            authenticated controller otherwise {@code false}
     */
    public TenantAwareAuthenticationDetails(final String tenant, final boolean controller) {
        this.tenant = tenant;
        this.controller = controller;
    }

    /**
     * @return the tenant
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * @return the controller
     */
    public boolean isController() {
        return controller;
    }

    @Override
    public String toString() {
        return "TenantAwareAuthenticationDetails [tenant=" + tenant + ", controller=" + controller + "]";
    }

}
