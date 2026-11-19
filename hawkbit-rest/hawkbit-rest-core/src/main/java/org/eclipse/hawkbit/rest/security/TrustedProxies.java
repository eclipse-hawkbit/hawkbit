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

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.util.ObjectUtils;

/**
 * Trusted proxies matcher - a comma separated list of IPs / CIDR blocks. Only IP literals are matched - no DNS lookup (would block the request,
 * and a host name could be sent by a client in the forwarded header), a not IP hop is never trusted.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TrustedProxies {

    /**
     * Default trusted proxies - the private, loopback, link-local and CGNAT ranges, IPv4 and IPv6. Same ranges as the Tomcat RemoteIpValve
     * <code>internalProxies</code> default.
     */
    public static final String DEFAULT =
            "10.0.0.0/8, 192.168.0.0/16, 169.254.0.0/16, 127.0.0.0/8, 100.64.0.0/10, 172.16.0.0/12, ::1/128, fe80::/10, fc00::/7";

    /**
     * @param proxies comma separated IPs / CIDR blocks, <code>null</code> or empty - nothing trusted
     * @return the matcher
     * @throws IllegalArgumentException if an IP / CIDR block is invalid
     */
    public static Predicate<String> of(final String proxies) {
        if (ObjectUtils.isEmpty(proxies)) {
            return ip -> false;
        }
        final List<Block> blocks = Arrays.stream(proxies.split(","))
                .map(String::trim)
                .filter(proxy -> !proxy.isEmpty())
                .map(proxy -> new Block(new IpAddressMatcher(proxy), proxy.indexOf(':') >= 0))
                .toList();
        return ip -> ip != null && blocks.stream().anyMatch(block -> block.matches(ip));
    }

    // the address family is checked explicitly - IpAddressMatcher compares the prefix bytes across the families (e.g. 10.0.0.0/8 would
    // match a00::1), so an IPv6 hop shall never match an IPv4 block and vice versa (an IPv4-mapped IPv6 address is IPv6 here)
    private record Block(IpAddressMatcher matcher, boolean ipv6) {

        private boolean matches(final String ip) {
            if ((ip.indexOf(':') >= 0) != ipv6) {
                return false;
            }
            try {
                return matcher.matches(ip);
            } catch (final IllegalArgumentException e) {
                return false; // not an IP address - not trusted
            }
        }
    }
}
