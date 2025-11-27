/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties.Clients;
import org.eclipse.hawkbit.utils.IpUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: IP Util Test
 */
@ExtendWith(MockitoExtension.class)
class IpUtilTest {

    private static final String X_FORWARDED_FOR = HawkbitSecurityProperties.Clients.X_FORWARDED_FOR;
    private static final String KNOWN_REQUEST_HEADER = "bumlux";

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private Clients clientMock;

    @Mock
    private HawkbitSecurityProperties securityPropertyMock;

    /**
     * Tests create uri from request
     */
    @Test
    void getRemoteAddrFromRequestIfForwardedHeaderNotPresent() {
        final URI knownRemoteClientIP = IpUtil.createHttpUri("127.0.0.1");
        when(requestMock.getRemoteAddr()).thenReturn(knownRemoteClientIP.getHost());

        final URI remoteAddr = IpUtil.getClientIpFromRequest(requestMock, KNOWN_REQUEST_HEADER);

        // verify
        assertThat(remoteAddr).as("The remote address should be as the known client IP address")
                .isEqualTo(knownRemoteClientIP);
        verify(requestMock, times(1)).getHeader(KNOWN_REQUEST_HEADER);
        verify(requestMock, times(1)).getRemoteAddr();
    }

    /**
     * Tests create uri from request with masked IP when IP tracking is disabled
     */
    @Test
    void maskRemoteAddrIfDisabled() {
        final URI knownRemoteClientIP = IpUtil.createHttpUri("***");
        when(securityPropertyMock.getClients()).thenReturn(clientMock);
        when(clientMock.getRemoteIpHeader()).thenReturn(KNOWN_REQUEST_HEADER);
        when(clientMock.isTrackRemoteIp()).thenReturn(false);

        final URI remoteAddr = IpUtil.getClientIpFromRequest(requestMock, securityPropertyMock);

        assertThat(remoteAddr).as("The remote address should be as the known client IP address")
                .isEqualTo(knownRemoteClientIP);
        verify(requestMock, times(0)).getHeader(KNOWN_REQUEST_HEADER);
        verify(requestMock, times(0)).getRemoteAddr();
    }

    /**
     * Tests create uri from x forward header
     */
    @Test
    void getRemoteAddrFromXForwardedForHeader() {
        final URI knownRemoteClientIP = IpUtil.createHttpUri("10.99.99.1");
        when(requestMock.getHeader(X_FORWARDED_FOR)).thenReturn(knownRemoteClientIP.getHost());

        final URI remoteAddr = IpUtil.getClientIpFromRequest(requestMock, "X-Forwarded-For");

        assertThat(remoteAddr).as("The remote address should be as the known client IP address")
                .isEqualTo(knownRemoteClientIP);
        verify(requestMock, times(1)).getHeader(X_FORWARDED_FOR);
        verify(requestMock, times(0)).getRemoteAddr();
    }

    /**
     * Tests client uri from request
     */
    @Test
    void testCreateClientHttpUri() {
        checkHostInfoResolution("0:0:0:0:0:0:0:1", "[0:0:0:0:0:0:0:1]", true);
        checkHostInfoResolution("127.0.0.1", "127.0.0.1", true);
        checkHostInfoResolution("127.0.0.1:93493", "127.0.0.1", true);
        checkHostInfoResolution("myhost", "myhost", true);
        checkHostInfoResolution("myhost.my", "myhost.my", true);
        checkHostInfoResolution("myhost.my:4233", "myhost.my", true);
        checkHostInfoResolution("[0:0:0:0:0:0:0:1]", "[0:0:0:0:0:0:0:1]", true);
        checkHostInfoResolution("[0:0:0:0:0:0:0:1]:4233", "[0:0:0:0:0:0:0:1]", true);
    }

    /**
     * Tests client uri from request
     */
    @Test
    void testResolveClientIpFromHeader() {
        checkHostInfoResolution("0:0:0:0:0:0:0:1", "[0:0:0:0:0:0:0:1]", false);
        checkHostInfoResolution("127.0.0.1", "127.0.0.1", false);
        checkHostInfoResolution("127.0.0.1:93493", "127.0.0.1", false);
        checkHostInfoResolution("[0:0:0:0:0:0:0:1]", "[0:0:0:0:0:0:0:1]", false);
        checkHostInfoResolution("[0:0:0:0:0:0:0:1]:4233", "[0:0:0:0:0:0:0:1]", false);
    }

