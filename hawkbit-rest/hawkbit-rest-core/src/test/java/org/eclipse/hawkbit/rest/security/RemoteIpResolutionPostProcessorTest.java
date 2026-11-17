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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Remote IP resolution
 */
class RemoteIpResolutionPostProcessorTest {

    private static final String X_FORWARDED_FOR = HawkbitSecurityProperties.Clients.X_FORWARDED_FOR;

    /**
     * Verifies that with NATIVE strategy and embedded Tomcat the remote address (resolved by Tomcat) is used
     */
    @Test
    void nativeWithEmbeddedTomcatUsesRemoteAddr() {
        assertThat(remoteIpHeader(tomcatContext(), "native")).isNull();
    }

    /**
     * Verifies that with the (deprecated) legacy remote IP resolution the header is read, even with NATIVE strategy and embedded Tomcat
     */
    @Test
    @SuppressWarnings("removal")
    void legacyRemoteIpResolutionReadsHeader() {
        final ServletWebServerApplicationContext context = tomcatContext();
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty(RemoteIpResolutionPostProcessor.LEGACY_REMOTE_IP_RESOLUTION, "true");
        context.setEnvironment(environment);
        assertThat(remoteIpHeader(context, "NATIVE")).isEqualTo(X_FORWARDED_FOR);
    }

    /**
     * Verifies that without a web server (e.g. mock web environment) the header is read
     */
    @Test
    void noWebServerReadsHeader() {
        final GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(TomcatServletWebServerFactory.class);
        assertThat(remoteIpHeader(context, "NATIVE")).isEqualTo(X_FORWARDED_FOR);
    }

    /**
     * Verifies that with NATIVE strategy and another web server (not Tomcat, e.g. Jetty) the header is read
     */
    @Test
    void nativeWithOtherWebServerReadsHeader() {
        assertThat(remoteIpHeader(new ServletWebServerApplicationContext(), "NATIVE")).isEqualTo(X_FORWARDED_FOR);
    }

    /**
     * Verifies that with other strategies (explicit or not set) the header is read
     */
    @Test
    void otherStrategiesReadHeader() {
        assertThat(remoteIpHeader(tomcatContext(), "FRAMEWORK")).isEqualTo(X_FORWARDED_FOR);
        assertThat(remoteIpHeader(tomcatContext(), "NONE")).isEqualTo(X_FORWARDED_FOR);
        assertThat(remoteIpHeader(tomcatContext(), null)).isEqualTo(X_FORWARDED_FOR);
    }

    /**
     * Verifies that the startup fails if the security properties are created without being post processed (e.g. before
     * the post processor is registered), and passes if they are post processed
     */
    @Test
    void failsIfSecurityPropertiesAreNotPostProcessed() {
        final ServletWebServerApplicationContext context = tomcatContext();
        final HawkbitSecurityProperties securityProperties = new HawkbitSecurityProperties();
        context.getBeanFactory().registerSingleton("securityProperties", securityProperties);
        context.setEnvironment(new MockEnvironment());

        final RemoteIpResolutionPostProcessor notProcessing = new RemoteIpResolutionPostProcessor();
        notProcessing.setApplicationContext(context);
        assertThatThrownBy(notProcessing::afterSingletonsInstantiated).isInstanceOf(IllegalStateException.class);

        final RemoteIpResolutionPostProcessor processing = new RemoteIpResolutionPostProcessor();
        processing.setApplicationContext(context);
        processing.postProcessAfterInitialization(securityProperties, "securityProperties");
        assertThatNoException().isThrownBy(processing::afterSingletonsInstantiated);
    }

    private static ServletWebServerApplicationContext tomcatContext() {
        final ServletWebServerApplicationContext context = new ServletWebServerApplicationContext();
        context.registerBean(TomcatServletWebServerFactory.class);
        return context;
    }

    private static String remoteIpHeader(final GenericApplicationContext context, final String strategy) {
        // keeps an already set (mock) environment, e.g. with additional properties
        final MockEnvironment environment = context.getEnvironment() instanceof MockEnvironment mockEnvironment
                ? mockEnvironment
                : new MockEnvironment();
        if (strategy != null) {
            environment.setProperty(RemoteIpResolutionPostProcessor.FORWARD_HEADERS_STRATEGY, strategy);
        }
        environment.setProperty(RemoteIpResolutionPostProcessor.TOMCAT_RELATIVE_REDIRECTS, "true");
        context.setEnvironment(environment);

        final RemoteIpResolutionPostProcessor postProcessor = new RemoteIpResolutionPostProcessor();
        postProcessor.setApplicationContext(context);
        final HawkbitSecurityProperties securityProperties = new HawkbitSecurityProperties();
        postProcessor.postProcessAfterInitialization(securityProperties, "securityProperties");
        return securityProperties.getClients().getRemoteIpHeader();
    }
}
