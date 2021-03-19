/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.util;

import static com.google.common.net.HttpHeaders.X_FORWARDED_FOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties.Clients;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Feature("Unit Tests - Security")
@Story("IP Util Test")
public class IpUtilTest {

    private static final String KNOWN_REQUEST_HEADER = "bumlux";

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private Clients clientMock;

    @Mock
    private HawkbitSecurityProperties securityPropertyMock;

    @Test
    @Description("Tests create uri from request")
    public void getRemoteAddrFromRequestIfForwaredHeaderNotPresent() {

        final URI knownRemoteClientIP = IpUtil.createHttpUri("127.0.0.1");
        when(requestMock.getRemoteAddr()).thenReturn(knownRemoteClientIP.getHost());

        final URI remoteAddr = IpUtil.getClientIpFromRequest(requestMock, KNOWN_REQUEST_HEADER);

        // verify
        assertThat(remoteAddr).as("The remote address should be as the known client IP address")
                .isEqualTo(knownRemoteClientIP);
        verify(requestMock, times(1)).getHeader(KNOWN_REQUEST_HEADER);
        verify(requestMock, times(1)).getRemoteAddr();
    }

    @Test
    @Description("Tests create uri from request with masked IP when IP tracking is disabled")
    public void maskRemoteAddrIfDisabled() {

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

    @Test
    @Description("Tests create uri from x forward header")
    public void getRemoteAddrFromXForwardedForHeader() {

        final URI knownRemoteClientIP = IpUtil.createHttpUri("10.99.99.1");
        when(requestMock.getHeader(X_FORWARDED_FOR)).thenReturn(knownRemoteClientIP.getHost());

        final URI remoteAddr = IpUtil.getClientIpFromRequest(requestMock, "X-Forwarded-For");

        assertThat(remoteAddr).as("The remote address should be as the known client IP address")
                .isEqualTo(knownRemoteClientIP);
        verify(requestMock, times(1)).getHeader(X_FORWARDED_FOR);
        verify(requestMock, times(0)).getRemoteAddr();
    }

    @Test
    @Description("Tests create http uri ipv4 and ipv6")
    public void testCreateHttpUri() {

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

    private void assertHttpUri(final String host, final URI httpUri) {
        assertThat(IpUtil.isHttpUri(httpUri)).as("The given URI has an http scheme").isTrue();
        assertThat(IpUtil.isAmqpUri(httpUri)).as("The given URI is not an AMQP scheme").isFalse();
        assertThat(host).as("The URI hosts matches the given host").isEqualTo(httpUri.getHost());
        assertThat(httpUri.getScheme()).as("The given URI scheme is http").isEqualTo("http");
    }

    @Test
    @Description("Tests create amqp uri ipv4 and ipv6")
    public void testCreateAmqpUri() {

        final String ipv4 = "10.99.99.1";
        URI amqpUri = IpUtil.createAmqpUri(ipv4, "path");
        assertAmqpUri(ipv4, amqpUri);

        final String host = "myhost";
        amqpUri = IpUtil.createAmqpUri(host, "path");
        assertAmqpUri(host, amqpUri);

        final String ipv6 = "0:0:0:0:0:0:0:1";
        amqpUri = IpUtil.createAmqpUri(ipv6, "path");
        assertAmqpUri("[" + ipv6 + "]", amqpUri);
    }

    private void assertAmqpUri(final String host, final URI amqpUri) {

        assertThat(IpUtil.isAmqpUri(amqpUri)).as("The given URI is an AMQP scheme").isTrue();
        assertThat(IpUtil.isHttpUri(amqpUri)).as("The given URI is not an HTTP scheme").isFalse();
        assertThat(amqpUri.getHost()).as("The given host matches the URI host").isEqualTo(host);
        assertThat(amqpUri.getScheme()).as("The given URI has an AMQP scheme").isEqualTo("amqp");
        assertThat(amqpUri.getRawPath()).as("The given URI has an AMQP path").isEqualTo("/path");
    }

    @Test
    @Description("Tests create invalid uri")
    public void testCreateInvalidUri() {

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
}
