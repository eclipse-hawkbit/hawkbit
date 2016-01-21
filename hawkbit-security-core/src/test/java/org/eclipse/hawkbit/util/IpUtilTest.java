/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.util;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.net.HttpHeaders;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@RunWith(MockitoJUnitRunner.class)
@Features("IpUtil Test")
@Stories("Tests the created uris")
public class IpUtilTest {

    @Mock
    private HttpServletRequest requestMock;

    @Test
    @Description("Tests create uri from request")
    public void getRemoteAddrFromRequestIfForwaredHeaderNotPresent() {
        // known values
        final URI knownRemoteClientIP = IpUtil.createHttpUri("127.0.0.1");
        // mock
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_FOR)).thenReturn(null);
        when(requestMock.getRemoteAddr()).thenReturn(knownRemoteClientIP.getHost());

        // test
        final URI remoteAddr = IpUtil.getClientIpFromRequest(requestMock, "bumlux");

        // verify
        assertThat(remoteAddr).isEqualTo(knownRemoteClientIP);
        verify(requestMock, times(1)).getHeader("bumlux");
        verify(requestMock, times(1)).getRemoteAddr();
    }

    @Test
    @Description("Tests create uri from x forward header")
    public void getRemoteAddrFromXForwardedForHeader() {
        // known values
        final URI knownRemoteClientIP = IpUtil.createHttpUri("10.99.99.1");
        // mock
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_FOR)).thenReturn(knownRemoteClientIP.getHost());
        when(requestMock.getRemoteAddr()).thenReturn(null);

        // test
        final URI remoteAddr = IpUtil.getClientIpFromRequest(requestMock, "X-Forwarded-For");

        // verify
        assertThat(remoteAddr).isEqualTo(knownRemoteClientIP);
        verify(requestMock, times(1)).getHeader(HttpHeaders.X_FORWARDED_FOR);
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
        assertTrue(IpUtil.isHttpUri(httpUri));
        assertFalse(IpUtil.isAmqpUri(httpUri));
        assertEquals(host, httpUri.getHost());
        assertEquals("http", httpUri.getScheme());
    }

    @Test
    @Description("Tests create amqp uri ipv4 and ipv6")
    public void testCreateAmqpUri() {
        final String ipv4 = "10.99.99.1";
        URI amqpUri = IpUtil.createAmqpUri(ipv4);
        assertAmqpUri(ipv4, amqpUri);

        final String host = "myhost";
        amqpUri = IpUtil.createAmqpUri(host);
        assertAmqpUri(host, amqpUri);

        final String ipv6 = "0:0:0:0:0:0:0:1";
        amqpUri = IpUtil.createAmqpUri(ipv6);
        assertAmqpUri("[" + ipv6 + "]", amqpUri);
    }

    private void assertAmqpUri(final String host, final URI httpUri) {
        assertTrue(IpUtil.isAmqpUri(httpUri));
        assertFalse(IpUtil.isHttpUri(httpUri));
        assertEquals(host, httpUri.getHost());
        assertEquals("amqp", httpUri.getScheme());
    }

    @Test(expected = IllegalArgumentException.class)
    @Description("Tests create invalid uri")
    public void testCreateInvalidUri() {
        final String host = "10.99.99.1";
        final URI testUri = IpUtil.createUri("test", host);
        assertFalse(IpUtil.isAmqpUri(testUri));
        assertFalse(IpUtil.isHttpUri(testUri));
        assertEquals(host, testUri.getHost());
        IpUtil.createUri(":/", host);
        fail();

    }
}
