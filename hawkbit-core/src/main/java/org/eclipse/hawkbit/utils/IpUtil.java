/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.utils;

import java.lang.annotation.Target;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;

/**
 * A utility which determines the correct IP of a connected {@link Target}. E.g from a {@link HttpServletRequest}.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
// Exception squid:S2083 - false positive, file paths not handled here
@SuppressWarnings("squid:S2083")
public final class IpUtil {

    private static final String HIDDEN_IP = "***";
    private static final String SCHEME_SEPARATOR = "://";
    private static final String HTTP_SCHEME = "http";
    private static final String AMQP_SCHEME = "amqp";

    // v4 address with (optionally) port
    private static final Pattern IPV4_ADDRESS_PATTERN = Pattern
            .compile("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})(:[0-9]{1,5})?");
    private static final Pattern IPV6_ADDRESS_PATTERN = Pattern.compile("([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}");
    // v6 address with [] amd (optionally) port
    private static final Pattern IPV6_ADDRESS_WITH_PORT_PATTERN = Pattern.compile(
            "\\[(?<address>([0-9a-f]{1,4}:){7}([0-9a-f]){1,4})](:[0-9]{1,5})?");

    /**
     * Converts address to URI. If the address is not parsable, it will log and return <code>null</code>.
     * @param address the address to convert
     * @return the {@link URI} or <code>null</code> if the address is not parsable
     */
    public static URI addressToUri(final String address) {
        if (address == null) {
            return null;
        }
        try {
            return URI.create(address);
        } catch (final IllegalArgumentException e) {
            log.debug("Failed to parse URI: {}", address, e);
            return null;
        }
    }

    /**
     * Retrieves the string based IP address from a given
     * {@link HttpServletRequest} by either the configured {@link HawkbitSecurityProperties.Clients#getRemoteIpHeader()}
     * (by default X-Forwarded-For) or by the {@link HttpServletRequest#getRemoteAddr()} method.
     *
     * @param request the {@link HttpServletRequest} to determine the IP address where this request has been sent from
     * @param securityProperties hawkBit security properties.
     * @return the {@link URI} based IP address from the client which sent the request
     */
    public static URI getClientIpFromRequest(final HttpServletRequest request, final HawkbitSecurityProperties securityProperties) {
        return getClientIpFromRequest(
                request, securityProperties.getClients().getRemoteIpHeader(), securityProperties.getClients().isTrackRemoteIp());
    }

    /**
     * Retrieves the string based IP address from a given {@link HttpServletRequest} by either the
     * forward header or by the {@link HttpServletRequest#getRemoteAddr()} method.
     *
     * @param request the {@link HttpServletRequest} to determine the IP address
     *         where this request has been sent from
     * @param forwardHeader the header name containing the IP address e.g. forwarded by a
     *         proxy {@code x-forwarded-for}
     * @return the {@link URI} based IP address from the client which sent the
     *         request
     */
    public static URI getClientIpFromRequest(final HttpServletRequest request, final String forwardHeader) {
        return getClientIpFromRequest(request, forwardHeader, true);
    }

    /**
     * Create a {@link URI} with scheme and host.
     *
     * @param scheme the scheme
     * @param host the host
     * @return the {@link URI}
     * @throws IllegalArgumentException If the given string not parsable
     */
    public static URI createUri(final String scheme, final String host) {
        final boolean isIpV6 = host.indexOf(':') >= 0 && host.indexOf('.') == -1 && host.charAt(0) != '[';
        if (isIpV6) {
            return URI.create(scheme + SCHEME_SEPARATOR + "[" + host + "]");
        }
        return URI.create(scheme + SCHEME_SEPARATOR + host);
    }

    /**
     * Create a {@link URI} with amqp scheme and host.
     *
     * @param host the host
     * @param exchange the exchange will store in the path
     * @return the {@link URI}
     * @throws IllegalArgumentException If the given string not parse able
     */
    public static URI createAmqpUri(final String host, final String exchange) {
        return createUri(AMQP_SCHEME, host).resolve("/" + exchange);
    }

    /**
     * Create a {@link URI} with http scheme and host.
     *
     * @param host the host
     * @return the {@link URI}
     * @throws IllegalArgumentException If the given string not parsable
     */
    public static URI createHttpUri(final String host) {
        return createUri(HTTP_SCHEME, host);
    }

    /**
     * Check if scheme contains http and uri ist not <code>null</code>.
     *
     * @param uri the uri
     * @return true = is http host false = not
     */
    public static boolean isHttpUri(final URI uri) {
        return uri != null && HTTP_SCHEME.equals(uri.getScheme());
    }

    /**
     * Check if host scheme amqp and uri ist not <code>null</code>.
     *
     * @param uri the uri
     * @return true = is http host false = not
     */
    public static boolean isAmqpUri(final URI uri) {
        return uri != null && AMQP_SCHEME.equals(uri.getScheme());
    }

    /**
     * Check if the IP address of that {@link URI} is known, i.e. not an AQMP
     * exchange in DMF case and not HIDDEN_IP in DDI case.
     *
     * @param uri the uri
     * @return <code>true</code> if IP address is actually known by the server
     */
    public static boolean isIpAddresKnown(final URI uri) {
        return uri != null && !(AMQP_SCHEME.equals(uri.getScheme()) || HIDDEN_IP.equals(uri.getHost()));
    }

    private static URI getClientIpFromRequest(final HttpServletRequest request, final String forwardHeader, final boolean trackRemoteIp) {
        String ip;
        if (trackRemoteIp) {
            ip = request.getHeader(forwardHeader);
            if (ip == null || (ip = findClientIpAddress(ip)) == null) {
                ip = request.getRemoteAddr();
            }
        } else {
            ip = HIDDEN_IP;
        }

        return createHttpUri(ip);
    }

    private static String findClientIpAddress(final String s) {
        Matcher matcher = IPV4_ADDRESS_PATTERN.matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = IPV6_ADDRESS_PATTERN.matcher(s);
        if (matcher.find()) {
            return matcher.group(0);
        }

        matcher = IPV6_ADDRESS_WITH_PORT_PATTERN.matcher(s);
        if (matcher.find()) {
            return matcher.group("address");
        }

        return null;
    }

}
