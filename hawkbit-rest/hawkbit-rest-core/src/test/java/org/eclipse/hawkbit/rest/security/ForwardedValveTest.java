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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.mock.env.MockEnvironment;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Forwarded headers processing - Tomcat valve
 */
class ForwardedValveTest {

    private static final ForwardedResolver RESOLVER = ForwardedResolver.of(new HawkbitSecurityProperties());

    /**
     * Verifies with a real Tomcat that the remote address, scheme, host, port and request URL are the forwarded ones (the loopback test client
     * is a default trusted proxy) and the client address is stored
     */
    @Test
    void appliesForwardedValuesWithRealTomcat() throws Exception {
        final TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
        new ForwardedValve.Customizer(RESOLVER, new MockEnvironment()).customize(factory);
        assertThat(factory.getEngineValves()).hasExactlyElementsOfTypes(ForwardedValve.class);
        final WebServer webServer = factory.getWebServer(servletContext -> servletContext.addServlet("echo", new HttpServlet() {

            @Override
            protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
                final ClientAddress clientAddress = ClientAddress.of(request);
                response.getWriter().write(String.join("|", request.getRemoteAddr(), request.getRemoteHost(), request.getScheme(),
                        String.valueOf(request.isSecure()), request.getServerName(), String.valueOf(request.getServerPort()),
                        request.getRequestURL(), clientAddress.peer(), String.valueOf(clientAddress.forwardedFor())));
            }
        }).addMapping("/*"));
        webServer.start();
        try (final HttpClient client = HttpClient.newHttpClient()) {
            final HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + webServer.getPort() + "/path"))
                            .header("X-Forwarded-For", "6.6.6.6, 203.0.113.7")
                            .header("X-Forwarded-Proto", "https")
                            .header("X-Forwarded-Host", "example.com")
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.body())
                    .isEqualTo("203.0.113.7|203.0.113.7|https|true|example.com|443|https://example.com/path|127.0.0.1|[6.6.6.6]");
        } finally {
            webServer.stop();
        }
    }

    /**
     * Verifies that the valve is not added with a forward headers strategy other than NONE
     */
    @Test
    void notAddedInLegacyMode() {
        final TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
        new ForwardedValve.Customizer(RESOLVER, new MockEnvironment().withProperty("server.forward-headers-strategy", "native"))
                .customize(factory);
        assertThat(factory.getEngineValves()).isEmpty();
    }

    /**
     * Verifies that the resolved values are kept after the invocation - for the access log, written after the valve pipeline returns
     */
    @Test
    void keepsResolvedValuesForAccessLog() throws Exception {
        final org.apache.coyote.Request coyoteRequest = new org.apache.coyote.Request();
        coyoteRequest.peerAddr().setString("10.0.0.1");
        coyoteRequest.scheme().setString("http");
        coyoteRequest.serverName().setString("internal");
        coyoteRequest.setServerPort(8080);
        coyoteRequest.requestURI().setString("/rest/v1/targets");
        coyoteRequest.getMimeHeaders().addValue("X-Forwarded-For").setString("203.0.113.7");
        coyoteRequest.getMimeHeaders().addValue("X-Forwarded-Proto").setString("https");
        coyoteRequest.getMimeHeaders().addValue("X-Forwarded-Host").setString("example.com");
        coyoteRequest.getMimeHeaders().addValue("X-Forwarded-Port").setString("8443");
        final Request request = new Request(new Connector(), coyoteRequest);
        request.setRemoteAddr("10.0.0.1");
        request.setRemoteHost("10.0.0.1");

        final List<String> seen = new ArrayList<>();
        final ForwardedValve valve = new ForwardedValve(RESOLVER);
        valve.setNext(new ValveBase() {

            @Override
            public void invoke(final Request request, final Response response) {
                seen.addAll(values(request));
            }
        });
        valve.invoke(request, null);

        assertThat(seen).containsExactly("203.0.113.7", "203.0.113.7", "https", "true", "example.com", "8443");
        assertThat(values(request)).containsExactly("203.0.113.7", "203.0.113.7", "https", "true", "example.com", "8443");
        assertThat(ClientAddress.of(request)).isEqualTo(new ClientAddress("10.0.0.1", "203.0.113.7", List.of(), null));
    }

    private static List<String> values(final Request request) {
        return List.of(request.getRemoteAddr(), request.getRemoteHost(), request.getScheme(), String.valueOf(request.isSecure()),
                request.getServerName(), String.valueOf(request.getServerPort()));
    }
}
