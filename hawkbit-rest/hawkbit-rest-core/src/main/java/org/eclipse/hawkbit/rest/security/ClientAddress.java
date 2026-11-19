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

import java.util.List;

import jakarta.servlet.ServletRequest;

/**
 * The client address of a request as resolved from the connection peer and the forwarded header - stored as request attribute
 * ({@link #ATTRIBUTE}) by the forwarded headers processing (Tomcat valve or servlet filter), or in legacy mode (forward headers strategy
 * not <code>NONE</code>) by a filter which just reads the client IP from the remote IP header as is.
 * <p/>
 * A hop is a network node the request passes on its way: device -> P1 -> P2 -> RP -> hawkBit. With RP trusted and P1, P2 not trusted, the
 * forwarded header is "device, P1, P2" and the connection peer is RP. Then: peer = RP, ip = P2, forwardedFor = [device, P1].
 *
 * @param peer the connection peer IP (e.g. the reverse proxy) - not changed by the forwarded headers processing
 * @param ip the resolved client IP - the first not trusted hop walking the hops from right (hawkBit) to left. Equals
 *         {@link ServletRequest#getRemoteAddr()} after the processing (in legacy mode - the client IP read from the remote IP header)
 * @param forwardedFor the hops left to the {@link #ip} (left to right) - as claimed by the {@link #ip} (a proxy), so not verifiable. Empty if
 *         the {@link #ip} didn't forward the request (or in legacy mode)
 * @param tenant if the {@link #ip} is resolved behind a proxy trusted for a tenant - the tenant (as in the DDI request path), otherwise
 *         <code>null</code>. Then the {@link #ip} is from the customer's network (e.g. a private one, overlapping with other tenants)
 */
public record ClientAddress(String peer, String ip, List<String> forwardedFor, String tenant) {

    /**
     * Request attribute holding the {@link ClientAddress}.
     */
    public static final String ATTRIBUTE = ClientAddress.class.getName();

    public ClientAddress {
        forwardedFor = List.copyOf(forwardedFor);
    }

    /**
     * @return the client address of the request - the stored one, or, if not processed (e.g. MockMvc without the processing), built from
     *         the remote address only
     */
    public static ClientAddress of(final ServletRequest request) {
        return request.getAttribute(ATTRIBUTE) instanceof ClientAddress clientAddress
                ? clientAddress
                : new ClientAddress(request.getRemoteAddr(), request.getRemoteAddr(), List.of(), null);
    }
}
