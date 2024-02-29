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

import java.io.Serializable;

import lombok.Getter;
import lombok.ToString;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * An authentication details object
 * {@link AbstractAuthenticationToken#getDetails()} which is stored in the
 * spring security authentication token details to transport the principal and
 * tenant in the security context session.
 */
@Getter
@ToString
public class TenantAwareAuthenticationDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String tenant;
    private final boolean controller;

    /**
     * @param tenant
     *            the current tenant
     * @param controller
     *            boolean flag to indicate if this authenticated token is a
     *            controller authentication. {@code true} in case of
     *            authenticated controller otherwise {@code false}
     */
    public TenantAwareAuthenticationDetails(final String tenant, final boolean controller) {
        this.tenant = tenant;
        this.controller = controller;
    }
}
