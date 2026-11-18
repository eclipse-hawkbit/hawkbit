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

import java.io.IOException;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.NetMaskSet;
import org.apache.catalina.valves.ValveBase;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.context.Mdc;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * DDI proxy detection - detects (not trusted) proxies the DDI devices come through and logs them per tenant and proxy chain (log only, no
 * metrics). Enabled if and only if the DEBUG log of this class (<code>org.eclipse.hawkbit.autoconfigure.ddi.DdiProxyDetectionConfiguration</code>)
 * is enabled on start. See {@link Properties} for the configuration.
 * <p/>
 * All devices behind a proxy which is not trusted share its IP, so they share its DoS filter limits
 * (<code>hawkbit.server.security.dos.filter.max-read</code> / <code>max-write</code> per second and client IP). The detection helps to find
 * such proxies:
 * <ul>
 *     <li>a client IP - resolved from the forwarded header as Tomcat (RemoteIpValve, forward headers strategy NATIVE) does, skipping the
 *     trusted proxies, i.e. the first not trusted hop - is considered as proxy if its DDI request rate reaches {@link Properties#getThresholdPercent()} of the DoS filter max
 *     read (<code>GET</code>) or max write requests per second (with the defaults: 40 read or 10 write requests per second). The requests are
 *     counted in fixed one-second windows, per node - as the DoS filter does</li>
 *     <li>then, for the authenticated requests (the tenant is known), entries per tenant and proxy IP are tracked, with a bounded sample
 *     of the claimed chains - the forwarded header hops left to the proxy IP, without the most left one (the device IP) - not verifiable.
 *     The trusted hops (right to the proxy IP, and the connection peer) are logged too. An IP without claimed chains is logged as shared
 *     IP (a proxy not setting the forwarded header, or a NAT)</li>
 *     <li>a new entry is logged immediately (counting starts with the detection). All active entries are logged every
 *     {@link Properties#getStartupReportInterval()} in the {@link Properties#getStartupPeriod()} after start, then every
 *     {@link Properties#getReportInterval()}. Entries without requests in a report interval are dropped (and logged as new if they come
 *     back)</li>
 *     <li>the DoS filter whitelist is not applied - so whitelisted (e.g. private) IPs could be logged too</li>
 * </ul>
 * If a detected proxy is a legitimate one (e.g. of a customer), it could be added to <code>server.tomcat.remoteip.internal-proxies</code> -
 * note that it is trusted for all tenants then. Afterwards, the next not trusted hop (in longer chains) is detected.
 * <p/>
 * The {@link ProxyChainValve} captures the forwarded header as received (before the RemoteIpValve removes the trusted hops) and the
 * connection peer - the full hop chain. The detection filter ({@link DdiProxyDetector#filter()}) is added to the DDI and the DDI download
 * security filter chains, after the authentication, by {@link ControllerSecurityConfiguration} and
 * {@link ControllerDownloadSecurityConfiguration} - as the DoS filter counts the DDI and the download requests.
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory")
@Conditional(DdiProxyDetectionConfiguration.DebugLogEnabledCondition.class)
@EnableConfigurationProperties(DdiProxyDetectionConfiguration.Properties.class)
public class DdiProxyDetectionConfiguration {

    static final String TOMCAT_REMOTE_IP_HEADER = "server.tomcat.remoteip.remote-ip-header";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Bean(initMethod = "start")
    DdiProxyDetector ddiProxyDetector(
            final Properties properties, final HawkbitSecurityProperties securityProperties, final Environment environment) {
        // bound from the environment - the TomcatServerProperties bean exists only with the Tomcat web server auto-configuration
        final TomcatServerProperties tomcatProperties = Binder.get(environment).bindOrCreate("server.tomcat", TomcatServerProperties.class);
        return new DdiProxyDetector(
                properties, securityProperties.getDos().getFilter(), trustedProxy(tomcatProperties.getRemoteip()), Clock.systemUTC());
    }

    // trusted (internal or trusted) proxy check - as the Tomcat RemoteIpValve does it, with the same (Spring Boot) properties
    static Predicate<String> trustedProxy(final TomcatServerProperties.Remoteip remoteIp) {
        return proxyMatcher(remoteIp.getInternalProxies()).or(proxyMatcher(remoteIp.getTrustedProxies()));
    }

    // as RemoteIpValve#setInternalProxies / setTrustedProxies: a comma separated list of CIDR blocks or a single regular expression
    private static Predicate<String> proxyMatcher(final String proxies) {
        if (ObjectUtils.isEmpty(proxies)) {
            return ip -> false;
        } else if (proxies.indexOf('/') > 0) {
            final NetMaskSet netMasks = NetMaskSet.parse(proxies);
            return ip -> {
                try {
                    // literal only - NetMaskSet#contains(String) would make a DNS lookup (blocking the request) for a host name (e.g. sent in
                    // the forwarded header)
                    return netMasks.contains(InetAddress.ofLiteral(ip));
                } catch (final IllegalArgumentException e) {
                    return false; // not an IP address - not trusted
                }
            };
        } else {
            final Pattern pattern = Pattern.compile(proxies);
            return ip -> pattern.matcher(ip).matches();
        }
    }

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> ddiProxyChainValveCustomizer(
            @Value("${" + TOMCAT_REMOTE_IP_HEADER + ":" + X_FORWARDED_FOR + "}") final String forwardedForHeader) {
        return new ProxyChainValveCustomizer(forwardedForHeader);
    }

    /**
     * DDI proxy detection properties.
     */
    @Data
    @ConfigurationProperties(Properties.PREFIX)
    public static class Properties {

        static final String PREFIX = "hawkbit.server.ddi.proxy-detection";

        /**
         * Percent of the DoS filter max read / write requests per second which, if reached by a client IP, marks it as proxy.
         */
        private int thresholdPercent = 20;
        /**
         * Period after start in which the detected proxies are reported with {@link #startupReportInterval}.
         */
        private Duration startupPeriod = Duration.ofHours(1);
        /**
         * Report interval in the {@link #startupPeriod}.
         */
        private Duration startupReportInterval = Duration.ofMinutes(5);
        /**
         * Report interval after the {@link #startupPeriod}.
         */
        private Duration reportInterval = Duration.ofHours(1);
        /**
         * Max number of tracked (tenant, proxy) entries. If reached, new ones are dropped (and the drop count is reported).
         */
        private int maxEntries = 1000;
        /**
         * Max number of distinct devices counted per entry.
         */
        private int maxDevicesPerEntry = 30;
        /**
         * Max number of distinct claimed chains kept per entry. Bounded since the claimed chains are not verifiable (e.g. could be random
         * forwarded headers sent by a device).
         */
        private int maxClaimedChainsPerEntry = 10;
    }

    /**
     * Adds the {@link ProxyChainValve} as first engine valve - so before the RemoteIpValve (added by Spring Boot customizer) removes the
     * trusted hops from the forwarded header. Hence, the lowest precedence - to run after the Spring Boot customizers.
     */
    static class ProxyChainValveCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>, Ordered {

        private final String forwardedForHeader;

        ProxyChainValveCustomizer(final String forwardedForHeader) {
            this.forwardedForHeader = forwardedForHeader;
        }

        @Override
        public void customize(final TomcatServletWebServerFactory factory) {
            final List<Valve> valves = new ArrayList<>(factory.getEngineValves());
            valves.addFirst(new ProxyChainValve(forwardedForHeader));
            factory.setEngineValves(valves);
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    }

    /**
     * Stores the forwarded header as received and the connection peer IP address as request attributes. Not parsed here - the valve runs
     * for all requests, the hops are needed only for the DDI ones.
     */
    static class ProxyChainValve extends ValveBase {

        private final String forwardedForHeader;

        ProxyChainValve(final String forwardedForHeader) {
            super(true);
            this.forwardedForHeader = forwardedForHeader;
        }

        @Override
        public void invoke(final Request request, final Response response) throws IOException, ServletException {
            final List<String> forwardedFor = Collections.list(request.getHeaders(forwardedForHeader));
            if (!forwardedFor.isEmpty()) {
                // multiple header lines are a comma separated list too
                request.setAttribute(DdiProxyDetector.FORWARDED_FOR_ATTRIBUTE, String.join(",", forwardedFor));
            }
            request.setAttribute(DdiProxyDetector.PEER_ADDRESS_ATTRIBUTE, request.getPeerAddr());
            getNext().invoke(request, response);
        }
    }

    // the logging system is initialized (with the configured levels) before the configuration conditions are evaluated
    static class DebugLogEnabledCondition implements Condition {

        @Override
        public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            return log.isDebugEnabled();
        }
    }

    /**
     * DDI proxy detector - see {@link DdiProxyDetectionConfiguration}.
     */
    static class DdiProxyDetector implements AutoCloseable {

        /**
         * Request attribute with the forwarded header (comma separated hops) as received - before the RemoteIpValve removes the trusted hops.
         */
        static final String FORWARDED_FOR_ATTRIBUTE = "org.eclipse.hawkbit.ddi.proxyDetection.forwardedFor";
        /**
         * Request attribute with the IP address of the connection peer (e.g. the reverse proxy) - not changed by the RemoteIpValve.
         */
        static final String PEER_ADDRESS_ATTRIBUTE = "org.eclipse.hawkbit.ddi.proxyDetection.peerAddress";

        private final Properties properties;
        private final Predicate<String> trustedProxy;
        private final int readThreshold;
        private final int writeThreshold;
        private final Clock clock;
        private final Instant started;

        // same fixed 1-second windows as in the DoS filter
        private final Cache<String, AtomicInteger> readCountCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS).build();
        private final Cache<String, AtomicInteger> writeCountCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS).build();

        // A hop is a network node the request passes on its way: device -> P1 -> P2 -> RP -> hawkBit (the device is the first, hawkBit the
        // last one). Here P1 and P2 are not trusted (e.g. customer) proxies and RP is the trusted reverse proxy (e.g. private IP) in front
        // of hawkBit - this example is used in the comments below. Each proxy appends the IP it got the request from to the
        // X-Forwarded-For, so hawkBit gets "device, P1, P2" from the connection peer RP. hawkBit's own IP is not part of the request -
        // so the hops (forwarded header + connection peer) are device -> P1 -> P2 -> RP (-> hawkBit).

        // detected proxies: key - the proxy IP (the client IP resolved by Tomcat - the first not trusted hop, e.g. P2), value - its peak
        // request rates. An IP is put when its request rate reaches the threshold - only then its requests are tracked in entries
        private final Map<String, Proxy> proxies = new ConcurrentHashMap<>();
        // tracked (tenant, proxy) entries of the detected proxies: key - tenant and proxy IP, e.g. (TENANT, P2) - see EntryKey, value - the
        // request / device stats and a bounded sample of the claimed chains since the last report. The devices of the same tenant coming
        // through the same proxy share an entry, e.g. device1 -> P1 -> P2, device2 -> P1 -> P2 and device3 -> P2 (directly) are all
        // (TENANT, P2) with claimed chains [P1] and []. The claimed chains are not part of the key - they are not verifiable, so a device
        // sending random forwarded headers could flood the entries otherwise
        private final Map<EntryKey, Entry> entries = new ConcurrentHashMap<>();
        private final AtomicLong dropped = new AtomicLong();
        private volatile Instant lastReport;

        // the thread is created with the first scheduled report (start)
        private final ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("ddi-proxy-detection").daemon().factory());

        DdiProxyDetector(
                final Properties properties, final HawkbitSecurityProperties.Dos.Filter dosFilterProperties,
                final Predicate<String> trustedProxy, final Clock clock) {
            Assert.isTrue(properties.getThresholdPercent() > 0 && properties.getThresholdPercent() <= 100,
                    Properties.PREFIX + ".threshold-percent must be in (0, 100]");
            Assert.isTrue(properties.getStartupReportInterval().isPositive() && properties.getReportInterval().isPositive(),
                    Properties.PREFIX + ".startup-report-interval and report-interval must be positive");
            this.properties = properties;
            this.trustedProxy = trustedProxy;
            readThreshold = threshold(dosFilterProperties.getMaxRead(), properties.getThresholdPercent());
            writeThreshold = threshold(dosFilterProperties.getMaxWrite(), properties.getThresholdPercent());
            this.clock = clock;
            started = clock.instant();
            lastReport = started;
        }

        /**
         * Starts the periodic reporting.
         */
        void start() {
            log.debug("DDI proxy detection: proxy if >= {} read or >= {} write requests per second ({}% of the DoS filter limits)",
                    readThreshold, writeThreshold, properties.getThresholdPercent());
            scheduleReport();
        }

        @Override
        public void close() {
            scheduler.shutdownNow();
        }

        OncePerRequestFilter filter() {
            return new OncePerRequestFilter() {

                @Override
                protected void doFilterInternal(
                        final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
                        throws ServletException, IOException {
                    try {
                        record(request);
                    } catch (final Throwable t) {
                        log.error("Unexpected record error", t);
                    }
                    filterChain.doFilter(request, response);
                }
            };
        }

        void record(final HttpServletRequest request) {
            final Chain chain = chain(request);
            // the client IP as Tomcat (RemoteIpValve) resolves it: walking the hops back from hawkBit (device -> P1 -> P2 -> RP -> hawkBit,
            // from right to left), the trusted ones are skipped (RP), the first not trusted one is the client IP, e.g. P2. It is verified -
            // added by a trusted proxy (e.g. RP) which got a connection from it. Hence, it is the one the DoS filter limits (all devices
            // behind it share the limits) and that could be added as trusted proxy (e.g. server.tomcat.remoteip.trusted-proxies). The hops
            // left to it are added by it - not verifiable (claimed). If there is no not trusted proxy (e.g. device -> RP -> hawkBit), it is
            // the device IP.
            // Resolved here (not request.getRemoteAddr()) - so it works also in legacy mode (legacy remote IP resolution, or forward headers
            // strategy not NATIVE) - to find the proxies before switching to NATIVE. Note: in legacy mode the DoS filter limits another IP
            // (the most left one of hawkbit.server.security.clients.remote-ip-header) - this is the IP it will limit after the switch
            final String ip = chain.ip();
            final boolean write = HttpMethod.valueOf(request.getMethod()) != HttpMethod.GET;
            final int count = (write ? writeCountCache : readCountCache).get(ip, _ -> new AtomicInteger()).incrementAndGet();
            Proxy proxy = proxies.get(ip);
            if (proxy == null && count < (write ? writeThreshold : readThreshold)) {
                return; // not a proxy (yet)
            }

            // the tenant is known only for authenticated requests - not authenticated ones are rejected after this filter. Checked before
            // putting the proxy - so only IPs with authenticated requests (bounded by the max entries) are put
            final String tenant = AccessContext.tenant();
            if (tenant == null) {
                return;
            }
            if (proxy == null) {
                proxy = proxies.computeIfAbsent(ip, _ -> new Proxy());
            }
            (write ? proxy.peakWrite : proxy.peakRead).accumulateAndGet(count, Math::max);

            final EntryKey key = new EntryKey(tenant, ip);
            Entry entry = entries.get(key);
            boolean isNew = false;
            if (entry == null) {
                if (entries.size() >= properties.getMaxEntries()) {
                    dropped.incrementAndGet();
                    return;
                }
                final Entry newEntry = new Entry(chain.trusted(), clock.instant());
                entry = entries.putIfAbsent(key, newEntry);
                if (entry == null) {
                    entry = newEntry;
                    isNew = true;
                }
            }
            // a report could concurrently remove / reset the entry - then this request is lost (or the entry is logged as new again). Just
            // the log is affected, so accepted (instead of synchronization)
            entry.record(controllerId(request), chain, clock.instant(), properties);
            if (isNew) {
                log(key, entry, proxy, true);
            }
        }

        /**
         * Logs all active entries and drops the ones without requests since the last report.
         */
        void report() {
            final Instant now = clock.instant();
            final Instant since = lastReport;
            lastReport = now;

            int active = 0;
            for (final Map.Entry<EntryKey, Entry> e : entries.entrySet()) {
                final Entry entry = e.getValue();
                final Proxy proxy = proxies.get(e.getKey().ip());
                // the proxy could be gone if a request added the entry concurrently with the previous report - not detected anymore
                if (entry.requests.get() == 0 || proxy == null) {
                    entries.remove(e.getKey(), entry);
                } else {
                    active++;
                    log(e.getKey(), entry, proxy, false);
                    entry.reset();
                }
            }
            // keep only the proxies with active entries, the others have to be detected again
            final Set<String> activeIps = new HashSet<>();
            entries.keySet().forEach(key -> activeIps.add(key.ip()));
            proxies.keySet().retainAll(activeIps);
            proxies.values().forEach(Proxy::reset);

            final long droppedCount = dropped.getAndSet(0);
            log.debug("DDI proxy detection report: {} active (tenant, proxy) entries since {}{}", active, since,
                    droppedCount == 0
                            ? ""
                            : ", " + droppedCount + " requests of new entries dropped (max entries " + properties.getMaxEntries() + " reached)");
        }

        // report interval - more often in the startup period
        Duration nextReportDelay() {
            return Duration.between(started, clock.instant()).compareTo(properties.getStartupPeriod()) < 0
                    ? properties.getStartupReportInterval()
                    : properties.getReportInterval();
        }

        private void scheduleReport() {
            scheduler.schedule(() -> {
                try {
                    report();
                } catch (final Throwable t) {
                    log.error("Unexpected report error", t);
                } finally {
                    // otherwise the reporting (and the cleanup) stops
                    scheduleReport();
                }
            }, nextReportDelay().toMillis(), TimeUnit.MILLISECONDS);
        }

        private void log(final EntryKey key, final Entry entry, final Proxy proxy, final boolean isNew) {
            // the tenant is in the MDC
            Mdc.asTenantAsActorRe(key.tenant(), null, () -> {
                log.debug("{} DDI {} {}: claimed chains {}{}, trusted hops {}, peak {}/{} read/write requests per second (threshold {}/{}), "
                                + "{}{} devices, {} requests, first seen {}, last seen {}",
                        isNew ? "New" : "Active",
                        entry.forwarded ? "proxy" : "shared IP (proxy not setting X-Forwarded-For, or NAT)",
                        key.ip(), entry.claimed, entry.claimed.size() >= properties.getMaxClaimedChainsPerEntry() ? "+" : "", entry.trusted,
                        proxy.peakRead.get(), proxy.peakWrite.get(), readThreshold, writeThreshold,
                        entry.devices.size(), entry.devices.size() >= properties.getMaxDevicesPerEntry() ? "+" : "",
                        entry.requests.get(), entry.firstSeen, entry.lastSeen);
                return null;
            });
        }

        // resolves the client IP (the first not trusted hop from right - as the RemoteIpValve does) and splits the full hop chain at it,
        // e.g. for client IP P2:
        //   device -> P1 -> P2 -> RP (-> hawkBit): claimed [P1], trusted [RP]
        //   device -> P2 -> RP (-> hawkBit), device connects P2 directly: claimed [], trusted [RP], forwarded (P2 added the device)
        //   P2 -> RP (-> hawkBit), P2 doesn't set X-Forwarded-For (or NAT): claimed [], trusted [RP], not forwarded - logged as shared IP
        private Chain chain(final HttpServletRequest request) {
            final List<String> hops = new ArrayList<>();
            if (request.getAttribute(FORWARDED_FOR_ATTRIBUTE) instanceof String forwardedFor) {
                Arrays.stream(forwardedFor.split(",")).map(String::trim).filter(hop -> !hop.isEmpty()).forEach(hops::add);
            }
            // without the valve (e.g. MockMvc, no embedded Tomcat) - the remote address is the only known hop
            hops.add(request.getAttribute(PEER_ADDRESS_ATTRIBUTE) instanceof String peer ? peer : request.getRemoteAddr());
            int index = hops.size() - 1;
            // if all hops are trusted - the most left one (as the RemoteIpValve does)
            while (index > 0 && trustedProxy.test(hops.get(index))) {
                index--;
            }
            // the first (most left) hop is the device (or a client claimed) IP - not part of the proxy chain
            final List<String> claimed = index <= 1 ? List.of() : List.copyOf(hops.subList(1, index));
            return new Chain(hops.get(index), index > 0, claimed, List.copyOf(hops.subList(index + 1, hops.size())));
        }

        // DDI path: <context path>/{tenant}/controller/v1/{controllerId}/...
        private static String controllerId(final HttpServletRequest request) {
            final String[] segments = request.getRequestURI().substring(request.getContextPath().length()).split("/");
            return segments.length > 4 ? segments[4] : null;
        }

        private static int threshold(final int max, final int percent) {
            return (int) Math.ceil(max * percent / 100.0);
        }

        // forwarded - if there is a hop left to the client IP, i.e. the client IP set the forwarded header (so it is a proxy)
        private record Chain(String ip, boolean forwarded, List<String> claimed, List<String> trusted) {}

        /**
         * Key of a tracked entry.
         *
         * @param tenant the (authenticated) tenant of the request
         * @param ip the proxy IP - the client IP (the first not trusted hop), e.g. P2
         */
        private record EntryKey(String tenant, String ip) {}

        // peak request rates of a proxy in the current report interval
        private static class Proxy {

            private final AtomicInteger peakRead = new AtomicInteger();
            private final AtomicInteger peakWrite = new AtomicInteger();

            private void reset() {
                peakRead.set(0);
                peakWrite.set(0);
            }
        }

        private static class Entry {

            // the trusted hops of the first request - just informative, not part of the key
            private final List<String> trusted;
            private final Instant firstSeen;
            // sample of the claimed chains - the hops between the device (the first hop, excluded so that all devices behind the same proxies
            // share the chain) and the proxy IP, e.g. [P1] for device -> P1 -> P2 -> RP -> hawkBit. Added by the proxy (not trusted) - so not
            // verifiable. If the proxy is trusted (e.g. P2 added to the trusted proxies), the next hop (e.g. P1) becomes the proxy IP
            private final Set<List<String>> claimed = ConcurrentHashMap.newKeySet();
            private final Set<String> devices = ConcurrentHashMap.newKeySet();
            private final AtomicLong requests = new AtomicLong();
            // if any request was forwarded by the proxy IP (it set the forwarded header) - proxy, otherwise shared IP
            private volatile boolean forwarded;
            private volatile Instant lastSeen;

            private Entry(final List<String> trusted, final Instant firstSeen) {
                this.trusted = trusted;
                this.firstSeen = firstSeen;
                lastSeen = firstSeen;
            }

            private void record(final String controllerId, final Chain chain, final Instant now, final Properties properties) {
                requests.incrementAndGet();
                lastSeen = now;
                if (chain.forwarded()) {
                    forwarded = true;
                }
                // bounded - the counts are "at least" if reached
                if (claimed.size() < properties.getMaxClaimedChainsPerEntry()) {
                    claimed.add(chain.claimed());
                }
                if (controllerId != null && devices.size() < properties.getMaxDevicesPerEntry()) {
                    devices.add(controllerId);
                }
            }

            private void reset() {
                requests.set(0);
                forwarded = false;
                claimed.clear();
                devices.clear();
            }
        }
    }
}
