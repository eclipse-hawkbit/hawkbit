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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.valves.RemoteIpValve;
import org.eclipse.hawkbit.security.controller.AuthenticationFilters;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Deprecated DDI reverse proxy trusted IPs with embedded Tomcat
 */
class DdiTrustedIpsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DdiTrustedIpsConfiguration.class));

    @Test
    void noValveIfTrustedIpsNotSet() {
        contextRunner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(WebServerFactoryCustomizer.class));
    }

    @Test
    void failsIfTrustedIpsSetButNotEnabled() {
        contextRunner
                .withPropertyValues(DdiTrustedIpsConfiguration.TRUSTED_IPS + "=10.0.0.1")
                .run(context -> assertThat(context).hasFailed().getFailure().rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(DdiTrustedIpsConfiguration.TRUSTED_IPS_ENABLE));
    }

    @Test
    @SuppressWarnings("unchecked")
    void valveAddedIfTrustedIpsSetAndEnabled() {
        contextRunner
                .withPropertyValues(
                        DdiTrustedIpsConfiguration.TRUSTED_IPS + "[0]=10.0.0.1", // indexed notation
                        DdiTrustedIpsConfiguration.TRUSTED_IPS_ENABLE + "=true")
                .run(context -> {
                    final TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
                    context.getBean(WebServerFactoryCustomizer.class).customize(factory);
                    assertThat(factory.getEngineValves()).hasExactlyElementsOfTypes(DdiTrustedIpsConfiguration.PeerAddressValve.class);
                });
    }

    /**
     * With a real Tomcat and RemoteIpValve: the remote address is the forwarded client IP, while the peer address attribute is the
     * (trusted proxy) connection peer.
     */
    @Test
    void peerAddressIsTheConnectionPeerWhileRemoteAddressIsForwarded() throws Exception {
        final TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
        // default internal proxies include the loopback - so the test client is a trusted proxy
        factory.addEngineValves(new RemoteIpValve(), new DdiTrustedIpsConfiguration.PeerAddressValve());
        final WebServer webServer = factory.getWebServer(servletContext -> servletContext.addServlet("echo", new HttpServlet() {

            @Override
            protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws java.io.IOException {
                response.getWriter().write(request.getRemoteAddr() + "|" + request.getAttribute(AuthenticationFilters.PEER_ADDRESS_ATTRIBUTE));
            }
        }).addMapping("/*"));
        webServer.start();
        try (final HttpClient client = HttpClient.newHttpClient()) {
            final HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + webServer.getPort() + "/"))
                            .header("X-Forwarded-For", "203.0.113.7")
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.body()).isEqualTo("203.0.113.7|127.0.0.1");
        } finally {
            webServer.stop();
        }
    }
}
