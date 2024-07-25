/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;

/**
 * An pre-authenticated processing filter which extracts the principal from a
 * request URI and the credential from a request header.
 */
@Slf4j
public class HttpControllerPreAuthenticatedSecurityHeaderFilter extends AbstractHttpControllerAuthenticationFilter {

    private final String caCommonNameHeader;
    private final String caAuthorityNameHeader;

    /**
     * Creates a new {@link ControllerPreAuthenticatedSecurityHeaderFilter}, in
     * case the HTTP request matches the given pattern the principal is parsed
     * from the HTTP request with the given URI pattern, in case the URI pattern
     * does not match the current request then only the existence of the
     * configured header field is checked.
     * 
     * @param caCommonNameHeader
     *            the http-header which holds the common-name of the certificate
     * @param caAuthorityNameHeader
     *            the http-header which holds the ca-authority name of the
     *            certificate
     * @param tenantConfigurationManagement
     *            the tenant configuration management service to retrieve
     *            configuration properties to check if the header authentication
     *            is enabled for this tenant
     * @param tenantAware
     *            the tenant aware service to get configuration for the specific
     *            tenant
     * @param systemSecurityContext
     *            the system security context
     */
    public HttpControllerPreAuthenticatedSecurityHeaderFilter(final String caCommonNameHeader,
            final String caAuthorityNameHeader, final TenantConfigurationManagement tenantConfigurationManagement,
            final TenantAware tenantAware, final SystemSecurityContext systemSecurityContext) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
        this.caCommonNameHeader = caCommonNameHeader;
        this.caAuthorityNameHeader = caAuthorityNameHeader;
    }

    @Override
    protected PreAuthenticationFilter createControllerAuthenticationFilter() {
        return new ControllerPreAuthenticatedSecurityHeaderFilter(caCommonNameHeader, caAuthorityNameHeader,
                tenantConfigurationManagement, tenantAware, systemSecurityContext);
    }

    @Override
    protected Logger log() {
        return log;
    }
}