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
import java.util.Map;

import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.utils.IpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Forwarded headers processing - client IP, scheme, host and port resolution
 */
class ForwardedResolverTest {

    private static final String XFF = "X-Forwarded-For";
    private static final String RP = "10.0.0.1"; // trusted reverse proxy (default trusted proxies)
    private static final String DEVICE = "198.51.100.1";
    private static final String P1 = "198.51.100.2";
    private static final String P2 = "203.0.113.7";
    private static final String CUSTOMER = "192.0.2.10"; // trusted proxy of the DEFAULT tenant only
    private static final String DDI_PATH = "/DEFAULT/controller/v1/device1";

    private final ForwardedResolver resolver = resolver(TrustedProxies.DEFAULT);

    /**
     * Verifies that without forwarded header the peer is the client IP
     */
    @Test
    void noHeader() {
        assertThat(resolve(resolver, RP, "/rest/v1/targets").clientAddress())
                .isEqualTo(new ClientAddress(RP, RP, List.of(), null));
    }

    /**
     * Verifies that a forwarded header sent by a not trusted peer is ignored (spoofing)
     */
    @Test
    void notTrustedPeerHeaderIgnored() {
        final ForwardedResolver.Result result = resolve(resolver, P2, "/rest/v1/targets", "6.6.6.6");
        assertThat(result.clientAddress()).isEqualTo(new ClientAddress(P2, P2, List.of("6.6.6.6"), null));
    }

    /**
     * Verifies that a client sent forwarded header left to a not trusted hop is ignored (spoofing through the trusted reverse proxy)
     */
    @Test
    void clientSentHopsLeftToNotTrustedHopIgnored() {
        // the client P2 sends "10.0.0.2" (a trusted IP) by itself, RP appends P2
        assertThat(resolve(resolver, RP, "/", "10.0.0.2, " + P2).clientAddress())
                .isEqualTo(new ClientAddress(RP, P2, List.of("10.0.0.2"), null));
    }

    /**
     * Verifies a multi hop chain: device -> P1 -> P2 -> RP -> hawkBit
     */
    @Test
    void multiHopChain() {
        assertThat(resolve(resolver, RP, "/", DEVICE + ", " + P1 + ", " + P2).clientAddress())
                .isEqualTo(new ClientAddress(RP, P2, List.of(DEVICE, P1), null));
    }

    /**
     * Verifies that if all hops are trusted the most left one is the client IP
     */
    @Test
    void allTrusted() {
        assertThat(resolve(resolver, "127.0.0.1", "/", "10.1.1.1, 192.168.1.1").clientAddress())
                .isEqualTo(new ClientAddress("127.0.0.1", "10.1.1.1", List.of(), null));
    }

    /**
     * Verifies that multiple header lines are a comma separated list, empty values are dropped
     */
    @Test
    void multipleHeaderLines() {
        assertThat(resolve(resolver, RP, "/", DEVICE + ", " + P2, " , 10.0.0.2,").clientAddress())
                .isEqualTo(new ClientAddress(RP, P2, List.of(DEVICE), null));
    }

    /**
     * Verifies the IPv6 handling - the hops are in the full (canonical) form
     */
    @Test
    void ipv6() {
        assertThat(resolve(resolver, "0:0:0:0:0:0:0:1", "/", "2001:db8::1, fd00::1").clientAddress())
                .isEqualTo(new ClientAddress("0:0:0:0:0:0:0:1", "2001:db8:0:0:0:0:0:1", List.of(), null));
        // an IPv6 hop doesn't match IPv4 blocks (0a00:: starts with the 10/8 bytes)
        assertThat(resolve(resolver, RP, "/", "a00::1").clientAddress().ip()).isEqualTo("a00:0:0:0:0:0:0:1");
    }

