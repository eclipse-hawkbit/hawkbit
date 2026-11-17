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

import jakarta.servlet.ServletException;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.eclipse.hawkbit.security.controller.AuthenticationFilters;
import org.eclipse.hawkbit.security.controller.DdiSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Support of the deprecated <code>hawkbit.server.ddi.security.rp.trusted-ips</code> with embedded Tomcat. The trusted IPs are reverse proxy
 * IPs, but with <code>server.forward-headers-strategy=NATIVE</code> the Tomcat RemoteIpValve replaces the remote address with the client
 * (device) IP. So, a valve stores the connection peer IP address (the reverse proxy) as request attribute, which is checked instead.
 * <p/>
 * Created only if the trusted IPs are set. Since they are deprecated, they have to be enabled explicitly with
 * <code>hawkbit.server.ddi.security.rp.trusted-ips-enable=true</code>, otherwise the application fails to start.
 *
 * @deprecated since 1.2.1 - see {@link DdiSecurityProperties.Rp#getTrustedIPs()}. Will be removed.
 */
@Deprecated(forRemoval = true, since = "1.2.1")
@SuppressWarnings("java:S1133")
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory")
@Conditional(DdiTrustedIpsConfiguration.TrustedIpsSetCondition.class)
public class DdiTrustedIpsConfiguration {

    static final String TRUSTED_IPS = "hawkbit.server.ddi.security.rp.trusted-ips";
    // just checked, not a DdiSecurityProperties property - it is only a confirmation for the deprecated trusted IPs
    static final String TRUSTED_IPS_ENABLE = "hawkbit.server.ddi.security.rp.trusted-ips-enable";

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> ddiPeerAddressValveCustomizer(final Environment environment) {
        if (Boolean.FALSE.equals(environment.getProperty(TRUSTED_IPS_ENABLE, Boolean.class, false))) {
            throw new IllegalStateException(String.format(
                    "%s is deprecated and will be removed! The deployment shall protect access to hawkBit " +
                            "(e.g. only trusted peers shall be able to call DDI with client certificate headers). " +
                            "If still needed, set %s=true explicitly.",
                    TRUSTED_IPS, TRUSTED_IPS_ENABLE));
        }
        log.warn("{} is deprecated and will be removed! The deployment shall protect access to hawkBit " +
                "(e.g. only trusted peers shall be able to call DDI with client certificate headers).", TRUSTED_IPS);
        return factory -> factory.addEngineValves(new PeerAddressValve());
    }

    /**
     * Stores the connection peer IP address as request attribute. {@link Request#getPeerAddr()} is not changed by the RemoteIpValve, so the
     * order of the valves doesn't matter.
     */
    static class PeerAddressValve extends ValveBase {

        PeerAddressValve() {
            super(true);
        }

        @Override
        public void invoke(final Request request, final Response response) throws IOException, ServletException {
            request.setAttribute(AuthenticationFilters.PEER_ADDRESS_ATTRIBUTE, request.getPeerAddr());
            getNext().invoke(request, response);
        }
    }

    // binds the list - so both, comma separated and indexed (trusted-ips[0]=...), notations are supported
    static class TrustedIpsSetCondition implements Condition {

        @Override
        public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            return Binder.get(context.getEnvironment())
                    .bind(TRUSTED_IPS, Bindable.listOf(String.class))
                    .map(trustedIps -> !trustedIps.isEmpty())
                    .orElse(false);
        }
    }
}
