/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.servlet.Filter;

import org.eclipse.hawkbit.rest.SecurityManagedConfiguration.LegacyClientAddressFilter;
import org.eclipse.hawkbit.rest.SecurityManagedConfiguration.Mode;
import org.eclipse.hawkbit.rest.security.ClientAddress;
import org.eclipse.hawkbit.rest.security.ForwardedFilter;
import org.eclipse.hawkbit.rest.security.ForwardedResolver;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Client IP resolution mode selection
 */
@ExtendWith(OutputCaptureExtension.class)
class ClientAddressModeTest {

    private static final String X_FORWARDED_FOR = HawkbitSecurityProperties.Clients.X_FORWARDED_FOR;

    /**
     * Verifies that with strategy NONE (explicit or default) and embedded Tomcat the valve is used (client address filter disabled)
     */
    @Test
    void noneWithEmbeddedTomcatUsesValve(final CapturedOutput output) {
        assertThat(SecurityManagedConfiguration.mode(withStrategy(tomcatContext(), "none"))).isEqualTo(Mode.VALVE);
        assertThat(SecurityManagedConfiguration.mode(withStrategy(tomcatContext(), null))).isEqualTo(Mode.VALVE);
        assertThat(clientAddressFilter(withStrategy(tomcatContext(), "NONE")).isEnabled()).isFalse();
        assertThat(output).contains("with the Tomcat valve");
    }

    /**
     * Verifies that with strategy NONE and another web server (e.g. Jetty) the filter is used
     */
    @Test
    void noneWithOtherWebServerUsesFilter() {
        assertThat(SecurityManagedConfiguration.mode(withStrategy(new ServletWebServerApplicationContext(), "NONE")))
                .isEqualTo(Mode.FILTER);
        final FilterRegistrationBean<Filter> filter = clientAddressFilter(withStrategy(new ServletWebServerApplicationContext(), "NONE"));
        assertThat(filter.isEnabled()).isTrue();
        assertThat(filter.getFilter()).isInstanceOf(ForwardedFilter.class);
    }

    /**
     * Verifies that with other strategies the legacy mode is used - the header is read as is
     */
    @Test
    void otherStrategiesAreLegacy(final CapturedOutput output) throws Exception {
        assertThat(SecurityManagedConfiguration.mode(withStrategy(tomcatContext(), "NATIVE"))).isEqualTo(Mode.LEGACY);
        assertThat(SecurityManagedConfiguration.mode(withStrategy(tomcatContext(), "FRAMEWORK"))).isEqualTo(Mode.LEGACY);

        final FilterRegistrationBean<Filter> filter = clientAddressFilter(withStrategy(tomcatContext(), "NATIVE"));
        assertThat(output).contains("hawkBit doesn't process the forwarded headers");
        assertThat(filter.isEnabled()).isTrue();
        assertThat(filter.getFilter()).isInstanceOf(LegacyClientAddressFilter.class);
        // the client IP is read from the header as is (the most left one), the peer is the remote address
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader(X_FORWARDED_FOR, "6.6.6.6, 203.0.113.7");
        filter.getFilter().doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(ClientAddress.of(request)).isEqualTo(new ClientAddress("10.0.0.1", "6.6.6.6", List.of(), null));
    }

    /**
     * Verifies that in legacy mode the client IP found in the header is normalized to the canonical form (IPv4-mapped IPv6 as IPv4, IPv6 in
     * full form), and without (valid) header the remote address is the client IP
     */
    @Test
    void legacyClientIpIsCanonical() throws Exception {
        final Filter filter = clientAddressFilter(withStrategy(tomcatContext(), "NATIVE")).getFilter();

        assertThat(legacyClientAddress(filter, "::ffff:1.2.3.4")).isEqualTo(new ClientAddress("10.0.0.1", "1.2.3.4", List.of(), null));
        assertThat(legacyClientAddress(filter, "[2001:0db8:0000:0000:0000:0000:0000:0001]:4233").ip()).isEqualTo("2001:db8:0:0:0:0:0:1");
        assertThat(legacyClientAddress(filter, "010.000.000.001").ip()).isEqualTo("10.0.0.1");
        // no / invalid header - the remote address
        assertThat(legacyClientAddress(filter, null)).isEqualTo(new ClientAddress("10.0.0.1", "10.0.0.1", List.of(), null));
        assertThat(legacyClientAddress(filter, "unknown").ip()).isEqualTo("10.0.0.1");
        assertThat(legacyClientAddress(filter, "999.1.1.1").ip()).isEqualTo("10.0.0.1");
        // compressed IPv6 is not found (as before)
        assertThat(legacyClientAddress(filter, "2001:db8::1").ip()).isEqualTo("10.0.0.1");
    }

