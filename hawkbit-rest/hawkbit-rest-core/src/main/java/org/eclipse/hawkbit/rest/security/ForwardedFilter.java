/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.security;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Servlet filter adapter of the forwarded headers processing ({@link ForwardedResolver}) - for web servers other than embedded Tomcat (e.g.
 * Jetty), where the {@link ForwardedValve} can't be used. Has to be the first filter. Wraps the request, so the filters after it and the
 * application see the resolved client IP, scheme, host and port (the container itself, e.g. its access log, doesn't), and stores the
 * {@link ClientAddress} as request attribute.
 * <p/>
 * The response is wrapped too: an absolute redirect location with the original (not forwarded) scheme, host and port - e.g. built from the
 * not wrapped request - gets the forwarded ones. Relative locations stay relative.
 * <p/>
 * Applied for each dispatch (incl. error and async): an already wrapped request (e.g. async dispatch with the wrapped request) is passed as
 * is - its remote address is the resolved one, so it can't be resolved again.
 */
public class ForwardedFilter implements Filter {

    private final ForwardedResolver resolver;

    public ForwardedFilter(final ForwardedResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)
                || isForwarded(request)) {
            chain.doFilter(request, response);
            return;
        }

        final ForwardedResolver.Result result = resolver.resolve(httpRequest.getRemoteAddr(), httpRequest);
        request.setAttribute(ClientAddress.ATTRIBUTE, result.clientAddress());
        final ForwardedRequest forwardedRequest = new ForwardedRequest(httpRequest, result);
        chain.doFilter(forwardedRequest, new ForwardedResponse(httpResponse, httpRequest, forwardedRequest));
    }

    private static boolean isForwarded(final ServletRequest request) {
        return request instanceof ForwardedRequest
                || (request instanceof ServletRequestWrapper wrapper && wrapper.isWrapperFor(ForwardedRequest.class));
    }

    static class ForwardedRequest extends HttpServletRequestWrapper {

        private final ForwardedResolver.Result result;

        ForwardedRequest(final HttpServletRequest request, final ForwardedResolver.Result result) {
            super(request);
            this.result = result;
        }

        @Override
        public String getRemoteAddr() {
            return result.clientAddress().ip();
        }

        @Override
        public String getRemoteHost() {
            // no DNS lookup - the host is the IP
            return result.clientAddress().ip();
        }

        @Override
        public String getScheme() {
            return result.scheme() == null ? super.getScheme() : result.scheme();
        }

        @Override
        public boolean isSecure() {
            return result.secure() == null ? super.isSecure() : result.secure();
        }

        @Override
        public String getServerName() {
            return result.host() == null ? super.getServerName() : result.host();
        }

        @Override
        public int getServerPort() {
            return result.port() > 0 ? result.port() : super.getServerPort();
        }

        @Override
        public StringBuffer getRequestURL() {
            return new StringBuffer(origin(getScheme(), getServerName(), getServerPort())).append(getRequestURI());
        }
    }

    static class ForwardedResponse extends HttpServletResponseWrapper {

        private final HttpServletRequest originalRequest;
        private final HttpServletRequest forwardedRequest;

        ForwardedResponse(final HttpServletResponse response, final HttpServletRequest originalRequest,
                final HttpServletRequest forwardedRequest) {
            super(response);
            this.originalRequest = originalRequest;
            this.forwardedRequest = forwardedRequest;
        }

        @Override
        public void sendRedirect(final String location) throws IOException {
            super.sendRedirect(location(location));
        }

        @Override
        public void sendRedirect(final String location, final int sc) throws IOException {
            super.sendRedirect(location(location), sc);
        }

        @Override
        public void sendRedirect(final String location, final boolean clearBuffer) throws IOException {
            super.sendRedirect(location(location), clearBuffer);
        }

        @Override
        public void sendRedirect(final String location, final int sc, final boolean clearBuffer) throws IOException {
            super.sendRedirect(location(location), sc, clearBuffer);
        }

        // replaces the original origin (scheme://host[:port]) of an absolute location with the forwarded one, others are kept as is
        private String location(final String location) {
            if (location == null) {
                return null;
            }
            final URI uri;
            try {
                uri = new URI(location);
            } catch (final URISyntaxException e) {
                return location;
            }
            if (!uri.isAbsolute() || uri.getRawAuthority() == null) {
                return location;
            }
            final String scheme = uri.getScheme();
            final int port = uri.getPort() < 0 ? defaultPort(scheme) : uri.getPort();
            if (scheme.equalsIgnoreCase(originalRequest.getScheme()) && uri.getHost() != null
                    && unbracket(uri.getHost()).equalsIgnoreCase(unbracket(originalRequest.getServerName()))
                    && port == originalRequest.getServerPort()) {
                // after scheme://authority - the authority could also be found in the scheme (e.g. host "h" in "http")
                final String rest = location.substring(scheme.length() + 3 + uri.getRawAuthority().length());
                return origin(forwardedRequest.getScheme(), forwardedRequest.getServerName(), forwardedRequest.getServerPort()) + rest;
            }
            return location;
        }
    }

    private static String origin(final String scheme, final String host, final int port) {
        final StringBuilder origin = new StringBuilder(scheme).append("://");
        // IPv6 hosts in brackets (the server name could be without)
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            origin.append('[').append(host).append(']');
        } else {
            origin.append(host);
        }
        if (port > 0 && port != defaultPort(scheme)) {
            origin.append(':').append(port);
        }
        return origin.toString();
    }

    private static String unbracket(final String host) {
        return host != null && host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : String.valueOf(host);
    }

    private static int defaultPort(final String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }
}
