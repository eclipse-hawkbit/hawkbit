/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.im.authentication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * * Utility method for creation of <tt>GrantedAuthority</tt> collections etc.
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
