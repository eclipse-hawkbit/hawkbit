/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.security.SpringSecurityAuditorAware;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class MDCHandler extends OncePerRequestFilter {

    private static String MDC_KEY_TENANT = "tenant";
    private static String MDC_KEY_USER = "user";

    private final SpringSecurityAuditorAware springSecurityAuditorAware;

    public MDCHandler(final SpringSecurityAuditorAware springSecurityAuditorAware) {
        this.springSecurityAuditorAware = springSecurityAuditorAware;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Object principal = authentication.getPrincipal();
        final String tenant;
        if (authentication.getDetails() instanceof TenantAwareAuthenticationDetails tenantAwareAuthenticationDetails) {
            tenant = tenantAwareAuthenticationDetails.getTenant();
        } else {
            tenant = null;
        }
        final String user = String.valueOf(springSecurityAuditorAware.getCurrentAuditor());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY_TENANT);
            MDC.remove(MDC_KEY_USER);
        }
    }
}
