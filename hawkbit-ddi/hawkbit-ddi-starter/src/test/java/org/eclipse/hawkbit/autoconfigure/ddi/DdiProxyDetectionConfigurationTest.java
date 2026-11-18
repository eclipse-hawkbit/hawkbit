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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.valves.RemoteIpValve;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: DDI proxy detection configuration with embedded Tomcat
 */
class DdiProxyDetectionConfigurationTest {

    private static final String SCHEDULER_THREAD = "ddi-proxy-detection";
    private static final String LOGGER = DdiProxyDetectionConfiguration.class.getName();
    private static final LoggingSystem LOGGING_SYSTEM = LoggingSystem.get(DdiProxyDetectionConfigurationTest.class.getClassLoader());

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DdiProxyDetectionConfiguration.class))
            .withBean(HawkbitSecurityProperties.class);

    // the detection logs on DEBUG
    @BeforeEach
    void enableDebugLog() {
        LOGGING_SYSTEM.setLogLevel(LOGGER, LogLevel.DEBUG);
    }

    @AfterEach
    void resetLog() {
        LOGGING_SYSTEM.setLogLevel(LOGGER, null);
    }

    @Test
    void disabledIfDebugLogNotEnabled() {
        LOGGING_SYSTEM.setLogLevel(LOGGER, LogLevel.INFO);
        contextRunner.run(context -> assertThat(context).hasNotFailed()
                .doesNotHaveBean(DdiProxyDetectionConfiguration.DdiProxyDetector.class)
                .doesNotHaveBean(DdiProxyDetectionConfiguration.ProxyChainValveCustomizer.class));
    }

    @Test
    void addsProxyChainValveFirst() throws InterruptedException {
        contextRunner
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(DdiProxyDetectionConfiguration.DdiProxyDetector.class);
                    final TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
                    // e.g. added by the Spring Boot customizer before
                    factory.addEngineValves(new RemoteIpValve());
                    context.getBean(DdiProxyDetectionConfiguration.ProxyChainValveCustomizer.class).customize(factory);
                    assertThat(factory.getEngineValves()).hasExactlyElementsOfTypes(
                            DdiProxyDetectionConfiguration.ProxyChainValve.class, RemoteIpValve.class);
                    assertThat(schedulerThreadAlive()).isTrue();
                });
        // context closed - the scheduler is shut down (close() as inferred destroy method)
        final long deadline = System.currentTimeMillis() + 5_000;
        while (schedulerThreadAlive() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(schedulerThreadAlive()).isFalse();
    }

    /**
     * With a real Tomcat: the attributes hold the forwarded header as received and the connection peer (the full hop chain), while the
     * RemoteIpValve resolves the remote address (skipping the trusted hops).
     */
    @Test
    void forwardedHeaderAsReceivedWhileRemoteAddressIsResolved() throws Exception {
        final TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
        factory.addEngineValves(new RemoteIpValve());
        new DdiProxyDetectionConfiguration.ProxyChainValveCustomizer("X-Forwarded-For").customize(factory);
        final WebServer webServer = factory.getWebServer(servletContext -> servletContext.addServlet("echo", new HttpServlet() {

            @Override
            protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
                response.getWriter().write(request.getRemoteAddr()
                        + "|" + request.getAttribute(DdiProxyDetectionConfiguration.DdiProxyDetector.FORWARDED_FOR_ATTRIBUTE)
                        + "|" + request.getAttribute(DdiProxyDetectionConfiguration.DdiProxyDetector.PEER_ADDRESS_ATTRIBUTE));
            }
        }).addMapping("/*"));
        webServer.start();
        try (final HttpClient client = HttpClient.newHttpClient()) {
            final HttpRequest forwarded = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + webServer.getPort() + "/"))
                    .header("X-Forwarded-For", "198.51.100.9, 203.0.113.7, 10.0.0.5")
                    .build();
            // default internal proxies include the loopback and 10/8 - so the test client and 10.0.0.5 are trusted proxies
            assertThat(client.send(forwarded, HttpResponse.BodyHandlers.ofString()).body())
                    .isEqualTo("203.0.113.7|198.51.100.9, 203.0.113.7, 10.0.0.5|127.0.0.1");
            // without forwarded header - just the connection peer
            assertThat(client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + webServer.getPort() + "/")).build(),
                    HttpResponse.BodyHandlers.ofString()).body())
                    .isEqualTo("127.0.0.1|null|127.0.0.1");
        } finally {
            webServer.stop();
        }
    }

    private static boolean schedulerThreadAlive() {
        return Thread.getAllStackTraces().keySet().stream().anyMatch(thread -> SCHEDULER_THREAD.equals(thread.getName()) && thread.isAlive());
    }
}