    /**
     * Tests create http uri ipv4 and ipv6
     */
    @Test
    void testCreateHttpUri() {
        final String ipv4 = "10.99.99.1";
        URI httpUri = IpUtil.createHttpUri(ipv4);
        assertHttpUri(ipv4, httpUri);

        final String host = "myhost";
        httpUri = IpUtil.createHttpUri(host);
        assertHttpUri(host, httpUri);

        final String ipv6 = "0:0:0:0:0:0:0:1";
        httpUri = IpUtil.createHttpUri(ipv6);
        assertHttpUri("[" + ipv6 + "]", httpUri);
    }

    /**
     * Tests create amqp uri ipv4 and ipv6
     */
    @Test
    void testCreateAmqpUri() {
        final String ipv4 = "10.99.99.1";
        URI amqpUri = IpUtil.createAmqpUri(ipv4, "path");
        assertAmqpUri(ipv4, amqpUri);
        final String ipv4Port = ipv4 + ":12000";
        amqpUri = IpUtil.createAmqpUri(ipv4Port, "path");
        assertAmqpUri(ipv4, amqpUri);

        final String host = "myhost";
        amqpUri = IpUtil.createAmqpUri(host, "path");
        assertAmqpUri(host, amqpUri);

        final String hostDots = "myhost.my";
        amqpUri = IpUtil.createAmqpUri(hostDots, "path");
        assertAmqpUri(hostDots, amqpUri);

        final String ipv6 = "0:0:0:0:0:0:0:1";
        amqpUri = IpUtil.createAmqpUri(ipv6, "path");
        assertAmqpUri("[" + ipv6 + "]", amqpUri);

        final String ipv6Braces = "[0:0:0:0:0:0:0:1]";
        amqpUri = IpUtil.createAmqpUri(ipv6Braces, "path");
        assertAmqpUri(ipv6Braces, amqpUri);
    }

    /**
     * Tests create invalid uri
     */
    @Test
    void testCreateInvalidUri() {

        final String host = "10.99.99.1";
        final URI testUri = IpUtil.createUri("test", host);

        assertThat(IpUtil.isAmqpUri(testUri)).as("The given URI is not an AMQP address").isFalse();
        assertThat(IpUtil.isHttpUri(testUri)).as("The given URI is not an HTTP address").isFalse();
        assertThat(host).as("The given host matches the URI host").isEqualTo(testUri.getHost());

        try {
            IpUtil.createUri(":/", host);
            Assertions.fail("Missing expected IllegalArgumentException due invalid URI");
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    private void checkHostInfoResolution(final String hostInfo, final String expectedHost, final boolean remoteAddress) {
        reset(requestMock);
        when(remoteAddress ? requestMock.getRemoteAddr() : requestMock.getHeader(KNOWN_REQUEST_HEADER)).thenReturn(hostInfo);

        final URI remoteAddr = IpUtil.getClientIpFromRequest(requestMock, KNOWN_REQUEST_HEADER);

        // verify
        assertThat(remoteAddr.getHost()).as("The remote address should be as the known client IP address")
                .isEqualTo(expectedHost);
        verify(requestMock, times(1)).getHeader(KNOWN_REQUEST_HEADER);
        if (remoteAddress) {
            verify(requestMock, times(1)).getRemoteAddr();
        }
    }

    private void assertHttpUri(final String host, final URI httpUri) {
        assertThat(IpUtil.isHttpUri(httpUri)).as("The given URI has an http scheme").isTrue();
        assertThat(IpUtil.isAmqpUri(httpUri)).as("The given URI is not an AMQP scheme").isFalse();
        assertThat(host).as("The URI hosts matches the given host").isEqualTo(httpUri.getHost());
        assertThat(httpUri.getScheme()).as("The given URI scheme is http").isEqualTo("http");
    }

    private void assertAmqpUri(final String host, final URI amqpUri) {

        assertThat(IpUtil.isAmqpUri(amqpUri)).as("The given URI is an AMQP scheme").isTrue();
        assertThat(IpUtil.isHttpUri(amqpUri)).as("The given URI is not an HTTP scheme").isFalse();
        assertThat(amqpUri.getHost()).as("The given host matches the URI host").isEqualTo(host);
        assertThat(amqpUri.getScheme()).as("The given URI has an AMQP scheme").isEqualTo("amqp");
        assertThat(amqpUri.getRawPath()).as("The given URI has an AMQP path").isEqualTo("/path");
    }
}
