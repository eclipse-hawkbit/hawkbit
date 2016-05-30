/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

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
