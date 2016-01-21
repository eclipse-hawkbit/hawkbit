/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;

/**
 * Extends the {@link TenantAwareAuthenticationDetails} to web information to
 * retrieve also e.g. the remoteAddress of the {@link HttpServletRequest} when
 * authenticating the requested controller e.g. based on the security header and
 * trusted IP address we need the remote address of the http request to verify
 * the e.g. the reverse proxy is trusted and allowed to set the header.
 * 
 *
 *
 */
public class TenantAwareWebAuthenticationDetails extends TenantAwareAuthenticationDetails {

    private static final long serialVersionUID = 1L;
    private final String remoteAddress;

    /**
     * @param tenant
     *            the current tenant
     * @param remoteAddress
     *            the remote address of this web request
     * @param controller
     *            {@code true} indicates this is an controller HTTP request
     *            otherwise {@code false}.
     */
    public TenantAwareWebAuthenticationDetails(final String tenant, final String remoteAddress,
            final boolean controller) {
        super(tenant, controller);
        this.remoteAddress = remoteAddress;
    }

    /**
     * @return the remoteAddress
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }
}
