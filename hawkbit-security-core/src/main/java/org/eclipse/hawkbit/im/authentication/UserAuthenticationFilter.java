/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.im.authentication;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Filter to integrate into the SP security filter-chain. The filter is called
 * in any remote call through HTTP except the SP login screen. E.g. using the SP
 * REST-API. To authenticate user e.g. using Basic-Authentication implement the
 * {@link #doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
 * method.
 *
 *
 *
 */
public interface UserAuthenticationFilter {

    /**
     * @see Filter#init(FilterConfig)
     *
     * @param filterConfig
     *            the filter config
     */
    void init(FilterConfig filterConfig) throws ServletException;

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     *
     * @param request
     *            the servlet request
     * @param response
     *            the servlet response
     * @param chain
     *            the filterchain
     * @throws IOException
     *             cannot read from request
     * @throws ServletException
     *             servlet exception
     */
    // this declaration of multiple checked exception is necessary so it's
    // aligned with the servlet API.
    @SuppressWarnings("squid:S1160")
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException;

    /**
     * @see Filter#destroy()
     */
    void destroy();

}
