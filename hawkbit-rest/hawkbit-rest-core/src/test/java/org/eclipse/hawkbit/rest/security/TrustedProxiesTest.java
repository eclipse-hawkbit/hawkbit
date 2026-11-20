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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Trusted proxies matcher - comma separated IPs / CIDR blocks
 */
class TrustedProxiesTest {

    /**
     * Verifies the CIDR blocks and single IPs, IPv4 and IPv6
     */
    @Test
    void cidrBlocksAndIps() {
        final Predicate<String> trusted = TrustedProxies.of("203.0.113.0/24, 2001:db8::/32, 192.0.2.1");
        assertThat(trusted).accepts("203.0.113.5", "2001:db8::5", "192.0.2.1").rejects("203.0.114.1", "192.0.2.2", "::1", null);
    }

    /**
     * Verifies that single IPs without '/' work (no CIDR block needed)
     */
    @Test
    void singleIpsWithoutSlash() {
        final Predicate<String> trusted = TrustedProxies.of("1.2.3.4, 5.6.7.8");
        assertThat(trusted).accepts("1.2.3.4", "5.6.7.8").rejects("1.2.3.5", "5.6.7.9", "11.2.3.4");
        assertThat(TrustedProxies.of("1.2.3.4")).accepts("1.2.3.4").rejects("1.2.3.40");
        assertThat(TrustedProxies.of("2001:db8::1")).accepts("2001:db8::1").rejects("2001:db8::2");
    }

    /**
     * Verifies the default trusted proxies - the private, loopback and link-local ranges
     */
    @Test
    void defaults() {
        assertThat(TrustedProxies.of(TrustedProxies.DEFAULT))
                .accepts("10.1.2.3", "192.168.1.1", "172.16.0.1", "172.31.255.255", "127.0.0.1", "169.254.1.1", "100.64.0.1", "::1",
                        "fe80::1", "fd00::1")
                .rejects("110.1.2.3", "172.32.0.1", "8.8.8.8", "2001:db8::1");
        // the address family is checked - the IPv4 blocks don't match IPv6 hops (with the same prefix bytes) and vice versa
        assertThat(TrustedProxies.of(TrustedProxies.DEFAULT)).rejects("a00::1", "7f00::1", "::ffff:10.0.0.1", "252.0.0.1", "254.128.0.1");
    }

    /**
     * Verifies that empty proxies trust nothing, and that empty entries are ignored
     */
    @Test
    void emptyTrustsNothing() {
        assertThat(TrustedProxies.of(null)).rejects("10.0.0.1", null);
        assertThat(TrustedProxies.of("")).rejects("10.0.0.1");
        assertThat(TrustedProxies.of(" ")).rejects("10.0.0.1");
        assertThat(TrustedProxies.of(" , 1.2.3.4, ")).accepts("1.2.3.4").rejects("10.0.0.1");
    }

    /**
     * Verifies that an invalid entry (including a regular expression - not supported anymore) fails
     */
    @Test
    void invalidEntryFails() {
        assertThatThrownBy(() -> TrustedProxies.of("10.0.0.0/33")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TrustedProxies.of("example.com/8")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TrustedProxies.of("1.2.3.4, example.com")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TrustedProxies.of("10\\.0\\.0\\.\\d+")).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Verifies that a not IP hop is never trusted (no DNS lookup)
     */
    @Test
    void notIpHopNotTrusted() {
        final Predicate<String> trusted = TrustedProxies.of("127.0.0.0/8, ::1/128, 0.0.0.0/0");
        assertThat(trusted).rejects("localhost", "example.com", "unknown", "", "1.2.3.4.5");
    }
}
