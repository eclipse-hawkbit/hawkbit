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

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Denial of Service protection filter
 */
@ExtendWith(OutputCaptureExtension.class)
class DosFilterTest {

    private static final String CLIENT_IP = "203.0.113.7";
    private static final String WHITELISTED_IP = "10.0.0.1";
    private static final String PROXY_IP = "198.51.100.1";
    private static final String DEVICE_IP = "192.0.2.1";
    // matches the test whitelist
    private static final String TENANT_DEVICE_IP = "10.0.0.5";
    private static final DosFilter.LoadReport LOAD_REPORT = new DosFilter.LoadReport(20, Duration.ofHours(1));
    private static final String DDI_PATH = "/DEFAULT/controller/v1/device1";
    private static final String DOWNLOAD_PATTERN = "/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/*";
    private static final String DOWNLOAD_PATH = DDI_PATH + "/softwaremodules/1/artifacts/file.bin";

    private final AtomicInteger chainCalls = new AtomicInteger();
    // fake ticker (nanos) for the load report interval
    private final AtomicLong ticker = new AtomicLong();

    /**
     * Verifies that the read / write requests above the limits are rejected with 429 and Retry-After
     */
    @Test
    void limitsReadAndWriteRequests() throws Exception {
        final DosFilter filter = filter(2, 1, 0, null);

        assertThat(perform(filter, get(CLIENT_IP)).getStatus()).isEqualTo(200);
        assertThat(perform(filter, get(CLIENT_IP)).getStatus()).isEqualTo(200);
        assertTooManyRequests(perform(filter, get(CLIENT_IP)));
        // write counted separately
        assertThat(perform(filter, request("POST", CLIENT_IP, DDI_PATH)).getStatus()).isEqualTo(200);
        assertTooManyRequests(perform(filter, request("POST", CLIENT_IP, DDI_PATH)));
        // other client IP - own counter
        assertThat(perform(filter, get("203.0.113.8")).getStatus()).isEqualTo(200);
        assertThat(chainCalls).hasValue(4);
    }

    /**
     * Verifies that whitelisted client IPs aren't limited, and blacklisted ones are forbidden
     */
    @Test
    void whitelistAndBlacklist() throws Exception {
        final DosFilter filter = filter(1, 1, 1, null);

        for (int i = 0; i < 10; i++) {
            assertThat(perform(filter, get(WHITELISTED_IP)).getStatus()).isEqualTo(200);
        }
        assertThat(perform(filter, get("192.168.0.1")).getStatus()).isEqualTo(403);
        assertThat(chainCalls).hasValue(10);
    }

    /**
     * Verifies that the failed authentications (401 by the chain - via sendError or setStatus) are limited per client IP: after the
     * maximum, the requests are rejected without calling the chain
     */
    @Test
    void limitsAuthFailures() throws Exception {
        final DosFilter filter = filter(100, 100, 2, null);

        assertThat(perform(filter, get(CLIENT_IP), response -> response.sendError(401)).getStatus()).isEqualTo(401);
        assertThat(perform(filter, get(CLIENT_IP), response -> response.setStatus(401)).getStatus()).isEqualTo(401);
        assertThat(chainCalls).hasValue(2);

        // even a request with (maybe) valid credentials is rejected without authentication
        assertTooManyRequests(perform(filter, get(CLIENT_IP)));
        assertTooManyRequests(perform(filter, request("POST", CLIENT_IP, DDI_PATH)));
        assertThat(chainCalls).hasValue(2);

        // other client IP - not affected
        assertThat(perform(filter, get("203.0.113.8")).getStatus()).isEqualTo(200);
    }

    /**
     * Verifies that 403 (authorization failure of an authenticated client) isn't counted as failed authentication
     */
    @Test
    void forbiddenIsNotCountedAsAuthFailure() throws Exception {
        final DosFilter filter = filter(100, 100, 1, null);

        for (int i = 0; i < 5; i++) {
            assertThat(perform(filter, get(CLIENT_IP), response -> response.sendError(403)).getStatus()).isEqualTo(403);
        }
        assertThat(chainCalls).hasValue(5);
    }

