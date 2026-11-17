/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Deprecated reverse proxy trusted IPs check
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticationFiltersTrustedIpsTest {

    private static final String PROXY_IP = "10.0.0.1";
    private static final String DEVICE_IP = "203.0.113.7";

    @Mock
    private SecurityHeaderAuthenticator authenticator;

    private AuthenticationFilters.SecurityHeaderAuthenticationFilter filter;

    @BeforeEach
    @SuppressWarnings("removal")
    void setUp() {
        when(authenticator.log()).thenReturn(LoggerFactory.getLogger(AuthenticationFiltersTrustedIpsTest.class));
        final DdiSecurityProperties properties = new DdiSecurityProperties();
        properties.getRp().setTrustedIPs(List.of(PROXY_IP));
        filter = new AuthenticationFilters.SecurityHeaderAuthenticationFilter(authenticator, properties);
    }

    @Test
    void peerAddressIsCheckedInsteadOfForwardedRemoteAddress() throws Exception {
        // e.g. with Tomcat + NATIVE: the remote address is the device IP, the peer is the proxy
        final MockHttpServletRequest request = request(DEVICE_IP);
        request.setAttribute(AuthenticationFilters.PEER_ADDRESS_ATTRIBUTE, PROXY_IP);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(authenticator).authenticate(any());
    }

    @Test
    void remoteAddressIsCheckedWithoutPeerAddress() throws Exception {
        filter.doFilter(request(PROXY_IP), new MockHttpServletResponse(), new MockFilterChain());
        verify(authenticator).authenticate(any());
    }

    @Test
    void notTrustedPeerIsNotAuthenticated() throws Exception {
        final MockHttpServletRequest request = request(PROXY_IP);
        // the remote address (e.g. forwarded) is a trusted one, but the connection peer is not
        request.setAttribute(AuthenticationFilters.PEER_ADDRESS_ATTRIBUTE, DEVICE_IP);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(authenticator, never()).authenticate(any());
    }

    private static MockHttpServletRequest request(final String remoteAddress) {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/DEFAULT/controller/v1/device1");
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
