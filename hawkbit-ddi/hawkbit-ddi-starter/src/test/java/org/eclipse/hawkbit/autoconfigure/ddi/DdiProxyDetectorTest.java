/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.ddi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.hawkbit.context.Principal;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: DDI proxy detection
 */
@ExtendWith(OutputCaptureExtension.class)
class DdiProxyDetectorTest {

    private static final LoggingSystem LOGGING_SYSTEM = LoggingSystem.get(DdiProxyDetectorTest.class.getClassLoader());

    private static final String IP = "203.0.113.7";
    private static final String NEW = "New DDI ";
    private static final String ACTIVE = "Active DDI ";

    private final TestClock clock = new TestClock(Instant.parse("2026-01-01T00:00:00Z"));

    // the detection logs on DEBUG
    @BeforeEach
    void enableDebugLog() {
        LOGGING_SYSTEM.setLogLevel(DdiProxyDetectionConfiguration.class.getName(), LogLevel.DEBUG);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        LOGGING_SYSTEM.setLogLevel(DdiProxyDetectionConfiguration.class.getName(), null);
    }

    @Test
    void belowReadThresholdNotDetected(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        send(detector, "GET", 39);
        assertThat(count(output, NEW)).isZero();
    }

    @Test
    void readThresholdReachedDetected(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        send(detector, "GET", 39);
        assertThat(count(output, NEW)).isZero();
        send(detector, "GET", 1);
        assertThat(count(output, NEW)).isOne();
        assertThat(output.getOut())
                .contains("New DDI shared IP (proxy not setting X-Forwarded-For, or NAT) " + IP)
                .contains("peak 40/0 read/write requests per second (threshold 40/10)")
                .contains("1 devices, 1 requests");
        // further requests of the same entry - not logged as new again
        send(detector, "GET", 10);
        assertThat(count(output, NEW)).isOne();
    }

    @Test
    void writeThresholdReachedDetected(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        send(detector, "POST", 9);
        assertThat(count(output, NEW)).isZero();
        send(detector, "PUT", 1);
        assertThat(count(output, NEW)).isOne();
        assertThat(output.getOut()).contains("peak 0/10 read/write requests per second (threshold 40/10)");
    }

    @Test
    void readAndWriteCountedSeparately(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        send(detector, "GET", 39);
        send(detector, "POST", 9);
        assertThat(count(output, NEW)).isZero();
    }

    @Test
    void thresholdPercentRespected(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.Properties properties = new DdiProxyDetectionConfiguration.Properties();
        properties.setThresholdPercent(50);
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(properties);
        authenticate("tenant");
        send(detector, "GET", 99);
        assertThat(count(output, NEW)).isZero();
        send(detector, "GET", 1);
        assertThat(count(output, NEW)).isOne();
        assertThat(output.getOut()).contains("(threshold 100/25)");
    }

    /**
     * Legacy mode (e.g. forward headers strategy NONE): no RemoteIpValve, so the remote address is the connection peer (the trusted reverse
     * proxy) - the client IP is resolved from the hops (with the default internal proxies) as the RemoteIpValve would do.
     */
    @Test
    void clientIpResolvedFromHopsNotFromRemoteAddress(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        sendWith(detector, 40, request -> {
            request.setRemoteAddr("10.0.0.2");
            hops(request, "device", "198.51.100.1", IP, "10.0.0.5", "10.0.0.2");
        });
        assertThat(count(output, NEW)).isOne();
        assertThat(output.getOut())
                .contains("New DDI proxy " + IP + ": claimed chains [[198.51.100.1]], trusted hops [10.0.0.5, 10.0.0.2]")
                .doesNotContain("DDI shared IP (proxy not setting X-Forwarded-For, or NAT) 10.0.0.2");
    }

    @Test
    void trustedProxyAsTomcat() {
        final TomcatServerProperties.Remoteip remoteIp = new TomcatServerProperties().getRemoteip();
        // default internal proxies - CIDR
        assertThat(DdiProxyDetectionConfiguration.trustedProxy(remoteIp))
                .accepts("10.1.2.3", "192.168.0.1", "127.0.0.1", "::1")
                .rejects(IP, "198.51.100.1");
        // trusted proxies - regex
        remoteIp.setTrustedProxies("203\\.0\\.113\\.7");
        assertThat(DdiProxyDetectionConfiguration.trustedProxy(remoteIp)).accepts("10.1.2.3", IP).rejects("198.51.100.1");
        // host names - not trusted, no DNS lookup
        remoteIp.setTrustedProxies(null);
        assertThat(DdiProxyDetectionConfiguration.trustedProxy(remoteIp)).rejects("localhost", "evil.example.com");
    }

    /**
     * The most common case - a single proxy setting the forwarded header for the devices connected to it directly: no claimed chain, but
     * forwarded - so a proxy, not a shared IP.
     */
    @Test
    void singleForwardingProxyIsProxy(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        sendWith(detector, 40, request -> hops(request, "device", IP, "10.0.0.2"));
        assertThat(output.getOut()).contains("New DDI proxy " + IP + ": claimed chains [[]], trusted hops [10.0.0.2]");
    }

