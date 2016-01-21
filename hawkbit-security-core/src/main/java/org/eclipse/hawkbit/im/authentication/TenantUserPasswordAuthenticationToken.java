/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.im.authentication;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * The authentication token which transports the username, password and the
 * tenant information for authentication.
 * 
 *
 *
 *
 */
public class TenantUserPasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {

    /**
    * 
    */
    private static final long serialVersionUID = 1L;

    final Object tenant;

    /**
     * 
     * Creating a new {@link TenantUserPasswordAuthenticationToken} as
     * {@link #isAuthenticated()} will return {@code false}.
     *
     * @param tenant
     *            the tenant to authenticate against
     * @param principal
     *            the principal to authenticate
     * @param credentials
     *            the credentials of the principal
     */
    public TenantUserPasswordAuthenticationToken(final Object tenant, final Object principal,
            final Object credentials) {
        super(principal, credentials);
        this.tenant = tenant;
    }

    /**
     * Creating a new {@link TenantUserPasswordAuthenticationToken} as
     * {@link #isAuthenticated()} will return {@code true}.
     * 
     * @param tenant
     *            the tenant to authenticate against
     * @param principal
     *            the principal to authenticate
     * @param credentials
     *            the credentials of the principal
     * @param authorities
     *            the principal's authorities
     */
    public TenantUserPasswordAuthenticationToken(final Object tenant, final Object principal, final Object credentials,
            final List<GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.tenant = tenant;
    }

    /**
     * @return the tenant
     */
    public Object getTenant() {
        return tenant;
    }
}