    /**
     * Verifies that the failed authentications aren't limited if disabled (0) or for whitelisted client IPs
     */
    @Test
    void authFailuresNotLimitedIfDisabledOrWhitelisted() throws Exception {
        final DosFilter disabled = filter(100, 100, 0, null);
        for (int i = 0; i < 5; i++) {
            assertThat(perform(disabled, get(CLIENT_IP), response -> response.sendError(401)).getStatus()).isEqualTo(401);
        }

        final DosFilter enabled = filter(100, 100, 1, null);
        for (int i = 0; i < 5; i++) {
            assertThat(perform(enabled, get(WHITELISTED_IP), response -> response.sendError(401)).getStatus()).isEqualTo(401);
        }
        assertThat(chainCalls).hasValue(10);
    }

    /**
     * Verifies that a client IP reaching the report percent of its limit is logged immediately - once per report interval (not for the
     * further requests, neither read nor write ones)
     */
    @Test
    void highLoadLoggedOnceImmediately(final CapturedOutput output) throws Exception {
        final DosFilter filter = filter(10, 10, 0, LOAD_REPORT);

        // 20% of 10 - the threshold is 2 requests per second
        perform(filter, get(CLIENT_IP));
        assertThat(output).doesNotContain("[DOS] High load!");
        perform(filter, get(CLIENT_IP));
        assertThat(output).contains("[DOS] High load! Client " + CLIENT_IP + " reached 2 READ requests per second (limit 10) - tenant DEFAULT, "
                + "forwarded for []");
        for (int i = 0; i < 3; i++) {
            perform(filter, get(CLIENT_IP));
            perform(filter, request("POST", CLIENT_IP, DDI_PATH));
        }
        // within the interval
        ticker.addAndGet(LOAD_REPORT.interval().toNanos() - 1);
        perform(filter, get(CLIENT_IP));
        assertThat(occurrences(output, "[DOS] High load! Client " + CLIENT_IP)).isEqualTo(1);
        assertThat(output).doesNotContain("[DOS] Load report");
        assertThat(chainCalls).hasValue(9);
    }

    /**
     * Verifies that with the first high load after the end of the report interval, the client IPs of the ended interval are logged with
     * their peak rates and removed - so they are logged as new (immediately) again
     */
    @Test
    void loadReportAtIntervalEnd(final CapturedOutput output) throws Exception {
        final DosFilter filter = filter(10, 10, 0, LOAD_REPORT);

        for (int i = 0; i < 3; i++) {
            perform(filter, get(CLIENT_IP));
        }
        for (int i = 0; i < 2; i++) {
            perform(filter, request("POST", "203.0.113.8", "/rest/v1/targets"));
        }
        perform(filter, request("POST", CLIENT_IP, DDI_PATH));
        perform(filter, request("POST", CLIENT_IP, DDI_PATH));
        assertThat(output).doesNotContain("[DOS] Load report");

        ticker.addAndGet(LOAD_REPORT.interval().toNanos());
        // a not high load request doesn't end the interval
        perform(filter, get("203.0.113.9"));
        assertThat(output).doesNotContain("[DOS] Load report");
        perform(filter, get(CLIENT_IP));
        assertThat(output)
                .contains("[DOS] Load report: client " + CLIENT_IP + " peak 3 READ / 2 WRITE requests per second (limits 10 / 10) - tenant "
                        + "DEFAULT, forwarded for []")
                .contains("[DOS] Load report: client 203.0.113.8 peak 0 READ / 2 WRITE requests per second (limits 10 / 10) - tenant "
                        + "null, forwarded for []")
                .contains("[DOS] High load! Client " + CLIENT_IP + " reached 4 READ requests per second");
        assertThat(occurrences(output, "[DOS] High load! Client " + CLIENT_IP)).isEqualTo(2);
        assertThat(occurrences(output, "[DOS] Load report: client")).isEqualTo(2);

        // the new interval started with the report - the reported clients are not reported again
        ticker.addAndGet(LOAD_REPORT.interval().toNanos() - 1);
        perform(filter, get(CLIENT_IP));
        assertThat(occurrences(output, "[DOS] Load report: client")).isEqualTo(2);
        ticker.addAndGet(1);
        perform(filter, get(CLIENT_IP));
        assertThat(output).contains("[DOS] Load report: client " + CLIENT_IP + " peak 5 READ / 0 WRITE requests per second");
        assertThat(occurrences(output, "[DOS] Load report: client")).isEqualTo(3);
        assertThat(occurrences(output, "[DOS] High load! Client " + CLIENT_IP)).isEqualTo(3);
    }

