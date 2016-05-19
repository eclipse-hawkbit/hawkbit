/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;

/**
 * An {@link AuthenticationDetailsSource} implementation which retrieves the
 * tenant from a request pattern {@link #TENANT_AWARE_CONTROLLER_PATTERN} and
 * stores the retrieved tenant in the {@link TenantAwareAuthenticationDetails}.
 *
 */
public class ControllerTenantAwareAuthenticationDetailsSource
        implements AuthenticationDetailsSource<HttpServletRequest, TenantAwareAuthenticationDetails> {

    private static final String TENANT_AWARE_CONTROLLER_PATTERN = "/{tenant}/controller/**";
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ControllerTenantAwareAuthenticationDetailsSource.class);
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
        LOGGER.debug("retrieving tenant from URI request {}", requestURI);
        final String requestPathPattern = request.getContextPath() + TENANT_AWARE_CONTROLLER_PATTERN;
        if (!pathExtractor.match(requestPathPattern, requestURI)) {
            LOGGER.info("Controller request not matching tenant aware request pattern requestpath: {}, pattern {}",
                    requestURI, TENANT_AWARE_CONTROLLER_PATTERN);
            return null;
        }
        final Map<String, String> extractUriTemplateVariables = pathExtractor
                .extractUriTemplateVariables(requestPathPattern, requestURI);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Parsed path variables {} using tenant {}", extractUriTemplateVariables,
                    extractUriTemplateVariables.get(TENANT_PLACE_HOLDER));
        }
        return extractUriTemplateVariables.get(TENANT_PLACE_HOLDER);
    }
}
