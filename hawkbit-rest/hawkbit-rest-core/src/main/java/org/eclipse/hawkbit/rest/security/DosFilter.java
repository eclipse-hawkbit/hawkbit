/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.security;

import static org.eclipse.hawkbit.audit.SecurityLogger.LOGGER;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.RequestedTenant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

/**
 * Filter for protection against Denial-of-Service (DoS) attacks. It reduces the maximum number of request per seconds which can be separately
 * configured for read (GET) and write (PUT/POST/DELETE) requests. Runs before the authentication, so the requests are counted per client IP
 * ({@link ClientAddress#ip()}). Additionally:
 * <ul>
 *     <li>limits the failed authentications (HTTP 401) per client IP</li>
 *     <li>reports the load ({@link LoadReport}) - the client IPs reaching a percent of their limits, e.g. proxies of many devices which
 *     could be trusted (see {@link ForwardedResolver}), or NATs</li>
 * </ul>
 * The rejected (HTTP 429) requests get a <code>Retry-After</code> header - the counters use fixed one-second windows.
 */
@Slf4j
public class DosFilter extends OncePerRequestFilter {

    // the counters use fixed one-second windows - so the client could retry after (at most) a second
    static final String RETRY_AFTER_SECONDS = "1";
    // max client IPs per load report interval - bounds the memory, the others are just counted
    private static final int MAX_REPORTED = 1000;
    // max forwarded for hops reported (the ones nearest to the client IP)
    private static final int MAX_REPORTED_HOPS = 5;
    private static final long NOT_STARTED = Long.MIN_VALUE;

    private final AntPathMatcher antMatcher = new AntPathMatcher();
    private final Collection<String> includeAntPaths;
    private final Collection<String> excludeAntPaths;

    private final Pattern ipAddressBlacklist;

    // request counters per client IP - a counter is put once (by the first request) and only incremented after, so it expires one second
    // after the first request, i.e. the requests are counted in fixed one-second windows
    private final Cache<String, AtomicInteger> readCountCache = newWindowCache();
    private final Cache<String, AtomicInteger> writeCountCache = newWindowCache();
    // failed authentication counters per client IP - the window starts with the first failure
    private final Cache<String, AuthFailures> authFailuresCache = newWindowCache();

    private final Limits limits;
    private final int maxAuthFailures;
    private final int reportReadThreshold;
    private final int reportWriteThreshold;
    private final long reportIntervalNanos;
    // the clients (keys - IP, or tenant and IP) reaching the report threshold in the current report interval
    private final Map<String, Load> loads = new ConcurrentHashMap<>();
    private final AtomicLong notReported = new AtomicLong();
    // started with the first reported load
    private final AtomicLong reportStart = new AtomicLong(NOT_STARTED);
    // for the counter windows and the report interval - replaceable in tests
    Ticker ticker = Ticker.systemTicker();

    private final Pattern whitelist;

    /**
     * Filter constructor including configuration.
     *
     * @param includeAntPaths paths where filter should hit, <code>null</code> or empty - all
     * @param excludeAntPaths paths (of the included ones) where filter should not hit, <code>null</code> or empty - none
     * @param limits maximum number of allowed read / write requests per second per client IP
     * @param maxAuthFailures maximum number of failed authentications (HTTP 401) per second per client IP, 0 to disable
     * @param loadReport the load report settings, <code>null</code> - disabled
     * @param ipDosWhiteListPattern {@link Pattern} with white list of client IP addresses for DOS filter
     * @param ipBlackListPattern {@link Pattern} with black listed IP addresses
     */
    public DosFilter(
            final Collection<String> includeAntPaths, final Collection<String> excludeAntPaths, final Limits limits, final int maxAuthFailures,
            final LoadReport loadReport, final String ipDosWhiteListPattern, final String ipBlackListPattern) {
        this.includeAntPaths = includeAntPaths;
        this.excludeAntPaths = excludeAntPaths;
        this.limits = limits;
        this.maxAuthFailures = maxAuthFailures;
        final int reportPercent = loadReport == null ? 0 : loadReport.percent();
        reportReadThreshold = threshold(limits.maxRead(), reportPercent);
        reportWriteThreshold = threshold(limits.maxWrite(), reportPercent);
        reportIntervalNanos = loadReport == null ? 0 : loadReport.interval().toNanos();

        if (ipBlackListPattern != null && !ipBlackListPattern.isEmpty()) {
            ipAddressBlacklist = Pattern.compile(ipBlackListPattern);
        } else {
            ipAddressBlacklist = null;
        }

        if (ipDosWhiteListPattern != null && !ipDosWhiteListPattern.isEmpty()) {
            whitelist = Pattern.compile(ipDosWhiteListPattern);
        } else {
            whitelist = null;
        }
    }