    /**
     * Verifies that at most 1000 client IPs are reported per interval - the high load requests of the others are just counted
     */
    @Test
    void loadReportMaxClients(final CapturedOutput output) throws Exception {
        final DosFilter filter = filter(10, 10, 0, LOAD_REPORT);

        for (int i = 0; i < 1001; i++) {
            final String ip = "203.0." + (i / 256) + "." + (i % 256);
            perform(filter, get(ip));
            perform(filter, get(ip));
        }
        // the not reported one again
        perform(filter, get("203.0.3.232"));
        assertThat(occurrences(output, "[DOS] High load! Client")).isEqualTo(1000);
        assertThat(output).doesNotContain("203.0.3.232");

        ticker.addAndGet(LOAD_REPORT.interval().toNanos());
        perform(filter, get(CLIENT_IP));
        perform(filter, get(CLIENT_IP));
        assertThat(occurrences(output, "[DOS] Load report: client")).isEqualTo(1000);
        assertThat(output).contains("[DOS] Load report: 2 more high load requests of not reported clients (max 1000 clients reported)")
                .contains("[DOS] High load! Client " + CLIENT_IP);
    }

    /**
     * Verifies that the load report is disabled with percent 0 or without load report settings, and that whitelisted client IPs aren't
     * reported
     */
    @Test
    void loadReportDisabledOrWhitelisted(final CapturedOutput output) throws Exception {
        for (final DosFilter filter : List.of(
                filter(10, 10, 0, null), filter(10, 10, 0, new DosFilter.LoadReport(0, Duration.ofHours(1))))) {
            for (int i = 0; i < 10; i++) {
                assertThat(perform(filter, get(CLIENT_IP)).getStatus()).isEqualTo(200);
            }
        }
        final DosFilter enabled = filter(10, 10, 0, LOAD_REPORT);
        for (int i = 0; i < 20; i++) {
            assertThat(perform(enabled, get(WHITELISTED_IP)).getStatus()).isEqualTo(200);
        }
        ticker.addAndGet(LOAD_REPORT.interval().toNanos());
        perform(enabled, get(WHITELISTED_IP));
        assertThat(output).doesNotContain("[DOS] High load!").doesNotContain("[DOS] Load report");
    }

    /**
     * Verifies that the tenant (from the DDI path) and the forwarded for hops (at most the 5 nearest to the client IP) are logged
     */
    @Test
    void loadReportTenantAndForwardedFor(final CapturedOutput output) throws Exception {
        final DosFilter filter = filter(10, 10, 0, LOAD_REPORT);

        for (int i = 0; i < 2; i++) {
            perform(filter, forwarded(request("GET", PROXY_IP, "/Tenant1/controller/v1/device1"), "192.0.2.1"));
            perform(filter, forwarded(request("GET", "198.51.100.2", "/ctx/Tenant2/controller/v1/device1"),
                    "192.0.2.1", "192.0.2.2", "192.0.2.3", "192.0.2.4", "192.0.2.5", "192.0.2.6"));
        }
        assertThat(output)
                .contains("[DOS] High load! Client " + PROXY_IP + " reached 2 READ requests per second (limit 10) - tenant Tenant1, "
                        + "forwarded for [192.0.2.1]")
                .contains("[DOS] High load! Client 198.51.100.2 reached 2 READ requests per second (limit 10) - tenant Tenant2, "
                        + "forwarded for [192.0.2.2, 192.0.2.3, 192.0.2.4, 192.0.2.5, 192.0.2.6]");

        ticker.addAndGet(LOAD_REPORT.interval().toNanos());
        perform(filter, forwarded(request("GET", PROXY_IP, "/Tenant1/controller/v1/device1"), "192.0.2.1"));
        assertThat(output).contains("[DOS] Load report: client " + PROXY_IP + " peak 2 READ / 0 WRITE requests per second (limits 10 / 10) "
                + "- tenant Tenant1, forwarded for [192.0.2.1]");
    }