    /**
     * Verifies that the hop IPs are normalized to the canonical form ({@link java.net.InetAddress#getHostAddress()}) - with or without
     * port / brackets, compressed or expanded, upper or lower case
     */
    @Test
    void hopsAreCanonical() {
        assertThat(ForwardedResolver.ip("::ffff:1.2.3.4")).isEqualTo("1.2.3.4");
        assertThat(ForwardedResolver.ip("[::ffff:1.2.3.4]:5678")).isEqualTo("1.2.3.4");
        assertThat(ForwardedResolver.ip("::1")).isEqualTo("0:0:0:0:0:0:0:1");
        assertThat(ForwardedResolver.ip("[::1]:80")).isEqualTo("0:0:0:0:0:0:0:1");
        assertThat(ForwardedResolver.ip("1.2.3.4:5678")).isEqualTo("1.2.3.4");
        assertThat(ForwardedResolver.ip("unknown")).isNull();
        assertThat(ForwardedResolver.ip("[2001:db8::1")).isNull();
        assertThat(ForwardedResolver.ip("999.1.1.1")).isNull();

        // compressed and expanded IPv6 - the same client IP
        for (final String hop : List.of(
                "2001:db8::1", "2001:0db8:0000:0000:0000:0000:0000:0001", "2001:DB8:0:0:0:0:0:1", "[2001:db8::1]", "[2001:db8::1]:5678")) {
            assertThat(resolve(resolver, RP, "/", hop).clientAddress().ip()).as(hop).isEqualTo("2001:db8:0:0:0:0:0:1");
        }
        // non-canonical IPv4 literals (accepted by InetAddress.ofLiteral) - as dotted quad
        for (final String hop : List.of("198.051.100.001", "3325256705", "198.51.25601")) {
            assertThat(resolve(resolver, RP, "/", hop).clientAddress().ip()).as(hop).isEqualTo(DEVICE);
        }
        // a non-canonical form of a trusted IP is trusted too
        assertThat(resolve(resolver, RP, "/", DEVICE + ", 10.1").clientAddress()).isEqualTo(new ClientAddress(RP, DEVICE, List.of(), null));
        assertThat(resolve(resolver, RP, "/", DEVICE + ", 010.0.0.2").clientAddress())
                .isEqualTo(new ClientAddress(RP, DEVICE, List.of(), null));
    }

    /**
     * Verifies that an IPv4-mapped IPv6 hop (set by a dual-stack proxy) is handled as IPv4 - a mapped trusted proxy is trusted by the IPv4
     * ranges (the walk continues past it), a mapped client IP is resolved as IPv4
     */
    @Test
    void ipv4MappedHops() {
        // device -> dual-stack proxy (10.0.0.3, reported as mapped by the next hop) -> RP -> hawkBit
        assertThat(resolve(resolver, RP, "/", DEVICE + ", ::ffff:10.0.0.3").clientAddress())
                .isEqualTo(new ClientAddress(RP, DEVICE, List.of(), null));
        assertThat(resolve(resolver, RP, "/", DEVICE + ", [::ffff:10.0.0.3]:5678").clientAddress())
                .isEqualTo(new ClientAddress(RP, DEVICE, List.of(), null));
        assertThat(resolve(resolver("10.0.0.0/24"), RP, "/", DEVICE + ", ::FFFF:10.0.0.3").clientAddress().ip()).isEqualTo(DEVICE);
        // a mapped client IP - as IPv4, also in the forwarded for hops
        assertThat(resolve(resolver, RP, "/", "::ffff:1.2.3.4").clientAddress()).isEqualTo(new ClientAddress(RP, "1.2.3.4", List.of(), null));
        assertThat(resolve(resolver, RP, "/", "::ffff:" + DEVICE + ", ::ffff:" + P2).clientAddress())
                .isEqualTo(new ClientAddress(RP, P2, List.of(DEVICE), null));
        // a mapped hop of a per tenant trusted proxy - trusted for the tenant
        assertThat(resolve(tenantResolver(), RP, DDI_PATH, "::ffff:" + DEVICE + ", ::ffff:" + CUSTOMER).clientAddress())
                .isEqualTo(new ClientAddress(RP, DEVICE, List.of(), "DEFAULT"));
    }

    /**
     * Verifies that the connection peer hop is normalized as the forwarded ones - e.g. a mapped (dual-stack) peer is trusted by the IPv4
     * ranges
     */
    @Test
    void peerHopIsCanonical() {
        final MockHttpServletRequest request = request("::ffff:10.0.0.1", "/", XFF, DEVICE);
        final ClientAddress clientAddress = resolve(resolver, request).clientAddress();
        assertThat(clientAddress.ip()).isEqualTo(DEVICE);
        assertThat(clientAddress.forwardedFor()).isEmpty();
        // not an IP literal peer - kept as is
        assertThat(resolve(resolver, "peer-host", "/").clientAddress().ip()).isEqualTo("peer-host");
    }

