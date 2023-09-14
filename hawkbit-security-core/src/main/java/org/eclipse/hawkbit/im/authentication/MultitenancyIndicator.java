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

import org.springframework.security.authentication.AuthenticationProvider;

/**
 * Indicates if the SP server runs in multi-tenancy mode. By means e.g. if a
 * login screen needs to allow to specifiy the tenant to login.
 *
 * This can defere e.g. in case if the {@link AuthenticationProvider} allows
 * {@link TenantUserPasswordAuthenticationToken} tokens or not.
 *
 *
 */
@FunctionalInterface
public interface MultitenancyIndicator {

    /**
     * @return {@code true} if multi-tenancy is supported, otherwise
     *         {@code false}.
     */
    boolean isMultiTenancySupported();

}
