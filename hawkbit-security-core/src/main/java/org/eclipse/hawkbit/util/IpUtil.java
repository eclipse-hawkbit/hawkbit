/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.util;

import java.lang.annotation.Target;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.security.HawkbitSecurityProperties;

/**
 * A utility which determines the correct IP of a connected {@link Target}. E.g
 * from a {@link HttpServletRequest}.
 *
 */
public final class IpUtil {

    private static final String HIDDEN_IP = "***";
    private static final String SCHEME_SEPERATOR = "://";
    private static final String HTTP_SCHEME = "http";
    private static final String AMPQP_SCHEME = "amqp";
    private static final Pattern IPV4_ADDRESS_PATTERN = Pattern
            .compile("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})");

    private static final Pattern IPV6_ADDRESS_PATTERN = Pattern.compile("([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}");

    private IpUtil() {

    }

    /**
     * Retrieves the string based IP address from a given
     * {@link HttpServletRequest} by either the
     * {@link HttpHeaders#X_FORWARDED_FOR} or by the
     * {@link HttpServletRequest#getRemoteAddr()} methods.
     *
     * @param request
     *            the {@link HttpServletRequest} to determine the IP address
     *            where this request has been sent from
     * @param securityProperties
     *            hawkBit security properties.
     * @return the {@link URI} based IP address from the client which sent the
     *         request
     */
    public static URI getClientIpFromRequest(final HttpServletRequest request,
            final HawkbitSecurityProperties securityProperties) {

        return getClientIpFromRequest(request, securityProperties.getClients().getRemoteIpHeader(),
                securityProperties.getClients().isTrackRemoteIp());
    }

    /**
     * Retrieves the string based IP address from a given
     * {@link HttpServletRequest} by either the
     * {@link HttpHeaders#X_FORWARDED_FOR} or by the
     * {@link HttpServletRequest#getRemoteAddr()} methods.
     *
     * @param request
     *            the {@link HttpServletRequest} to determine the IP address
     *            where this request has been sent from
     * @param forwardHeader
     *            the header name containing the IP address e.g. forwarded by a
     *            proxy {@code x-forwarded-for}
     * @return the {@link URI} based IP address from the client which sent the
     *         request
     */
    public static URI getClientIpFromRequest(final HttpServletRequest request, final String forwardHeader) {
        return getClientIpFromRequest(request, forwardHeader, true);
    }

    private static URI getClientIpFromRequest(final HttpServletRequest request, final String forwardHeader,
            final boolean trackRemoteIp) {

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

        final Matcher matcherv4 = IPV4_ADDRESS_PATTERN.matcher(s);

        if (matcherv4.find()) {
            return matcherv4.group(0);
        }

        final Matcher matcherv6 = IPV6_ADDRESS_PATTERN.matcher(s);

        if (matcherv6.find()) {
            return matcherv6.group(0);
        }

        return null;
    }

    /**
     * Create a {@link URI} with scheme and host.
     *
     * @param scheme
     *            the scheme
     * @param host
     *            the host
     * @return the {@link URI}
     * @throws IllegalArgumentException
     *             If the given string not parsable
     */
    public static URI createUri(final String scheme, final String host) {
        final boolean isIpV6 = host.indexOf(':') >= 0 && host.charAt(0) != '[';
        if (isIpV6) {
            return URI.create(scheme + SCHEME_SEPERATOR + "[" + host + "]");
        }
        return URI.create(scheme + SCHEME_SEPERATOR + host);
    }

    /**
     * Create a {@link URI} with amqp scheme and host.
     *
     * @param host
     *            the host
     * @param exchange
     *            the exchange will store in the path
     * @return the {@link URI}
     * @throws IllegalArgumentException
     *             If the given string not parsable
     */
    public static URI createAmqpUri(final String host, final String exchange) {
        return createUri(AMPQP_SCHEME, host).resolve("/" + exchange);
    }

    /**
     * Create a {@link URI} with http scheme and host.
     *
     * @param host
     *            the host
     * @return the {@link URI}
     * @throws IllegalArgumentException
     *             If the given string not parsable
     */
    public static URI createHttpUri(final String host) {
        return createUri(HTTP_SCHEME, host);
    }

    /**
     * Check if scheme contains http and uri ist not <code>null</code>.
     *
     * @param uri
     *            the uri
     * @return true = is http host false = not
     */
    public static boolean isHttpUri(final URI uri) {
        return uri != null && HTTP_SCHEME.equals(uri.getScheme());
    }

    /**
     * Check if host scheme amqp and uri ist not <code>null</code>.
     *
     * @param uri
     *            the uri
     * @return true = is http host false = not
     */
    public static boolean isAmqpUri(final URI uri) {
        return uri != null && AMPQP_SCHEME.equals(uri.getScheme());
    }

    /**
     * Check if the IP address of that {@link URI} is known, i.e. not an AQMP
     * exchange in DMF case and not HIDDEN_IP in DDI case.
     *
     * @param uri
     *            the uri
     * @return <code>true</code> if IP address is actually known by the server
     */
    public static boolean isIpAddresKnown(final URI uri) {
        return uri != null && !(AMPQP_SCHEME.equals(uri.getScheme()) || HIDDEN_IP.equals(uri.getHost()));
    }

}
