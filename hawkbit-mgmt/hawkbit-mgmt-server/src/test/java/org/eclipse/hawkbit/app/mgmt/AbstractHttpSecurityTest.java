/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app.mgmt;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.eclipse.hawkbit.repository.test.util.SharedSqlTestDatabaseExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Base for security tests over real HTTP (embedded server). Needed when the request processing relies on a real servlet container,
 * e.g. on the servlet filter mappings, the not normalized request paths or the request (context) paths of the tenant path.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SharedSqlTestDatabaseExtension.class)
abstract class AbstractHttpSecurityTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    protected final HttpResponse<String> get(final String path, final String authorization) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path)).GET(), authorization);
    }

    protected final HttpResponse<String> post(final String path, final String authorization, final String json)
            throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(json)), authorization);
    }

    protected final String baseUrl() {
        return "http://localhost:" + port;
    }

    protected static String basicAuth(final String user, final String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private URI uri(final String path) {
        return URI.create(baseUrl() + path);
    }

    private static HttpResponse<String> send(final HttpRequest.Builder builder, final String authorization)
            throws IOException, InterruptedException {
        if (authorization != null) {
            builder.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
