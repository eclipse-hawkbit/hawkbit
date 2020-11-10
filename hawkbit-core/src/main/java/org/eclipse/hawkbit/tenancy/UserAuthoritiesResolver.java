/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.tenancy;

import java.util.Collection;

/**
 * The service responsible for making the lookup for user authorities/roles
 * based on his tenant and username
 */
@FunctionalInterface
public interface UserAuthoritiesResolver {

    /**
     * User authorities/roles lookup based on the tenant and the username
     *
     * @param tenant
     *            The tenant that this user belongs to
     * @param username
     *            The username of the user
     * @return a {@link Collection} of authorities/roles for this user
     */
    Collection<String> getUserAuthorities(String tenant, String username);
}
