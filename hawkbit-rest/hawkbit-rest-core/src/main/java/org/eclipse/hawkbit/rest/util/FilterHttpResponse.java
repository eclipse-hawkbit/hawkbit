/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter is needed to autowire the {@link HttpServletResponse}.
 * 
 */
public class FilterHttpResponse implements Filter {

    private ThreadLocal<HttpServletResponse> threadLocalResponse = new ThreadLocal<>();

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // not needed
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        try {
            threadLocalResponse.set((HttpServletResponse) response);
            chain.doFilter(request, response);
        } finally {
            threadLocalResponse.remove();
        }
    }

    public HttpServletResponse getHttpServletReponse() {
        return threadLocalResponse.get();
    }

    @Override
    public void destroy() {
        threadLocalResponse = null;
    }

}
