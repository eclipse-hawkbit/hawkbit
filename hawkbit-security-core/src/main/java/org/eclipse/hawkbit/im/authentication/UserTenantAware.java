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

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;

/**
 * A software provisioning user principal definition stored in the
 * {@link SecurityContext} which contains the user specific attributes.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserTenantAware extends User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String tenant;

    /**
     * @param username the username of the user
     * @param password the password of the user
     * @param authorities the authorities which the user has
     * @param tenant the tenant of the user
     */
    public UserTenantAware(final String username, final String password,
            final Collection<? extends GrantedAuthority> authorities, final String tenant) {
        super(username, password, authorities == null ? Collections.emptyList() : authorities);
        this.tenant = tenant;
    }

    /**
     * Create user without password and any credentials. For test purposes only.
     *
     * @param username the username of the user
     * @param tenant the tenant of the user
     */
    public UserTenantAware(final String username, String tenant) {
        this(username, "***", null, tenant);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}