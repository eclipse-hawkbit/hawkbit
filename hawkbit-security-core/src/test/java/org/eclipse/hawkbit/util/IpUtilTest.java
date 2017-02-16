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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties.Clients;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@RunWith(MockitoJUnitRunner.class)
@Features("Unit Tests - Security")
@Stories("IP Util Test")
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
        when(requestMock.getHeader(X_FORWARDED_FOR)).thenReturn(null);
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
        when(requestMock.getHeader(X_FORWARDED_FOR)).thenReturn(null);
        when(requestMock.getRemoteAddr()).thenReturn(knownRemoteClientIP.getHost());
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
        when(requestMock.getRemoteAddr()).thenReturn(null);

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
        assertTrue("The given URI has an http scheme", IpUtil.isHttpUri(httpUri));
        assertFalse("The given URI is not an AMQP scheme", IpUtil.isAmqpUri(httpUri));
        assertEquals("The URI hosts matches the given host", host, httpUri.getHost());
        assertEquals("The given URI scheme is http", "http", httpUri.getScheme());
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

        assertTrue("The given URI is an AMQP scheme", IpUtil.isAmqpUri(amqpUri));
        assertFalse("The given URI is not an HTTP scheme", IpUtil.isHttpUri(amqpUri));
        assertEquals("The given host matches the URI host", host, amqpUri.getHost());
        assertEquals("The given URI has an AMQP scheme", "amqp", amqpUri.getScheme());
        assertEquals("The given URI has an AMQP path", "/path", amqpUri.getRawPath());
    }

    @Test
    @Description("Tests create invalid uri")
    public void testCreateInvalidUri() {

        final String host = "10.99.99.1";
        final URI testUri = IpUtil.createUri("test", host);

        assertFalse("The given URI is not an AMQP address", IpUtil.isAmqpUri(testUri));
        assertFalse("The given URI is not an HTTP address", IpUtil.isHttpUri(testUri));
        assertEquals("The given host matches the URI host", host, testUri.getHost());

        try {
            IpUtil.createUri(":/", host);
            fail("Missing expected IllegalArgumentException due invalid URI");
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }
}