    /**
     * Verifies that the stored (DDI) target address of a mapped client IP is the IPv4 one
     */
    @Test
    void targetAddressOfMappedClientIp() {
        final String ip = resolve(resolver, RP, TenantResolverFilter.DDI_PATH, "::ffff:1.2.3.4").clientAddress().ip();
        assertThat(IpUtil.createHttpUri(ip)).hasToString("http://1.2.3.4");
        assertThat(IpUtil.createHttpUri(ForwardedResolver.ip("::ffff:1.2.3.4"))).hasToString("http://1.2.3.4");
        assertThat(IpUtil.createHttpUri(ForwardedResolver.ip("2001:db8::1"))).hasToString("http://[2001:db8:0:0:0:0:0:1]");
    }

    /**
     * Verifies that the hops are IP literals - a port (and the IPv6 brackets) is removed, and a not IP hop ends the chain
     */
    @Test
    void hopsAreIpLiterals() {
        assertThat(resolve(resolver, RP, "/", P2 + ":5678").clientAddress().ip()).isEqualTo(P2);
        assertThat(resolve(resolver, RP, "/", "[2001:db8::1]:5678").clientAddress().ip()).isEqualTo("2001:db8:0:0:0:0:0:1");
        assertThat(resolve(resolver, RP, "/", "[2001:db8::1]").clientAddress().ip()).isEqualTo("2001:db8:0:0:0:0:0:1");
        // a not IP hop - the hops left to it are dropped, the client IP is the hop right to it
        assertThat(resolve(resolver, RP, "/", P1 + ", unknown, " + P2).clientAddress())
                .isEqualTo(new ClientAddress(RP, P2, List.of(), null));
        // a not IP hop next to trusted hops only - the most right trusted one
        assertThat(resolve(resolver, RP, "/", "unknown, 10.0.0.2").clientAddress().ip()).isEqualTo("10.0.0.2");
        assertThat(resolve(resolver, RP, "/", "a b").clientAddress().ip()).isEqualTo(RP);
    }

    /**
     * Verifies that configured trusted proxies (IPs / CIDR blocks) replace the default ones
     */
    @Test
    void configuredTrustedProxies() {
        final ForwardedResolver configured = resolver("10.0.0.0/24, " + RP);
        assertThat(resolve(configured, RP, "/", P2 + ", 10.0.0.9").clientAddress().ip()).isEqualTo(P2);
        // a private IP not configured - not trusted anymore
        assertThat(resolve(configured, RP, "/", P2 + ", 192.168.0.1").clientAddress().ip()).isEqualTo("192.168.0.1");
        // empty - nothing trusted
        assertThat(resolve(resolver(""), RP, "/", P2).clientAddress().ip()).isEqualTo(RP);
    }

    /**
     * Verifies that the client IP chain is read from the clients remote IP header
     */
    @Test
    void remoteIpHeaderFromClientsProperties() {
        final HawkbitSecurityProperties securityProperties = new HawkbitSecurityProperties();
        securityProperties.getClients().setRemoteIpHeader("X-Chain");
        final ForwardedResolver chainResolver = ForwardedResolver.of(securityProperties);
        assertThat(resolve(chainResolver, request(RP, "/", "X-Chain", DEVICE + ", " + P2, XFF, P1)).clientAddress())
                .isEqualTo(new ClientAddress(RP, P2, List.of(DEVICE), null));
    }