    @Test
    void invalidPropertiesFailFast() {
        final DdiProxyDetectionConfiguration.Properties zeroPercent = new DdiProxyDetectionConfiguration.Properties();
        zeroPercent.setThresholdPercent(0);
        assertThatIllegalArgumentException().isThrownBy(() -> detector(zeroPercent)).withMessageContaining("threshold-percent");
        final DdiProxyDetectionConfiguration.Properties zeroInterval = new DdiProxyDetectionConfiguration.Properties();
        zeroInterval.setReportInterval(Duration.ZERO);
        assertThatIllegalArgumentException().isThrownBy(() -> detector(zeroInterval)).withMessageContaining("report-interval");
    }

    @Test
    void chainSplitAtRemoteAddress(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        sendWith(detector, 40, request -> hops(request, "device", "198.51.100.1", IP, "10.0.0.5", "10.0.0.2"));
        assertThat(count(output, NEW)).isOne();
        assertThat(output.getOut())
                .contains("New DDI proxy " + IP + ": claimed chains [[198.51.100.1]], trusted hops [10.0.0.5, 10.0.0.2]");
    }

    @Test
    void chainWithoutForwardedHeaderIsSharedIp(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        // no forwarded header - the hops are just the connection peer
        send(detector, "GET", 40);
        assertThat(output.getOut()).contains(
                "New DDI shared IP (proxy not setting X-Forwarded-For, or NAT) " + IP + ": claimed chains [[]], trusted hops []");
    }

    @Test
    void separateEntriesPerTenantNotPerChain(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant1");
        send(detector, "GET", 40);
        assertThat(count(output, NEW)).isOne();

        // proxy already detected - the first request of another tenant is a new entry
        authenticate("tenant2");
        send(detector, "GET", 1);
        assertThat(count(output, NEW)).isEqualTo(2);

        // another claimed chain - same entry (the claimed chains are not part of the key)
        sendWith(detector, 1, request -> hops(request, "device", "198.51.100.1", IP));
        sendWith(detector, 5, request -> hops(request, "device2", "198.51.100.1", IP));
        authenticate("tenant1");
        send(detector, "GET", 5);
        assertThat(count(output, NEW)).isEqualTo(2);

        detector.report();
        assertThat(output.getOut()).contains("Active DDI proxy " + IP + ": claimed chains [[]")
                .contains("DDI proxy detection report: 2 active (tenant, proxy) entries");
    }

    /**
     * The claimed chains are not verifiable - e.g. random forwarded headers sent by a device. They don't create entries and are bounded.
     */
    @Test
    void randomClaimedChainsBounded(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.Properties properties = new DdiProxyDetectionConfiguration.Properties();
        properties.setMaxClaimedChainsPerEntry(3);
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(properties);
        authenticate("tenant");
        for (int i = 0; i < 50; i++) {
            final String random = "198.51.100." + i;
            sendWith(detector, 1, request -> hops(request, "device", random, IP, "10.0.0.2"));
        }
        assertThat(count(output, NEW)).isOne();

        detector.report();
        assertThat(output.getOut()).containsPattern(
                        "Active DDI proxy " + Pattern.quote(IP) + ": claimed chains \\[[^]]*\\], \\[[^]]*\\], \\[[^]]*\\]\\]\\+,")
                .contains("DDI proxy detection report: 1 active (tenant, proxy) entries");
    }

    /**
     * Without the valve (e.g. MockMvc) the remote address is the only known hop.
     */
    @Test
    void remoteAddressWithoutValve(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        sendWith(detector, 40, request -> request.removeAttribute(DdiProxyDetectionConfiguration.DdiProxyDetector.PEER_ADDRESS_ATTRIBUTE));
        assertThat(output.getOut()).contains(
                "New DDI shared IP (proxy not setting X-Forwarded-For, or NAT) " + IP + ": claimed chains [[]], trusted hops []");
    }

    @Test
    void sameTenantDifferentTrustedHopsIsSameEntry(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        sendWith(detector, 40, request -> hops(request, "device", IP, "10.0.0.2"));
        sendWith(detector, 1, request -> hops(request, "device", IP, "10.0.0.3"));
        assertThat(count(output, NEW)).isOne();
    }

    @Test
    void noEntryWithoutTenant(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        send(detector, "GET", 50);
        assertThat(count(output, NEW)).isZero();
        detector.report();
        assertThat(output.getOut()).contains("DDI proxy detection report: 0 active");
    }

    @Test
    void reportLogsActiveAndDropsIdleEntries(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        send(detector, "GET", 40);
        assertThat(count(output, NEW)).isOne();

        clock.advance(Duration.ofMinutes(10));
        detector.report();
        assertThat(count(output, ACTIVE)).isOne();
        assertThat(output.getOut())
                .contains("Active DDI shared IP (proxy not setting X-Forwarded-For, or NAT) " + IP)
                .contains("DDI proxy detection report: 1 active (tenant, proxy) entries since 2026-01-01T00:00:00Z");

        // no requests since the last report - dropped
        clock.advance(Duration.ofMinutes(10));
        detector.report();
        assertThat(count(output, ACTIVE)).isOne();
        assertThat(output.getOut()).contains("DDI proxy detection report: 0 active (tenant, proxy) entries since 2026-01-01T00:10:00Z");

        // comes back - logged as new again (after being detected again)
        send(detector, "GET", 40);
        assertThat(count(output, NEW)).isEqualTo(2);
    }