    /**
     * Maximum number of read / write requests per second.
     */
    public record Limits(int maxRead, int maxWrite) {

        int max(final boolean write) {
            return write ? maxWrite : maxRead;
        }
    }

    /**
     * Load report settings: a client IP reaching the percent of its read or write limit (in a one-second window) is logged - the first time
     * in a report interval immediately, and at the end of the interval with its peak rates. Then all are reset - so they are logged as new
     * in the next interval. The end of the interval is reported with the next high load request after it.
     *
     * @param percent the percent of the limits, 0 - disabled
     * @param interval the report interval
     */
    public record LoadReport(int percent, Duration interval) {}

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        if (!shouldInclude(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final ClientAddress clientAddress = ClientAddress.of(request);
        final String ip = clientAddress.ip();

        if (checkIpFails(ip)) {
            handleMissingIpAddress(response);
            return;
        }
        if (!checkAgainstBlacklist(response, ip)) {
            return;
        }

        // an IP resolved behind a proxy of a tenant is in the customer's network - never whitelisted (e.g. a private IP), and counted per
        // tenant and IP (the customers' networks could overlap)
        final String key = clientAddress.tenant() == null ? ip : clientAddress.tenant().toLowerCase(Locale.ROOT) + "/" + ip;
        final boolean limited = clientAddress.tenant() != null || !isWhitelisted(ip);
        final boolean write = HttpMethod.valueOf(request.getMethod()) != HttpMethod.GET;
        if (limited && (!checkAuthFailures(response, key) || !handleRequest(request, response, key, clientAddress, write))) {
            return;
        }

        filterChain.doFilter(request, response);

        // Only 401 (authentication failed) is counted. 403 is an authorization failure of an authenticated client (e.g. a Management API
        // user without the needed permission) - a legitimate client must not be throttled because of it.
        // The status is reliable here: both, sendError(401) (e.g. basic authentication entry point) and setStatus(401) (e.g. DDI entry point),
        // set it on the response (the error page is rendered by the container after the filter chain returns). A committed response keeps
        // its status, so it doesn't matter if the response has already been committed.
        if (limited && maxAuthFailures > 0 && response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            authFailuresCache.get(key, _ -> new AuthFailures()).count().incrementAndGet();
        }
    }

    // reads the ticker field on each access - all time of the filter (windows and report interval) comes from one ticker
    private <V> Cache<String, V> newWindowCache() {
        return Caffeine.newBuilder().ticker(() -> ticker.read()).expireAfterWrite(1, TimeUnit.SECONDS).build();
    }

    private static boolean checkIpFails(final String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }

    private static void handleMissingIpAddress(final HttpServletResponse response) {
        log.error("Failed to get peer IP address");
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private static void tooManyRequests(final HttpServletResponse response) {
        response.setHeader(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    private boolean isWhitelisted(final String ip) {
        return whitelist != null && whitelist.matcher(ip).find();
    }

    private boolean shouldInclude(final HttpServletRequest request) {
        return (includeAntPaths == null || includeAntPaths.isEmpty() || matches(includeAntPaths, request))
                && (excludeAntPaths == null || excludeAntPaths.isEmpty() || !matches(excludeAntPaths, request));
    }

    private boolean matches(final Collection<String> antPaths, final HttpServletRequest request) {
        // matched against the decoded path within the application (as the servlet container and the handler mappings do) - the raw
        // request URI could be percent-encoded (e.g. /r%65st/**) and so would bypass the filter while still being served
        final String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        return antPaths.stream().anyMatch(pattern -> antMatcher.match(pattern, path));
    }

    /**
     * @return false if the given ip address is on the blacklist and further
     *         processing of the request if forbidden
     */
    private boolean checkAgainstBlacklist(final HttpServletResponse response, final String ip) {
        if (ipAddressBlacklist != null && ipAddressBlacklist.matcher(ip).find()) {
            LOGGER.info("[BLACKLIST] Blacklisted client ({}) tries to access the server!", ip);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        return true;
    }

    /**
     * @return false if the client IP has reached the maximum number of failed authentications in the current window - then the request is
     *         rejected without trying to authenticate it (brute force protection)
     */
    private boolean checkAuthFailures(final HttpServletResponse response, final String ip) {
        if (maxAuthFailures <= 0) {
            return true;
        }
        // getIfPresent - a window is started by a failure (on increment), not by every checked request
        final AuthFailures authFailures = authFailuresCache.getIfPresent(ip);
        if (authFailures == null || authFailures.count().get() < maxAuthFailures) {
            return true;
        }

        // logged once per window - by the first rejected request
        if (authFailures.logged().compareAndSet(false, true)) {
            LOGGER.info("[DOS] Registered brute force attack! Client {} is above configured failed authentication threshold ({})!",
                    ip, maxAuthFailures);
        }
        tooManyRequests(response);
        return false;
    }

    private boolean handleRequest(
            final HttpServletRequest request, final HttpServletResponse response, final String key, final ClientAddress clientAddress,
            final boolean write) {
        final int limit = limits.max(write);
        // atomically created by the first request of the window, so concurrent first requests are not lost
        final int count = (write ? writeCountCache : readCountCache).get(key, _ -> new AtomicInteger()).incrementAndGet();
        if (count >= (write ? reportWriteThreshold : reportReadThreshold) && reportIntervalNanos > 0) {
            reportLoad(request, key, clientAddress, write, count);
        }
        if (count <= limit) {
            return true;
        }

        // logged once per window - by the first rejected request
        if (count == limit + 1) {
            LOGGER.info("[DOS] Registered DOS attack! Client {} is above configured {} request threshold ({}) - tenant {}, "
                    + "forwarded for {}!", key, write ? "WRITE" : "READ", limit, tenant(request), forwardedFor(clientAddress));
        }
        tooManyRequests(response);
        return false;
    }

    private void reportLoad(
            final HttpServletRequest request, final String key, final ClientAddress clientAddress, final boolean write, final int count) {
        endReportInterval();
        // atomic add / update - so an entry can't be removed (by the end of the interval) between getting it and updating its peaks
        loads.compute(key, (k, load) -> {
            Load current = load;
            if (current == null) {
                if (loads.size() >= MAX_REPORTED) {
                    notReported.incrementAndGet();
                    return null;
                }
                current = new Load(tenant(request), forwardedFor(clientAddress));
                LOGGER.info("[DOS] High load! Client {} reached {} {} requests per second (limit {}) - tenant {}, forwarded for {}",
                        k, count, write ? "WRITE" : "READ", limits.max(write), current.tenant(), current.forwardedFor());
            }
            (write ? current.peakWrite() : current.peakRead()).accumulateAndGet(count, Math::max);
            return current;
        });
    }

    // reports the client IPs of the ended interval and removes them - so they are new (logged immediately) in the next interval
    private void endReportInterval() {
        final long now = ticker.read();
        final long start = reportStart.get();
        if (start == NOT_STARTED) {
            reportStart.compareAndSet(NOT_STARTED, now);
            return;
        }
        if (now - start < reportIntervalNanos || !reportStart.compareAndSet(start, now)) {
            return;
        }
        for (final String key : loads.keySet()) {
            final Load load = loads.remove(key);
            if (load != null) {
                LOGGER.info("[DOS] Load report: client {} peak {} READ / {} WRITE requests per second (limits {} / {}) - tenant {}, "
                                + "forwarded for {}", key, load.peakRead(), load.peakWrite(), limits.maxRead(), limits.maxWrite(), load.tenant(),
                        load.forwardedFor());
            }
        }
        final long notReportedCount = notReported.getAndSet(0);
        if (notReportedCount > 0) {
            LOGGER.info("[DOS] Load report: {} more high load requests of not reported clients (max {} clients reported)",
                    notReportedCount, MAX_REPORTED);
        }
    }

    // the tenant of a DDI request (from the path, not authenticated), null if not a DDI request or invalid
    private static String tenant(final HttpServletRequest request) {
        final String tenant = TenantResolverFilter.tenant(request, TenantResolverFilter.DDI_PATH);
        return RequestedTenant.isValidTenant(tenant) ? tenant : null;
    }

    // the hops nearest to the client IP - IP literals (see ForwardedResolver), so safe to log
    private static List<String> forwardedFor(final ClientAddress clientAddress) {
        final List<String> forwardedFor = clientAddress.forwardedFor();
        return forwardedFor.size() <= MAX_REPORTED_HOPS
                ? forwardedFor
                : forwardedFor.subList(forwardedFor.size() - MAX_REPORTED_HOPS, forwardedFor.size());
    }

    private static int threshold(final int max, final int percent) {
        return percent <= 0 ? Integer.MAX_VALUE : (int) Math.ceil(max * percent / 100.0);
    }

    private record Load(String tenant, List<String> forwardedFor, AtomicInteger peakRead, AtomicInteger peakWrite) {

        private Load(final String tenant, final List<String> forwardedFor) {
            this(tenant, forwardedFor, new AtomicInteger(), new AtomicInteger());
        }
    }

    private record AuthFailures(AtomicInteger count, AtomicBoolean logged) {

        private AuthFailures() {
            this(new AtomicInteger(), new AtomicBoolean());
        }
    }
}
