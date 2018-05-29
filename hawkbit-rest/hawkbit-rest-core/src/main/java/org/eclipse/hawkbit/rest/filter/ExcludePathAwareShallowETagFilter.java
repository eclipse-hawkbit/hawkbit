/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * An {@link ShallowEtagHeaderFilter} with exclusion paths to exclude some paths
 * where no ETag header should be generated due that calculating the ETag is an
 * expensive operation and the response output need to be copied in memory which
 * should be excluded in case of artifact downloads which could be big of size.
 */
public class ExcludePathAwareShallowETagFilter extends ShallowEtagHeaderFilter {

    private final String[] excludeAntPaths;
    private final AntPathMatcher antMatcher = new AntPathMatcher();

    /**
     * @param excludeAntPaths
     */
    public ExcludePathAwareShallowETagFilter(final String... excludeAntPaths) {
        this.excludeAntPaths = excludeAntPaths;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        final boolean shouldExclude = shouldExclude(request);
        if (shouldExclude) {
            filterChain.doFilter(request, response);
        } else {
            super.doFilterInternal(request, response, filterChain);
        }
    }

    private boolean shouldExclude(final HttpServletRequest request) {
        for (final String pattern : excludeAntPaths) {
            if (antMatcher.match(request.getContextPath() + pattern, request.getRequestURI())) {
                // exclude this request from eTag filter
                return true;
            }
        }
        return false;
    }
}
