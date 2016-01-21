/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
