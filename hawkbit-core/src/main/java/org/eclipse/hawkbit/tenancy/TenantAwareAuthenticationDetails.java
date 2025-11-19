/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy;

import java.io.Serial;
import java.io.Serializable;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * An auth details object {@link AbstractAuthenticationToken#getDetails()} which is stored in the
 * spring security auth token details to transport the principal and tenant in the security context session.
 */
public record TenantAwareAuthenticationDetails(String tenant, boolean controller) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}