    /**
     * Verifies the protocol, host and port headers of a trusted peer
     */
    @Test
    void protocolHostPort() {
        assertThat(resolve(resolver, request(RP, "/", "X-Forwarded-Proto", "https", "X-Forwarded-Host", "example.com")))
                .extracting(ForwardedResolver.Result::scheme, ForwardedResolver.Result::host, ForwardedResolver.Result::port,
                        ForwardedResolver.Result::secure)
                .containsExactly("https", "example.com", 443, true);
        assertThat(resolve(resolver, request(RP, "/", "X-Forwarded-Proto", "http"))).extracting(
                        ForwardedResolver.Result::scheme, ForwardedResolver.Result::host, ForwardedResolver.Result::port)
                .containsExactly("http", null, 80);
        // port header wins
        assertThat(resolve(resolver, request(RP, "/", "X-Forwarded-Proto", "https", "X-Forwarded-Host", "example.com:9443",
                "X-Forwarded-Port", "8443")).port()).isEqualTo(8443);
        // host with port, IPv6 host
        assertThat(resolve(resolver, request(RP, "/", "X-Forwarded-Proto", "https", "X-Forwarded-Host", "example.com:9443")))
                .extracting(ForwardedResolver.Result::host, ForwardedResolver.Result::port).containsExactly("example.com", 9443);
        assertThat(resolve(resolver, request(RP, "/", "X-Forwarded-Host", "[2001:db8::1]:8080")))
                .extracting(ForwardedResolver.Result::scheme, ForwardedResolver.Result::host, ForwardedResolver.Result::port)
                .containsExactly(null, "[2001:db8::1]", 8080);
        // first of multiple values, invalid ignored
        assertThat(resolve(resolver, request(RP, "/", "X-Forwarded-Host", "a.example.com, b.example.com")).host()).isEqualTo("a.example.com");
        assertThat(resolve(resolver, request(RP, "/", "X-Forwarded-Host", "evil.com/path")).host()).isNull();
        assertThat(resolve(resolver, request(RP, "/", "X-Forwarded-Port", "99999")).port()).isEqualTo(-1);
        // secure only if all values are https
        assertThat(resolve(resolver, request(RP, "/", "X-Forwarded-Proto", "https, http")).scheme()).isEqualTo("http");
    }

    /**
     * Verifies that the protocol, host and port headers of a not trusted peer are ignored
     */
    @Test
    void notTrustedPeerProtocolHostPortIgnored() {
        assertThat(resolve(resolver, request(P2, "/", "X-Forwarded-Proto", "https", "X-Forwarded-Host", "example.com",
                "X-Forwarded-Port", "8443")))
                .extracting(ForwardedResolver.Result::scheme, ForwardedResolver.Result::host, ForwardedResolver.Result::port,
                        ForwardedResolver.Result::secure)
                .containsExactly(null, null, -1, null);
    }

    /**
     * Verifies that a tenant trusted proxy is trusted for the DDI requests of its tenant (tenant case-insensitive, with context path,
     * artifact download) - so the devices behind it are resolved by their own IPs (with the tenant set), also through the global trusted
     * proxies
     */
    @Test
    void tenantTrustedProxyForTenantDdiRequests() {
        final ForwardedResolver tenantResolver = tenantResolver();

        // device -> CUSTOMER -> RP -> hawkBit, the tenant as in the path
        assertThat(resolve(tenantResolver, RP, "/DEFAULT/controller/v1/device1", DEVICE + ", " + CUSTOMER).clientAddress())
                .isEqualTo(new ClientAddress(RP, DEVICE, List.of(), "DEFAULT"));
        assertThat(resolve(tenantResolver, RP, "/default/controller/v1/device1", DEVICE + ", " + CUSTOMER).clientAddress())
                .isEqualTo(new ClientAddress(RP, DEVICE, List.of(), "default"));
        assertThat(resolve(tenantResolver, RP, "/DEFAULT/controller/v1/device1/softwaremodules/1/artifacts/file.bin",
                DEVICE + ", " + CUSTOMER).clientAddress())
                .isEqualTo(new ClientAddress(RP, DEVICE, List.of(), "DEFAULT"));
        final MockHttpServletRequest withContextPath = request(RP, "/ctx/DEFAULT/controller/v1/device1", XFF, DEVICE + ", " + CUSTOMER);
        withContextPath.setContextPath("/ctx");
        assertThat(resolve(tenantResolver, withContextPath).clientAddress()).isEqualTo(new ClientAddress(RP, DEVICE, List.of(), "DEFAULT"));
        // globally trusted proxies right to the tenant proxy
        assertThat(resolve(tenantResolver, RP, DDI_PATH, DEVICE + ", " + CUSTOMER + ", 10.0.0.5").clientAddress())
                .isEqualTo(new ClientAddress(RP, DEVICE, List.of(), "DEFAULT"));
        // the tenant proxy as connection peer
        assertThat(resolve(tenantResolver, CUSTOMER, DDI_PATH, DEVICE).clientAddress())
                .isEqualTo(new ClientAddress(CUSTOMER, DEVICE, List.of(), "DEFAULT"));
        // hops sent by the device itself are not trusted
        assertThat(resolve(tenantResolver, RP, DDI_PATH, "6.6.6.6, " + DEVICE + ", " + CUSTOMER).clientAddress())
                .isEqualTo(new ClientAddress(RP, DEVICE, List.of("6.6.6.6"), "DEFAULT"));
    }