    /**
     * Verifies that the rejected (429) request log contains the tenant and the forwarded for hops
     */
    @Test
    void tooManyRequestsLogContainsTenantAndForwardedFor(final CapturedOutput output) throws Exception {
        final DosFilter filter = filter(1, 1, 0, null);

        assertThat(perform(filter, forwarded(get(CLIENT_IP), DEVICE_IP)).getStatus()).isEqualTo(200);
        assertTooManyRequests(perform(filter, forwarded(get(CLIENT_IP), DEVICE_IP)));
        assertThat(output).contains("[DOS] Registered DOS attack! Client " + CLIENT_IP + " is above configured READ request threshold (1) - "
                + "tenant DEFAULT, forwarded for [" + DEVICE_IP + "]!");
    }

    /**
     * Verifies that the excluded paths (of the included ones) are neither limited nor counted
     */
    @Test
    void excludedPathsNotCounted() throws Exception {
        final DosFilter filter = new DosFilter(
                List.of("/{tenant}/controller/v1/**"), List.of(DOWNLOAD_PATTERN), new DosFilter.Limits(1, 1), 0, null, null, null);

        for (int i = 0; i < 5; i++) {
            assertThat(perform(filter, request("GET", CLIENT_IP, DOWNLOAD_PATH)).getStatus()).isEqualTo(200);
        }
        // not counted by the excluded requests - the first included one passes
        assertThat(perform(filter, get(CLIENT_IP)).getStatus()).isEqualTo(200);
        assertTooManyRequests(perform(filter, get(CLIENT_IP)));
        // with context path
        final MockHttpServletRequest withContextPath = request("GET", CLIENT_IP, "/ctx" + DOWNLOAD_PATH);
        withContextPath.setContextPath("/ctx");
        assertThat(perform(filter, withContextPath).getStatus()).isEqualTo(200);
    }

    /**
     * Verifies that a request without a client IP is rejected with 500
     */
    @Test
    void missingClientIp() throws Exception {
        final MockHttpServletRequest request = get(CLIENT_IP);
        request.setAttribute(ClientAddress.ATTRIBUTE, new ClientAddress(CLIENT_IP, "unknown", List.of(), null));
        assertThat(perform(filter(100, 100, 0, null), request).getStatus()).isEqualTo(500);
        assertThat(chainCalls).hasValue(0);
    }

    /**
     * Verifies that a client IP resolved behind a proxy of a tenant is limited even if whitelisted (e.g. a private IP of the customer's
     * network)
     */
    @Test
    void tenantResolvedWhitelistedIpIsLimited(final CapturedOutput output) throws Exception {
        final DosFilter filter = filter(1, 1, 0, null);

        assertThat(perform(filter, tenantResolved("TenantA", TENANT_DEVICE_IP)).getStatus()).isEqualTo(200);
        assertTooManyRequests(perform(filter, tenantResolved("TenantA", TENANT_DEVICE_IP)));
        assertThat(output).contains("[DOS] Registered DOS attack! Client tenanta/" + TENANT_DEVICE_IP
                + " is above configured READ request threshold (1) - tenant TenantA, forwarded for []!");
        // the same IP not resolved for a tenant - whitelisted
        for (int i = 0; i < 5; i++) {
            assertThat(perform(filter, get(TENANT_DEVICE_IP)).getStatus()).isEqualTo(200);
        }
        assertThat(chainCalls).hasValue(6);
    }

    /**
     * Verifies that the same client IP of different tenants is counted separately (the customers' networks could overlap), and that the
     * tenant is case-insensitive
     */
    @Test
    void tenantResolvedIpCountedPerTenant() throws Exception {
        final DosFilter filter = filter(2, 1, 0, null);

        assertThat(perform(filter, tenantResolved("TenantA", TENANT_DEVICE_IP)).getStatus()).isEqualTo(200);
        assertThat(perform(filter, tenantResolved("tenanta", TENANT_DEVICE_IP)).getStatus()).isEqualTo(200);
        assertTooManyRequests(perform(filter, tenantResolved("TENANTA", TENANT_DEVICE_IP)));
        // other tenant - own counter
        assertThat(perform(filter, tenantResolved("TenantB", TENANT_DEVICE_IP)).getStatus()).isEqualTo(200);
        assertThat(perform(filter, tenantResolved("TenantB", TENANT_DEVICE_IP)).getStatus()).isEqualTo(200);
        assertTooManyRequests(perform(filter, tenantResolved("TenantB", TENANT_DEVICE_IP)));
        // a not whitelisted IP - the tenant one doesn't share the counter with the not tenant one
        assertThat(perform(filter, get(CLIENT_IP)).getStatus()).isEqualTo(200);
        assertThat(perform(filter, get(CLIENT_IP)).getStatus()).isEqualTo(200);
        assertThat(perform(filter, tenantResolved("TenantA", CLIENT_IP)).getStatus()).isEqualTo(200);
        assertThat(chainCalls).hasValue(7);
    }