    /**
     * Verifies that a warning is logged if Tomcat RemoteIpValve properties are set while hawkBit processes the forwarded headers (they are
     * ignored), not in legacy mode or if not set
     */
    @Test
    void tomcatRemoteIpPropertiesIgnoredWarning(final CapturedOutput output) {
        final ServletWebServerApplicationContext withRemoteIp = tomcatContext();
        withRemoteIp.setEnvironment(new MockEnvironment()
                .withProperty("server.tomcat.remoteip.internal-proxies", "10\\..*")
                .withProperty("server.tomcat.remoteip.remote-ip-header", "X-Real-IP"));
        clientAddressFilter(withStrategy(withRemoteIp, "NONE"));
        assertThat(output).contains("[server.tomcat.remoteip.internal-proxies, server.tomcat.remoteip.remote-ip-header] are ignored!");
        // filter mode too
        final ServletWebServerApplicationContext other = new ServletWebServerApplicationContext();
        other.setEnvironment(new MockEnvironment().withProperty("server.tomcat.remoteip.port-header", "X-Forwarded-Port"));
        clientAddressFilter(withStrategy(other, "NONE"));
        assertThat(output).contains("[server.tomcat.remoteip.port-header] are ignored!");
    }

    /**
     * Verifies that no Tomcat RemoteIpValve properties warning is logged in legacy mode or if they are not set
     */
    @Test
    void noTomcatRemoteIpPropertiesWarning(final CapturedOutput output) {
        final ServletWebServerApplicationContext legacy = tomcatContext();
        legacy.setEnvironment(new MockEnvironment().withProperty("server.tomcat.remoteip.internal-proxies", "10\\..*"));
        clientAddressFilter(withStrategy(legacy, "NATIVE"));
        clientAddressFilter(withStrategy(tomcatContext(), "NONE"));
        clientAddressFilter(withStrategy(new ServletWebServerApplicationContext(), "NONE"));
        assertThat(output).contains("with the Tomcat valve").contains("with the servlet filter").doesNotContain("are ignored!");
    }

    /**
     * Verifies that without a web server (e.g. mock web environment) the header is read as is, without warning
     */
    @Test
    void noWebServerIsLegacy(final CapturedOutput output) {
        final GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean(TomcatServletWebServerFactory.class);
        assertThat(SecurityManagedConfiguration.mode(withStrategy(context, "NONE"))).isEqualTo(Mode.LEGACY);
        assertThat(clientAddressFilter(withStrategy(new GenericWebApplicationContext(), "NONE")).getFilter())
                .isInstanceOf(LegacyClientAddressFilter.class);
        assertThat(output).doesNotContain("hawkBit doesn't process the forwarded headers");
    }

    /**
     * Verifies that a missing or blank strategy is NONE - not deduced from the cloud platform (e.g. Kubernetes)
     */
    @Test
    void missingOrBlankStrategyIsNone() {
        final ServletWebServerApplicationContext kubernetes = tomcatContext();
        kubernetes.setEnvironment(new MockEnvironment().withProperty("spring.main.cloud-platform", "kubernetes"));
        assertThat(SecurityManagedConfiguration.mode(kubernetes)).isEqualTo(Mode.VALVE);
        assertThat(SecurityManagedConfiguration.mode(withStrategy(tomcatContext(), " "))).isEqualTo(Mode.VALVE);
    }

    private static ClientAddress legacyClientAddress(final Filter filter, final String header) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        if (header != null) {
            request.addHeader(X_FORWARDED_FOR, header);
        }
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return ClientAddress.of(request);
    }

    private static ServletWebServerApplicationContext tomcatContext() {
        final ServletWebServerApplicationContext context = new ServletWebServerApplicationContext();
        context.registerBean(TomcatServletWebServerFactory.class);
        return context;
    }

    // keeps an already set (mock) environment, e.g. with additional properties
    private static <T extends GenericApplicationContext> T withStrategy(final T context, final String strategy) {
        final MockEnvironment environment = context.getEnvironment() instanceof MockEnvironment mockEnvironment
                ? mockEnvironment
                : new MockEnvironment();
        if (strategy != null) {
            environment.setProperty(SecurityManagedConfiguration.FORWARD_HEADERS_STRATEGY, strategy);
        }
        context.setEnvironment(environment);
        return context;
    }

    private static FilterRegistrationBean<Filter> clientAddressFilter(final GenericApplicationContext context) {
        return clientAddressFilter(context, new HawkbitSecurityProperties());
    }

    private static FilterRegistrationBean<Filter> clientAddressFilter(
            final GenericApplicationContext context, final HawkbitSecurityProperties securityProperties) {
        return new SecurityManagedConfiguration().clientAddressFilter(
                context, securityProperties, ForwardedResolver.of(securityProperties));
    }
}
