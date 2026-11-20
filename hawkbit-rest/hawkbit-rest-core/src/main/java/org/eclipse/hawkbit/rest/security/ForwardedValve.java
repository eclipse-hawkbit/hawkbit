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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;

import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.eclipse.hawkbit.rest.SecurityManagedConfiguration;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * Embedded Tomcat adapter of the forwarded headers processing ({@link ForwardedResolver}) - an engine valve, so the resolved client IP,
 * scheme, host and port are seen by everything after it (as with the Tomcat RemoteIpValve): the access log valve, the servlet filters
 * (incl. Spring Security) and the application. Stores the {@link ClientAddress} as request attribute.
 * <p/>
 * The connection peer is taken from {@link Request#getPeerAddr()} - never changed, so a repeated invocation for the same request (e.g. async
 * dispatch) resolves the same result. The forwarded headers are left unchanged. The resolved values are not restored after the invocation -
 * the access log is written after the valve pipeline returns and shall log them, and the request is recycled anyway.
 */
public class ForwardedValve extends ValveBase {

    private final ForwardedResolver resolver;

    public ForwardedValve(final ForwardedResolver resolver) {
        super(true);
        this.resolver = resolver;
    }

    @Override
    public void invoke(final Request request, final Response response) throws IOException, ServletException {
        final ForwardedResolver.Result result = resolver.resolve(request.getPeerAddr(), request);
        final ClientAddress clientAddress = result.clientAddress();
        request.setRemoteAddr(clientAddress.ip());
        // no DNS lookup - the host is the IP (as RemoteIpValve with disabled lookups)
        request.setRemoteHost(clientAddress.ip());
        if (result.scheme() != null) {
            request.setSecure(result.secure());
            request.getCoyoteRequest().scheme().setString(result.scheme());
        }
        if (result.host() != null) {
            request.getCoyoteRequest().serverName().setString(result.host());
        }
        if (result.port() > 0) {
            request.setServerPort(result.port());
        }
        request.setAttribute(ClientAddress.ATTRIBUTE, clientAddress);

        getNext().invoke(request, response);
    }

    /**
     * Adds the {@link ForwardedValve} as first engine valve, if the forward headers strategy is <code>NONE</code> (otherwise hawkBit doesn't
     * process the forwarded headers). First - so all other valves (e.g. the access log) see the resolved values. Hence, the lowest
     * precedence - to run after the Spring Boot customizers.
     */
    public static class Customizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>, Ordered {

        private final ForwardedResolver resolver;
        private final Environment environment;

        public Customizer(final ForwardedResolver resolver, final Environment environment) {
            this.resolver = resolver;
            this.environment = environment;
        }

        @Override
        public void customize(final TomcatServletWebServerFactory factory) {
            if (SecurityManagedConfiguration.isForwardedProcessing(environment)) {
                final List<Valve> valves = new ArrayList<>(factory.getEngineValves());
                valves.addFirst(new ForwardedValve(resolver));
                factory.setEngineValves(valves);
            }
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    }
}