    /**
     * Verifies that the failed authentications of a client IP resolved behind a proxy of a tenant are limited per tenant and IP
     */
    @Test
    void tenantResolvedAuthFailuresPerTenant() throws Exception {
        final DosFilter filter = filter(100, 100, 1, null);

        assertThat(perform(filter, tenantResolved("TenantA", TENANT_DEVICE_IP), response -> response.sendError(401)).getStatus())
                .isEqualTo(401);
        assertTooManyRequests(perform(filter, tenantResolved("tenanta", TENANT_DEVICE_IP)));
        assertThat(chainCalls).hasValue(1);
        // other tenant, same IP - not affected
        assertThat(perform(filter, tenantResolved("TenantB", TENANT_DEVICE_IP)).getStatus()).isEqualTo(200);
        // not tenant resolved, same (whitelisted) IP - not affected
        assertThat(perform(filter, get(TENANT_DEVICE_IP)).getStatus()).isEqualTo(200);
        assertThat(chainCalls).hasValue(3);
    }

    /**
     * Verifies that the load report of a client IP resolved behind a proxy of a tenant is keyed by tenant and IP
     */
    @Test
    void tenantResolvedLoadReport(final CapturedOutput output) throws Exception {
        final DosFilter filter = filter(10, 10, 0, LOAD_REPORT);

        perform(filter, tenantResolved("TenantA", TENANT_DEVICE_IP));
        perform(filter, tenantResolved("TenantA", TENANT_DEVICE_IP));
        assertThat(output).contains("[DOS] High load! Client tenanta/" + TENANT_DEVICE_IP + " reached 2 READ requests per second (limit 10) "
                + "- tenant TenantA, forwarded for []");
        ticker.addAndGet(LOAD_REPORT.interval().toNanos());
        perform(filter, tenantResolved("TenantB", TENANT_DEVICE_IP));
        perform(filter, tenantResolved("TenantB", TENANT_DEVICE_IP));
        assertThat(output).contains("[DOS] Load report: client tenanta/" + TENANT_DEVICE_IP + " peak 2 READ / 0 WRITE requests per second")
                .contains("[DOS] High load! Client tenantb/" + TENANT_DEVICE_IP + " reached 2 READ requests per second");
    }

    /**
     * Verifies that the default whitelist matches the whole IP - the private ranges only, not IPs just containing them
     */
    @Test
    void defaultWhitelistMatchesWholeIp() throws Exception {
        final DosFilter filter = new DosFilter(null, null, new DosFilter.Limits(1, 1), 0, null,
                new HawkbitSecurityProperties.Dos.Filter().getWhitelist(), null);

        for (final String ip : List.of("10.1.2.3", "192.168.1.1", "172.16.0.1", "127.0.0.1", "169.254.1.1")) {
            for (int i = 0; i < 3; i++) {
                assertThat(perform(filter, get(ip)).getStatus()).as(ip).isEqualTo(200);
            }
        }
        for (final String ip : List.of("110.1.2.3", "210.10.1.1", "1192.168.1.1", "172.32.0.1")) {
            assertThat(perform(filter, get(ip)).getStatus()).as(ip).isEqualTo(200);
            assertTooManyRequests(perform(filter, get(ip)));
        }
    }

