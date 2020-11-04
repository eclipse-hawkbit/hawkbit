/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.tenancy.UserAuthoritiesResolver;

/**
 * An Implementatoin of the {@link UserAuthoritiesResolver} that is based on in memory user permissions
 */
public class InMemoryUserAuthoritiesResolver implements UserAuthoritiesResolver {

    private final Map<String, List<String>> usernamesToPermissions;

    public InMemoryUserAuthoritiesResolver(final Map<String, List<String>> usernamesToPermissions) {
        this.usernamesToPermissions = usernamesToPermissions;
    }

    @Override
    public Collection<String> getUserAuthorities(final String tenant, final String username) {
        return usernamesToPermissions.get(username);
    }
}
