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

import java.net.URI;

import org.eclipse.hawkbit.utils.IpUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: IP Util Test
 */
class IpUtilTest {

    /**
     * Tests that the first IPv4 address in a remote IP header value is found - with or without port, as is
     */
    @Test
    void findsFirstIpv4Address() {
        assertThat(IpUtil.findClientIpAddress("10.99.99.1")).isEqualTo("10.99.99.1");
        assertThat(IpUtil.findClientIpAddress("127.0.0.1:93493")).isEqualTo("127.0.0.1");
        assertThat(IpUtil.findClientIpAddress("10.0.0.1, 10.0.0.2")).isEqualTo("10.0.0.1");
        // IPv4 has priority over (a preceding) IPv6
        assertThat(IpUtil.findClientIpAddress("0:0:0:0:0:0:0:1, 10.0.0.2")).isEqualTo("10.0.0.2");
        // IPv4-mapped IPv6 - the IPv4 part is found
        assertThat(IpUtil.findClientIpAddress("::ffff:1.2.3.4")).isEqualTo("1.2.3.4");
        // not normalized - as is
        assertThat(IpUtil.findClientIpAddress("010.0.0.1")).isEqualTo("010.0.0.1");
    }

    /**
     * Tests that the first IPv6 address in full form in a remote IP header value is found - without or in brackets (with optional port)
     */
    @Test
    void findsFirstFullIpv6Address() {
        assertThat(IpUtil.findClientIpAddress("0:0:0:0:0:0:0:1")).isEqualTo("0:0:0:0:0:0:0:1");
        assertThat(IpUtil.findClientIpAddress("[0:0:0:0:0:0:0:1]")).isEqualTo("0:0:0:0:0:0:0:1");
        assertThat(IpUtil.findClientIpAddress("[0:0:0:0:0:0:0:1]:4233")).isEqualTo("0:0:0:0:0:0:0:1");
        assertThat(IpUtil.findClientIpAddress("2001:db8:0:0:0:0:0:1, 2001:db8:0:0:0:0:0:2")).isEqualTo("2001:db8:0:0:0:0:0:1");
    }

    /**
     * Tests that null is returned if there is no IPv4 or full form IPv6 address in the header value
     */
    @Test
    void findsNoIpAddress() {
        assertThat(IpUtil.findClientIpAddress("")).isNull();
        assertThat(IpUtil.findClientIpAddress("unknown")).isNull();
        assertThat(IpUtil.findClientIpAddress("myhost.my:4233")).isNull();
        // compressed IPv6 is not supported
        assertThat(IpUtil.findClientIpAddress("::1")).isNull();
        assertThat(IpUtil.findClientIpAddress("[2001:db8::1]:4233")).isNull();
    }

    /**
     * Tests create http uri from a host info (as the remote address could be)
     */
    @Test
    void testCreateClientHttpUri() {
        checkHttpUriHost("0:0:0:0:0:0:0:1", "[0:0:0:0:0:0:0:1]");
        checkHttpUriHost("127.0.0.1", "127.0.0.1");
        checkHttpUriHost("127.0.0.1:93493", "127.0.0.1");
        checkHttpUriHost("myhost", "myhost");
        checkHttpUriHost("myhost.my", "myhost.my");
        checkHttpUriHost("myhost.my:4233", "myhost.my");
        checkHttpUriHost("[0:0:0:0:0:0:0:1]", "[0:0:0:0:0:0:0:1]");
        checkHttpUriHost("[0:0:0:0:0:0:0:1]:4233", "[0:0:0:0:0:0:0:1]");
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

    private static void checkHttpUriHost(final String hostInfo, final String expectedHost) {
        assertThat(IpUtil.createHttpUri(hostInfo).getHost()).as("The URI host should be as the expected one").isEqualTo(expectedHost);
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
