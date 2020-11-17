/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.tenancy.UserAuthoritiesResolver;

/**
 * An implementation of the {@link UserAuthoritiesResolver} that is based on
 * in-memory user permissions.
 */
public class InMemoryUserAuthoritiesResolver implements UserAuthoritiesResolver {

    private final Map<String, List<String>> usernamesToAuthorities;

    /**
     * Constructs the resolver based on the given authority lookup map.
     * 
     * @param usernamesToAuthorities
     *            The authority map to read from. Must not be <code>null</code>.
     */
    public InMemoryUserAuthoritiesResolver(final Map<String, List<String>> usernamesToAuthorities) {
        this.usernamesToAuthorities = usernamesToAuthorities;
    }

    @Override
    public Collection<String> getUserAuthorities(final String tenant, final String username) {
        // we can ignore the tenant here (no multi-tenancy by default)
        final Collection<String> authorities = usernamesToAuthorities.get(username);
        if (authorities == null) {
            return Collections.emptyList();
        }
        return authorities;
    }

}