    /**
     * Verifies that left to a tenant trusted proxy only the tenant's trusted proxies are trusted - not the global ones (e.g. the private
     * ranges), since these hops are in the customer's network
     */
    @Test
    void onlyTenantTrustedProxiesLeftToTenantProxy() {
        final ForwardedResolver tenantResolver = tenantResolver();

        // a private (globally trusted by default) hop left to the tenant proxy is the client
        assertThat(resolve(tenantResolver, RP, DDI_PATH, "6.6.6.6, 192.168.1.20, " + CUSTOMER).clientAddress())
                .isEqualTo(new ClientAddress(RP, "192.168.1.20", List.of("6.6.6.6"), "DEFAULT"));
        assertThat(resolve(tenantResolver, RP, DDI_PATH, DEVICE + ", 10.0.0.5, " + CUSTOMER).clientAddress())
                .isEqualTo(new ClientAddress(RP, "10.0.0.5", List.of(DEVICE), "DEFAULT"));
        // the tenant proxy as connection peer - same
        assertThat(resolve(tenantResolver, CUSTOMER, DDI_PATH, "6.6.6.6, 192.168.1.20").clientAddress())
                .isEqualTo(new ClientAddress(CUSTOMER, "192.168.1.20", List.of("6.6.6.6"), "DEFAULT"));

        // a customer internal proxy listed for the tenant - trusted
        final ForwardedResolver internalResolver = resolver(TrustedProxies.DEFAULT, Map.of("Default", "192.0.2.0/28, 192.168.1.20"));
        assertThat(resolve(internalResolver, RP, DDI_PATH, DEVICE + ", 192.168.1.20, " + CUSTOMER).clientAddress())
                .isEqualTo(new ClientAddress(RP, DEVICE, List.of(), "DEFAULT"));
        assertThat(resolve(internalResolver, RP, DDI_PATH, "6.6.6.6, 192.168.1.21, 192.168.1.20, " + CUSTOMER).clientAddress())
                .isEqualTo(new ClientAddress(RP, "192.168.1.21", List.of("6.6.6.6"), "DEFAULT"));
        // listed for the tenant, but not right to a tenant proxy - not trusted, global walk only
        assertThat(resolve(internalResolver, RP, DDI_PATH, DEVICE + ", " + P2).clientAddress())
                .isEqualTo(new ClientAddress(RP, P2, List.of(DEVICE), null));
    }

    /**
     * Verifies that the tenant is not set if no tenant proxy is involved in the resolution
     */
    @Test
    void noTenantWithoutTenantProxy() {
        final ForwardedResolver tenantResolver = tenantResolver();

        assertThat(resolve(tenantResolver, RP, DDI_PATH).clientAddress()).isEqualTo(new ClientAddress(RP, RP, List.of(), null));
        assertThat(resolve(tenantResolver, RP, DDI_PATH, DEVICE).clientAddress()).isEqualTo(new ClientAddress(RP, DEVICE, List.of(), null));
        assertThat(resolve(tenantResolver, RP, DDI_PATH, DEVICE + ", " + P2).clientAddress())
                .isEqualTo(new ClientAddress(RP, P2, List.of(DEVICE), null));
        assertThat(resolve(tenantResolver, "127.0.0.1", DDI_PATH, "10.1.1.1, 192.168.1.1").clientAddress())
                .isEqualTo(new ClientAddress("127.0.0.1", "10.1.1.1", List.of(), null));
        // the tenant proxy is the client (nothing left to it)
        assertThat(resolve(tenantResolver, RP, DDI_PATH, CUSTOMER).clientAddress())
                .isEqualTo(new ClientAddress(RP, CUSTOMER, List.of(), null));
    }

