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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.springframework.util.ObjectUtils;

/**
 * Container independent forwarded headers processing - resolves the {@link ClientAddress} (and the original scheme, host and port) of a
 * request from its connection peer and forwarded headers. Applied by the container specific adapters ({@link ForwardedValve} for embedded
 * Tomcat, {@link ForwardedFilter} for the others).
 * <p/>
 * The client IP is resolved as the Tomcat RemoteIpValve does it: the hops (the forwarded header values + the connection peer) are walked from
 * right (hawkBit) to left, the trusted ones are skipped, the first not trusted one is the client IP. So, a client can't spoof its IP by
 * sending the forwarded header by itself - the hops left to a not trusted hop are ignored. If all are trusted - the most left one.
 * <p/>
 * Per tenant trusted proxies: for the DDI requests of a tenant (<code>/{tenant}/controller/**</code>) coming through a proxy trusted for
 * the tenant (e.g. a customer proxy of the tenant's devices) the walk continues with the tenant's trusted proxies only - so the devices are
 * resolved (and limited) by their own IPs. The other tenants and APIs don't trust the hops it adds.
 */
@Slf4j
public class ForwardedResolver {

    // host name, IPv4 or [IPv6], with an optional port - anything else (e.g. with a path, user info or comma) is ignored
    private static final Pattern HOST = Pattern.compile("^(\\[[0-9A-Fa-f:.]+(?:%[0-9A-Za-z._~-]+)?]|[0-9A-Za-z._-]+)(?::(\\d{1,5}))?$");

    private final Predicate<String> trustedProxies;
    private final Map<String, Predicate<String>> tenantTrustedProxies;
    private final String remoteIpHeader;
    private final String protocolHeader;
    private final String hostHeader;
    private final String portHeader;

    /**
     * @param trustedProxies the global trusted proxies
     * @param tenantTrustedProxies the per tenant (key - the tenant, case-insensitive) additionally trusted proxies for the DDI requests
     * @param remoteIpHeader the header with the client IP chain
     * @param protocolHeader the protocol header, <code>null</code> - not applied
     * @param hostHeader the host header, <code>null</code> - not applied
     * @param portHeader the port header, <code>null</code> - not applied
     */
    public ForwardedResolver(
            final Predicate<String> trustedProxies, final Map<String, Predicate<String>> tenantTrustedProxies,
            final String remoteIpHeader, final String protocolHeader, final String hostHeader, final String portHeader) {
        this.trustedProxies = trustedProxies;
        this.tenantTrustedProxies = new HashMap<>();
        tenantTrustedProxies.forEach((tenant, proxies) -> this.tenantTrustedProxies.put(tenant.toLowerCase(Locale.ROOT), proxies));
        this.remoteIpHeader = remoteIpHeader;
        this.protocolHeader = emptyToNull(protocolHeader);
        this.hostHeader = emptyToNull(hostHeader);
        this.portHeader = emptyToNull(portHeader);
    }

    /**
     * Creates the resolver from the hawkBit security properties.
     *
     * @param securityProperties the security properties - the forwarded ones and the remote IP header ({@link HawkbitSecurityProperties.Clients})
     * @return the resolver
     */
    public static ForwardedResolver of(final HawkbitSecurityProperties securityProperties) {
        final HawkbitSecurityProperties.Forwarded forwarded = securityProperties.getForwarded();
        final Map<String, Predicate<String>> tenantTrustedProxies = new HashMap<>();
        forwarded.getTenants().forEach((tenant, proxies) -> {
            if (!ObjectUtils.isEmpty(proxies)) {
                tenantTrustedProxies.put(tenant, TrustedProxies.of(proxies));
            }
        });
        return new ForwardedResolver(
                TrustedProxies.of(forwarded.getTrustedProxies() == null ? TrustedProxies.DEFAULT : forwarded.getTrustedProxies()),
                tenantTrustedProxies, securityProperties.getClients().getRemoteIpHeader(),
                forwarded.getProtocolHeader(), forwarded.getHostHeader(), forwarded.getPortHeader());
    }