    /**
     * Verifies that the different forms of a client IP (resolved from the forwarded headers) are counted as one client - IPv4 and
     * IPv4-mapped IPv6 (behind a dual-stack proxy), compressed and expanded IPv6 - and that a mapped whitelisted IP is whitelisted
     */
    @Test
    void clientIpFormsCountedAsOneClient() throws Exception {
        final DosFilter filter = filter(2, 1, 0, null);
        final ForwardedFilter forwardedFilter = new ForwardedFilter(ForwardedResolver.of(new HawkbitSecurityProperties()));

        assertThat(performForwarded(forwardedFilter, filter, "::ffff:" + CLIENT_IP).getStatus()).isEqualTo(200);
        assertThat(performForwarded(forwardedFilter, filter, CLIENT_IP).getStatus()).isEqualTo(200);
        assertTooManyRequests(performForwarded(forwardedFilter, filter, "[::ffff:" + CLIENT_IP + "]:1234"));

        assertThat(performForwarded(forwardedFilter, filter, "2001:db8::1").getStatus()).isEqualTo(200);
        assertThat(performForwarded(forwardedFilter, filter, "2001:DB8:0:0:0:0:0:1").getStatus()).isEqualTo(200);
        assertTooManyRequests(performForwarded(forwardedFilter, filter, "[2001:0db8::0001]:1234"));

        for (int i = 0; i < 5; i++) {
            assertThat(performForwarded(forwardedFilter, filter, "::ffff:10.0.0.9").getStatus()).isEqualTo(200);
        }
    }

    // the request through the (default trusted) reverse proxy, the client IP resolved by the forwarded filter
    private MockHttpServletResponse performForwarded(
            final ForwardedFilter forwardedFilter, final DosFilter filter, final String forwardedFor) throws Exception {
        final MockHttpServletRequest request = get("10.0.0.2");
        request.addHeader(HawkbitSecurityProperties.Clients.X_FORWARDED_FOR, forwardedFor);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        forwardedFilter.doFilter(request, response, (req, res) -> filter.doFilter(req, res, (r, s) -> chainCalls.incrementAndGet()));
        return response;
    }

    private DosFilter filter(
            final int maxRead, final int maxWrite, final int maxAuthFailures, final DosFilter.LoadReport loadReport) {
        final DosFilter filter = new DosFilter(
                null, null, new DosFilter.Limits(maxRead, maxWrite), maxAuthFailures, loadReport, "^10\\.", "^192\\.168\\.");
        filter.ticker = ticker::get;
        return filter;
    }

    // the request as forwarded by the given hops (left to right) through the client IP (the remote address, a proxy)
    private static MockHttpServletRequest forwarded(final MockHttpServletRequest request, final String... forwardedFor) {
        if (request.getRequestURI().startsWith("/ctx/")) {
            request.setContextPath("/ctx");
        }
        request.setAttribute(ClientAddress.ATTRIBUTE, new ClientAddress("10.0.0.2", request.getRemoteAddr(), List.of(forwardedFor), null));
        return request;
    }

    // the client IP resolved behind a proxy trusted for the tenant (the tenant as in the DDI request path)
    private static MockHttpServletRequest tenantResolved(final String tenant, final String ip) {
        final MockHttpServletRequest request = request("GET", PROXY_IP, "/" + tenant + "/controller/v1/device1");
        request.setAttribute(ClientAddress.ATTRIBUTE, new ClientAddress("10.0.0.2", ip, List.of(), tenant));
        return request;
    }

    private static int occurrences(final CapturedOutput output, final String text) {
        return output.getAll().split(Pattern.quote(text), -1).length - 1;
    }

    private static MockHttpServletRequest get(final String remoteAddr) {
        return request("GET", remoteAddr, DDI_PATH);
    }

    private static MockHttpServletRequest request(final String method, final String remoteAddr, final String uri) {
        final MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private static void assertTooManyRequests(final MockHttpServletResponse response) {
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
    }

    private MockHttpServletResponse perform(final DosFilter filter, final MockHttpServletRequest request) throws Exception {
        return perform(filter, request, _ -> {});
    }

    private MockHttpServletResponse perform(final DosFilter filter, final MockHttpServletRequest request, final Chain chain) throws Exception {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = (req, res) -> {
            chainCalls.incrementAndGet();
            chain.handle((HttpServletResponse) res);
        };
        filter.doFilter(request, response, filterChain);
        return response;
    }

    @FunctionalInterface
    private interface Chain {

        void handle(HttpServletResponse response) throws IOException;
    }
}