    /**
     * Verifies that a tenant trusted proxy is not trusted for another tenant and for not DDI requests
     */
    @Test
    void tenantTrustedProxyNotTrustedForOtherTenantsAndApis() {
        final ForwardedResolver tenantResolver = tenantResolver();

        for (final String uri : List.of(
                "/OTHER/controller/v1/device1", "/rest/v1/targets", "/DEFAULT/rest/v1/targets", "/controller/v1/device1",
                "/DEFAULT/controllers/v1/device1")) {
            assertThat(resolve(tenantResolver, RP, uri, DEVICE + ", " + CUSTOMER).clientAddress())
                    .as(uri).isEqualTo(new ClientAddress(RP, CUSTOMER, List.of(DEVICE), null));
        }
        // without tenants - not trusted
        assertThat(resolve(resolver, RP, DDI_PATH, DEVICE + ", " + CUSTOMER).clientAddress().ip()).isEqualTo(CUSTOMER);
    }

    /**
     * Verifies that the protocol, host and port headers of a peer trusted only for the tenant are ignored - only globally trusted peers
     * apply them
     */
    @Test
    void tenantTrustedPeerProtocolHostPortIgnored() {
        final ForwardedResolver tenantResolver = tenantResolver();

        final ForwardedResolver.Result result = resolve(tenantResolver, request(CUSTOMER, DDI_PATH,
                XFF, DEVICE, "X-Forwarded-Proto", "https", "X-Forwarded-Host", "example.com", "X-Forwarded-Port", "8443"));
        assertThat(result.clientAddress().ip()).isEqualTo(DEVICE);
        assertThat(result).extracting(ForwardedResolver.Result::scheme, ForwardedResolver.Result::host, ForwardedResolver.Result::port)
                .containsExactly(null, null, -1);
        // through the globally trusted reverse proxy - applied
        assertThat(resolve(tenantResolver, request(RP, DDI_PATH,
                XFF, DEVICE + ", " + CUSTOMER, "X-Forwarded-Proto", "https", "X-Forwarded-Host", "example.com")))
                .extracting(ForwardedResolver.Result::scheme, ForwardedResolver.Result::host, ForwardedResolver.Result::port)
                .containsExactly("https", "example.com", 443);
    }

    private static ForwardedResolver resolver(final String trustedProxies) {
        return resolver(trustedProxies, Map.of());
    }

    // tenantTrustedProxies - per tenant trusted proxies
    private static ForwardedResolver resolver(final String trustedProxies, final Map<String, String> tenantTrustedProxies) {
        final HawkbitSecurityProperties securityProperties = new HawkbitSecurityProperties();
        securityProperties.getForwarded().setTrustedProxies(trustedProxies);
        securityProperties.getForwarded().getTenants().putAll(tenantTrustedProxies);
        return ForwardedResolver.of(securityProperties);
    }

    // the tenant (case-insensitive) trusts CUSTOMER, other tenants trust other proxies or none (empty)
    private static ForwardedResolver tenantResolver() {
        return resolver(TrustedProxies.DEFAULT, Map.of("Default", "192.0.2.0/28", "Other", "203.0.113.0/24", "Third", ""));
    }

    private static ForwardedResolver.Result resolve(final ForwardedResolver resolver, final String peer, final String uri,
            final String... forwardedFor) {
        final MockHttpServletRequest request = request(peer, uri);
        for (final String value : forwardedFor) {
            request.addHeader(XFF, value);
        }
        return resolver.resolve(peer, request);
    }

    private static ForwardedResolver.Result resolve(final ForwardedResolver resolver, final MockHttpServletRequest request) {
        return resolver.resolve(request.getRemoteAddr(), request);
    }

    private static MockHttpServletRequest request(final String peer, final String uri, final String... headers) {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr(peer);
        for (int i = 0; i < headers.length; i += 2) {
            request.addHeader(headers[i], headers[i + 1]);
        }
        return request;
    }
}