    /**
     * Resolves the client address of a request.
     *
     * @param connectionPeer the connection peer IP
     * @param request the request - for the headers
     * @return the resolution result
     */
    public Result resolve(final String connectionPeer, final HttpServletRequest request) {
        final String peerIp = ip(connectionPeer);
        final String peer = peerIp == null ? connectionPeer : peerIp;
        final List<String> hops = hops(request, peer);

        int index = skipTrusted(hops, hops.size() - 1, trustedProxies);
        String resolvedForTenant = null;
        if (index > 0 && !tenantTrustedProxies.isEmpty()) {
            // DDI request through a proxy trusted for its tenant - continue the walk with the tenant's trusted proxies ONLY: the hops left to
            // it are in the customer's network, where the global ones (e.g. the private ranges) mean nothing. The tenant is from the (not yet
            // authenticated) path - fine, only requests coming through the tenant's proxies are affected
            final String tenant = TenantResolverFilter.tenant(request, TenantResolverFilter.DDI_PATH);
            final Predicate<String> tenantTrusted = tenant == null ? null : tenantTrustedProxies.get(tenant.toLowerCase(Locale.ROOT));
            if (tenantTrusted != null && tenantTrusted.test(hops.get(index))) {
                index = skipTrusted(hops, index, tenantTrusted);
                resolvedForTenant = tenant;
            }
        }
        final ClientAddress clientAddress = new ClientAddress(peer, hops.get(index), hops.subList(0, index), resolvedForTenant);

        // scheme / host / port - only if the connection peer is trusted, i.e. the headers are set by a trusted proxy
        if (!trustedProxies.test(peer)) {
            return new Result(clientAddress, null, null, -1);
        }
        String scheme = null;
        int port = -1;
        if (protocolHeader != null) {
            final List<String> protocols = values(request, protocolHeader);
            if (!protocols.isEmpty()) {
                // as RemoteIpValve - secure only if all (comma separated) values are https
                scheme = protocols.stream().allMatch("https"::equalsIgnoreCase) ? "https" : "http";
                port = "https".equals(scheme) ? 443 : 80;
            }
        }
        String host = null;
        if (hostHeader != null) {
            final List<String> hosts = values(request, hostHeader);
            if (!hosts.isEmpty()) {
                // the first one (the one the client used), if valid
                final Matcher matcher = HOST.matcher(hosts.getFirst());
                if (matcher.matches()) {
                    host = matcher.group(1);
                    // a port in the host header (host:port) is used if there is no port header (as Spring ForwardedHeaderFilter)
                    if (matcher.group(2) != null) {
                        port = port(matcher.group(2), port);
                    }
                } else {
                    log.debug("Invalid host header {} value: {}", hostHeader, hosts.getFirst());
                }
            }
        }
        if (portHeader != null) {
            final List<String> ports = values(request, portHeader);
            if (!ports.isEmpty()) {
                port = port(ports.getFirst(), port);
            }
        }
        return new Result(clientAddress, scheme, host, port);
    }

    // walks the hops to the left skipping the trusted ones - returns the index of the first not trusted one, 0 if all are trusted
    private static int skipTrusted(final List<String> hops, final int from, final Predicate<String> trusted) {
        int index = from;
        while (index > 0 && trusted.test(hops.get(index))) {
            index--;
        }
        return index;
    }

    // the forwarded header hops + the connection peer, as IP literals. A hop not being an IP literal ends the chain (the hops left to it are
    // dropped) - so the client IP is always an IP address
    private List<String> hops(final HttpServletRequest request, final String peer) {
        final List<String> hops = new ArrayList<>();
        for (final String value : values(request, remoteIpHeader)) {
            final String ip = ip(value);
            if (ip == null) {
                hops.clear();
            } else {
                hops.add(ip);
            }
        }
        hops.add(peer);
        return hops;
    }

    /**
     * Normalizes a hop to the canonical form of its IP address - without port (some proxies add it: IPv4:port, [IPv6]:port) and brackets,
     * an IPv4-mapped IPv6 address (e.g. <code>::ffff:1.2.3.4</code>, set by dual-stack proxies) as IPv4, IPv6 in the (full) form of
     * {@link InetAddress#getHostAddress()} (as the servlet container reports the peer). So an address has one form - for the trusted proxies
     * check, the DoS counters / white- and blacklist and the target address.
     *
     * @param hop the hop
     * @return the IP address, <code>null</code> if not an IP literal
     */
    public static String ip(final String hop) {
        String ip = hop;
        if (ip.startsWith("[")) {
            final int end = ip.indexOf(']');
            if (end < 0) {
                return null;
            }
            ip = ip.substring(1, end);
        } else if (ip.indexOf(':') > 0 && ip.indexOf(':') == ip.lastIndexOf(':')) {
            ip = ip.substring(0, ip.indexOf(':')); // IPv4:port, an IPv6 address has more than one colon
        }
        try {
            return InetAddress.ofLiteral(ip).getHostAddress();
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    // multiple header lines are a comma separated list too
    private static List<String> values(final HttpServletRequest request, final String header) {
        final List<String> values = new ArrayList<>();
        final Enumeration<String> headers = request.getHeaders(header);
        if (headers != null) {
            for (final String line : Collections.list(headers)) {
                for (final String value : line.split(",")) {
                    final String trimmed = value.trim();
                    if (!trimmed.isEmpty()) {
                        values.add(trimmed);
                    }
                }
            }
        }
        return values;
    }

    private static int port(final String value, final int defaultPort) {
        try {
            final int port = Integer.parseInt(value.trim());
            return port < 1 || port > 65535 ? defaultPort : port;
        } catch (final NumberFormatException e) {
            return defaultPort;
        }
    }

    private static String emptyToNull(final String value) {
        return ObjectUtils.isEmpty(value) ? null : value;
    }

    /**
     * Resolution result.
     *
     * @param clientAddress the client address
     * @param scheme the original scheme (<code>http</code> or <code>https</code>), <code>null</code> - not forwarded (unchanged)
     * @param host the original host (server name, without port), <code>null</code> - not forwarded (unchanged)
     * @param port the original port, <code>-1</code> - not forwarded (unchanged)
     */
    public record Result(ClientAddress clientAddress, String scheme, String host, int port) {

        /**
         * @return if the original scheme is https, <code>null</code> - not forwarded (unchanged)
         */
        public Boolean secure() {
            return scheme == null ? null : "https".equals(scheme);
        }
    }
}
