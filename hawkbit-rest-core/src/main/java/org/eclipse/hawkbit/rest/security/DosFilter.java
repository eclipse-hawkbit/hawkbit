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
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.utils.IpUtil;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter for protection against Denial-of-Service (DoS) attacks. It reduces the maximum number of request per seconds which can be separately
 * configured for read (GET) and write (PUT/POST/DELETE) requests.
 */
@Slf4j
public class DosFilter extends OncePerRequestFilter {

    private final AntPathMatcher antMatcher = new AntPathMatcher();
    private final Collection<String> includeAntPaths;

    private final Pattern ipAddressBlacklist;

    private final Cache<String, AtomicInteger> readCountCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS).build();

    private final Cache<String, AtomicInteger> writeCountCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS).build();

    private final int maxRead;
    private final int maxWrite;

    private final Pattern whitelist;

    private final String forwardHeader;

    /**
     * Filter constructor including configuration.
     *
     * @param includeAntPaths paths where filter should hit
     * @param maxRead Maximum number of allowed REST read/GET requests per second per client
     * @param maxWrite Maximum number of allowed REST write/(PUT/POST/etc.) requests per second per client
     * @param ipDosWhiteListPattern {@link Pattern} with with white list of peer IP addresses for DOS filter
     * @param ipBlackListPattern {@link Pattern} with black listed IP addresses
     * @param forwardHeader the header containing the forwarded IP address e.g. {@code x-forwarded-for}
     */
    public DosFilter(
            final Collection<String> includeAntPaths, final int maxRead, final int maxWrite,
            final String ipDosWhiteListPattern, final String ipBlackListPattern, final String forwardHeader) {
        this.includeAntPaths = includeAntPaths;
        this.maxRead = maxRead;
        this.maxWrite = maxWrite;
        this.forwardHeader = forwardHeader;

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

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        if (!shouldInclude(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean processChain;

        final String ip = IpUtil.getClientIpFromRequest(request, forwardHeader).getHost();

        if (checkIpFails(ip)) {
            processChain = handleMissingIpAddress(response);
        } else {
            processChain = checkAgainstBlacklist(response, ip);

            if (processChain && (whitelist == null || !whitelist.matcher(ip).find())) {
                // read request
                if (HttpMethod.valueOf(request.getMethod()) == HttpMethod.GET) {
                    processChain = handleReadRequest(response, ip);
                }
                // write request
                else {
                    processChain = handleWriteRequest(response, ip);
                }
            }
        }

        if (processChain) {
            filterChain.doFilter(request, response);
        }
    }

    private static boolean checkIpFails(final String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }

    private static boolean handleMissingIpAddress(final HttpServletResponse response) {
        log.error("Failed to get peer IP address");
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return false;
    }

    private boolean shouldInclude(final HttpServletRequest request) {
        if (includeAntPaths == null || includeAntPaths.isEmpty()) {
            return true;
        }

        return includeAntPaths.stream()
                .anyMatch(pattern -> antMatcher.match(request.getContextPath() + pattern, request.getRequestURI()));
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

    private boolean handleWriteRequest(final HttpServletResponse response, final String ip) {
        boolean processChain = true;
        final AtomicInteger count = writeCountCache.getIfPresent(ip);

        if (count == null) {
            writeCountCache.put(ip, new AtomicInteger());
        } else if (count.getAndIncrement() > maxWrite) {
            LOGGER.info("[DOS] Registered DOS attack! Client {} is above configured WRITE request threshold ({})!", ip, maxWrite);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            processChain = false;
        }

        return processChain;
    }

    private boolean handleReadRequest(final HttpServletResponse response, final String ip) {
        boolean processChain = true;
        final AtomicInteger count = readCountCache.getIfPresent(ip);

        if (count == null) {
            readCountCache.put(ip, new AtomicInteger());
        } else if (count.getAndIncrement() > maxRead) {
            LOGGER.info("[DOS] Registered DOS attack! Client {} is above configured READ request threshold ({})!", ip, maxRead);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            processChain = false;
        }

        return processChain;
    }
}