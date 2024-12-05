/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.ddi.security;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.security.controller.TenantAwareWebAuthenticationDetails;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.util.UrlUtils;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;

/**
 * An {@link AuthenticationDetailsSource} implementation which retrieves the
 * tenant from a request pattern {@link #TENANT_AWARE_CONTROLLER_PATTERN} and
 * stores the retrieved tenant in the {@link TenantAwareAuthenticationDetails}.
 */
@Slf4j
public class ControllerTenantAwareAuthenticationDetailsSource
        implements AuthenticationDetailsSource<HttpServletRequest, TenantAwareAuthenticationDetails> {

    private static final String TENANT_AWARE_CONTROLLER_PATTERN = "/{tenant}/controller/**";
    private static final String TENANT_PLACE_HOLDER = "tenant";
    private final AntPathMatcher pathExtractor;

    /**
     * Constructor.
     */
    public ControllerTenantAwareAuthenticationDetailsSource() {
        pathExtractor = new AntPathMatcher();
    }

    @Override
    public TenantAwareAuthenticationDetails buildDetails(final HttpServletRequest request) {
        return new TenantAwareWebAuthenticationDetails(getTenantFromRequestUri(request), request.getRemoteAddr(), true);
    }

    private String getTenantFromRequestUri(final HttpServletRequest request) {
        final String requestURI = request.getRequestURI();
        log.debug("retrieving tenant from URI request {}", requestURI);
        final String requestPathPattern = request.getContextPath() + TENANT_AWARE_CONTROLLER_PATTERN;
        if (!pathExtractor.match(requestPathPattern, requestURI)) {
            log.info("Controller request not matching tenant aware request pattern requestpath: {}, pattern {}",
                    requestURI, TENANT_AWARE_CONTROLLER_PATTERN);
            return null;
        }
        final Map<String, String> extractUriTemplateVariables = pathExtractor
                .extractUriTemplateVariables(requestPathPattern, requestURI);
        if (log.isTraceEnabled()) {
            log.trace("Parsed path variables {} using tenant {}", extractUriTemplateVariables,
                    extractUriTemplateVariables.get(TENANT_PLACE_HOLDER));
        }
        return UrlUtils.decodeUriValue(extractUriTemplateVariables.get(TENANT_PLACE_HOLDER));
    }
}