    @Test
    void entriesDroppedIfMaxEntriesReached(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.Properties properties = new DdiProxyDetectionConfiguration.Properties();
        properties.setMaxEntries(1);
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(properties);
        authenticate("tenant1");
        send(detector, "GET", 40);
        authenticate("tenant2");
        send(detector, "GET", 3);
        assertThat(count(output, NEW)).isOne();

        detector.report();
        assertThat(output.getOut()).contains(
                "DDI proxy detection report: 1 active (tenant, proxy) entries since 2026-01-01T00:00:00Z, "
                        + "3 requests of new entries dropped (max entries 1 reached)");
    }

    @Test
    void devicesCountBounded(final CapturedOutput output) {
        final DdiProxyDetectionConfiguration.Properties properties = new DdiProxyDetectionConfiguration.Properties();
        properties.setMaxDevicesPerEntry(2);
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(properties);
        authenticate("tenant");
        send(detector, "GET", 40);
        for (int i = 0; i < 5; i++) {
            final int device = i;
            sendWith(detector, 1, request -> request.setRequestURI("/TENANT/controller/v1/device" + device));
        }
        detector.report();
        assertThat(output.getOut()).contains("2+ devices, 6 requests");
    }

    @Test
    void nextReportDelayInAndAfterStartupPeriod() {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        assertThat(detector.nextReportDelay()).isEqualTo(Duration.ofMinutes(5));
        clock.advance(Duration.ofMinutes(59));
        assertThat(detector.nextReportDelay()).isEqualTo(Duration.ofMinutes(5));
        clock.advance(Duration.ofMinutes(1));
        assertThat(detector.nextReportDelay()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void filterRecordsAndContinues(final CapturedOutput output) throws Exception {
        final DdiProxyDetectionConfiguration.DdiProxyDetector detector = detector(new DdiProxyDetectionConfiguration.Properties());
        authenticate("tenant");
        for (int i = 0; i < 40; i++) {
            final MockHttpServletRequest request = request();
            final MockFilterChain chain = new MockFilterChain();
            detector.filter().doFilter(request, new MockHttpServletResponse(), chain);
            assertThat(chain.getRequest()).isSameAs(request);
        }
        assertThat(count(output, NEW)).isOne();
    }

    private DdiProxyDetectionConfiguration.DdiProxyDetector detector(final DdiProxyDetectionConfiguration.Properties properties) {
        return new DdiProxyDetectionConfiguration.DdiProxyDetector(properties, new HawkbitSecurityProperties.Dos.Filter(),
                DdiProxyDetectionConfiguration.trustedProxy(new TomcatServerProperties().getRemoteip()), clock);
    }

    private static void authenticate(final String tenant) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new Principal(tenant, "controller"), null, List.of()));
    }

    private static MockHttpServletRequest request() {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/TENANT/controller/v1/dev1");
        request.setRemoteAddr(IP);
        // as set by the valve without forwarded header - just the connection peer
        hops(request, IP);
        return request;
    }

    // as set by the valve: the last hop is the connection peer, the others are the forwarded header
    private static void hops(final MockHttpServletRequest request, final String... hops) {
        if (hops.length > 1) {
            request.setAttribute(DdiProxyDetectionConfiguration.DdiProxyDetector.FORWARDED_FOR_ATTRIBUTE,
                    String.join(", ", List.of(hops).subList(0, hops.length - 1)));
        } else {
            request.removeAttribute(DdiProxyDetectionConfiguration.DdiProxyDetector.FORWARDED_FOR_ATTRIBUTE);
        }
        request.setAttribute(DdiProxyDetectionConfiguration.DdiProxyDetector.PEER_ADDRESS_ATTRIBUTE, hops[hops.length - 1]);
    }

    private static void send(final DdiProxyDetectionConfiguration.DdiProxyDetector detector, final String method, final int count) {
        sendWith(detector, count, request -> request.setMethod(method));
    }

    private static void sendWith(
            final DdiProxyDetectionConfiguration.DdiProxyDetector detector, final int count,
            final Consumer<MockHttpServletRequest> customizer) {
        for (int i = 0; i < count; i++) {
            final MockHttpServletRequest request = request();
            customizer.accept(request);
            detector.record(request);
        }
    }

    private static int count(final CapturedOutput output, final String text) {
        final Matcher matcher = Pattern.compile(Pattern.quote(text)).matcher(output.getOut());
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    // mutable clock - to test the time dependent behavior without waiting
    private static class TestClock extends Clock {

        private volatile Instant instant;

        private TestClock(final Instant instant) {
            this.instant = instant;
        }

        private void advance(final Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
