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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Forwarded headers processing - servlet filter
 */
class ForwardedFilterTest {

    private final ForwardedFilter filter =
            new ForwardedFilter(ForwardedResolver.of(new HawkbitSecurityProperties()));

    /**
     * Verifies that the wrapped request has the forwarded values and the client address is stored
     */
    @Test
    void wrapsRequest() throws Exception {
        final AtomicReference<HttpServletRequest> seen = new AtomicReference<>();
        filter.doFilter(forwardedRequest(), new MockHttpServletResponse(), new MockFilterChain() {

            @Override
            public void doFilter(final ServletRequest request, final ServletResponse response) {
                seen.set((HttpServletRequest) request);
            }
        });

        final HttpServletRequest request = seen.get();
        assertThat(request.getRemoteAddr()).isEqualTo("203.0.113.7");
        assertThat(request.getRemoteHost()).isEqualTo("203.0.113.7");
        assertThat(request.getScheme()).isEqualTo("https");
        assertThat(request.isSecure()).isTrue();
        assertThat(request.getServerName()).isEqualTo("example.com");
        assertThat(request.getServerPort()).isEqualTo(8443);
        assertThat(request.getRequestURL()).hasToString("https://example.com:8443/rest/v1/targets");
        assertThat(ClientAddress.of(request))
                .isEqualTo(new ClientAddress("10.0.0.1", "203.0.113.7", List.of(), null));
    }

    /**
     * Verifies that a not trusted peer is the client IP and its forwarded headers are ignored
     */
    @Test
    void notTrustedPeer() throws Exception {
        final MockHttpServletRequest original = forwardedRequest();
        original.setRemoteAddr("203.0.113.9");
        final AtomicReference<HttpServletRequest> seen = new AtomicReference<>();
        filter.doFilter(original, new MockHttpServletResponse(), new MockFilterChain() {

            @Override
            public void doFilter(final ServletRequest request, final ServletResponse response) {
                seen.set((HttpServletRequest) request);
            }
        });

        final HttpServletRequest request = seen.get();
        assertThat(request.getRemoteAddr()).isEqualTo("203.0.113.9");
        assertThat(request.getScheme()).isEqualTo("http");
        assertThat(request.getServerName()).isEqualTo("internal");
        assertThat(request.getRequestURL()).hasToString("http://internal:8080/rest/v1/targets");
    }

    /**
     * Verifies that the remote address of the wrapped request is the canonical client IP - an IPv4-mapped one (behind a dual-stack proxy)
     * as IPv4, a compressed IPv6 one in full form
     */
    @Test
    void remoteAddrIsCanonical() throws Exception {
        assertThat(wrappedRemoteAddr("::ffff:1.2.3.4, ::ffff:10.0.0.3")).isEqualTo("1.2.3.4");
        assertThat(wrappedRemoteAddr("[2001:db8::1]:5678")).isEqualTo("2001:db8:0:0:0:0:0:1");
    }

    private String wrappedRemoteAddr(final String forwardedFor) throws Exception {
        final MockHttpServletRequest original = new MockHttpServletRequest("GET", "/rest/v1/targets");
        original.setRemoteAddr("10.0.0.1");
        original.addHeader("X-Forwarded-For", forwardedFor);
        final AtomicReference<HttpServletRequest> seen = new AtomicReference<>();
        filter.doFilter(original, new MockHttpServletResponse(), new MockFilterChain() {

            @Override
            public void doFilter(final ServletRequest request, final ServletResponse response) {
                seen.set((HttpServletRequest) request);
            }
        });
        assertThat(ClientAddress.of(seen.get()).ip()).isEqualTo(seen.get().getRemoteAddr());
        return seen.get().getRemoteAddr();
    }

    /**
     * Verifies that an already wrapped request (e.g. async dispatch) isn't resolved again
     */
    @Test
    void alreadyWrappedRequestPassedAsIs() throws Exception {
        final AtomicReference<ServletRequest> first = new AtomicReference<>();
        filter.doFilter(forwardedRequest(), new MockHttpServletResponse(), new MockFilterChain() {

            @Override
            public void doFilter(final ServletRequest request, final ServletResponse response) {
                first.set(request);
            }
        });
        final AtomicReference<ServletRequest> second = new AtomicReference<>();
        filter.doFilter(first.get(), new MockHttpServletResponse(), new MockFilterChain() {

            @Override
            public void doFilter(final ServletRequest request, final ServletResponse response) {
                second.set(request);
            }
        });
        assertThat(second.get()).isSameAs(first.get());
    }

    /**
     * Verifies that an absolute redirect with the original origin gets the forwarded one, others (relative, other host) are kept
     */
    @Test
    void redirects() throws Exception {
        assertThat(redirect("/login")).isEqualTo("/login");
        assertThat(redirect("login?x=1")).isEqualTo("login?x=1");
        assertThat(redirect("http://internal:8080/login?x=1#y")).isEqualTo("https://example.com:8443/login?x=1#y");
        assertThat(redirect("http://other:8080/login")).isEqualTo("http://other:8080/login");
        assertThat(redirect("https://internal:8080/login")).isEqualTo("https://internal:8080/login");
        // the original host is also found in the scheme ("h" in "http")
        assertThat(redirect("h", "http://h:8080/login")).isEqualTo("https://example.com:8443/login");
    }

    private String redirect(final String location) throws Exception {
        return redirect("internal", location);
    }

    private String redirect(final String serverName, final String location) throws Exception {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockHttpServletRequest request = forwardedRequest();
        request.setServerName(serverName);
        filter.doFilter(request, response, new MockFilterChain() {

            @Override
            public void doFilter(final ServletRequest request, final ServletResponse response) throws java.io.IOException {
                ((HttpServletResponse) response).sendRedirect(location);
            }
        });
        return response.getRedirectedUrl();
    }

    private static MockHttpServletRequest forwardedRequest() {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/rest/v1/targets");
        request.setRemoteAddr("10.0.0.1");
        request.setScheme("http");
        request.setServerName("internal");
        request.setServerPort(8080);
        request.addHeader("X-Forwarded-For", "203.0.113.7");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "example.com");
        request.addHeader("X-Forwarded-Port", "8443");
        return request;
    }
}
