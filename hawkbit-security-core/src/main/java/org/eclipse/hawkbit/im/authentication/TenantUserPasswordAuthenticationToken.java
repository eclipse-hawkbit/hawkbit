/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.im.authentication;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * The authentication token which transports the username, password and the
 * tenant information for authentication.
 */
public class TenantUserPasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {

    // Exception squid:S1948 - no need to be Serializable
    @SuppressWarnings({ "squid:S1948" })
    final Object tenant;
    private static final long serialVersionUID = 1L;

    /**
     * Creating a new {@link TenantUserPasswordAuthenticationToken} as
     * {@link #isAuthenticated()} will return {@code false}.
     *
     * @param tenant the tenant to authenticate against
     * @param principal the principal to authenticate
     * @param credentials the credentials of the principal
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
     * @param tenant the tenant to authenticate against
     * @param principal the principal to authenticate
     * @param credentials the credentials of the principal
     * @param authorities the principal's authorities
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

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TenantUserPasswordAuthenticationToken other = (TenantUserPasswordAuthenticationToken) obj;
        if (tenant == null) {
            if (other.tenant != null) {
                return false;
            }
        } else if (!tenant.equals(other.tenant)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((tenant == null) ? 0 : tenant.hashCode());
        return result;
    }

}
