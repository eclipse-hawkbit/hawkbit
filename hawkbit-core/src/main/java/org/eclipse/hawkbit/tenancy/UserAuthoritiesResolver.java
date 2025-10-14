/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy;

import java.util.Collection;

/**
 * The service responsible for making the lookup for user authorities/roles based on his tenant and username
 */
@FunctionalInterface
public interface UserAuthoritiesResolver {

    /**
     * User authorities/roles lookup based on the username and the tenant context
     *
     * @param username The username of the user
     * @return a {@link Collection} of authorities/roles for this user
     */
    Collection<String> getUserAuthorities(String username);
}