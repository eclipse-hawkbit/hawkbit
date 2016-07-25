/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.im.authentication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Utility method for creation of <tt>GrantedAuthority</tt> collections etc.
 */
public final class PermissionUtils {

    private PermissionUtils() {

    }

    /**
     * Create {@link GrantedAuthority} by a special role.
     * 
     * @param roles
     *            the roles
     * @return a list of {@link GrantedAuthority}
     */
    public static List<GrantedAuthority> createAuthorityList(final Collection<String> roles) {
        final List<GrantedAuthority> authorities = new ArrayList<>(roles.size());

        for (final String role : roles) {
            authorities.add(new SimpleGrantedAuthority(role));
            // add spring security ROLE authority which is indicated by the
            // `ROLE_` prefix
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        return authorities;
    }

    /**
     * Returns all authorities.
     * 
     * @return a list of {@link GrantedAuthority}
     */
    public static List<GrantedAuthority> createAllAuthorityList() {
        return createAuthorityList(SpPermission.getAllAuthorities());
    }
}
