/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.eclipse.hawkbit.rest.util.OpenApiSpecExporter;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * Exports the Direct Device Integration API OpenAPI spec to <code>target/openapi/ddi.json</code>. The docs workflow
 * (<code>.github/workflows/rest-api-docs.yaml</code>) runs this test and copies the file into <code>docs/rest-api/</code>, rendered by
 * <code>docs/rest-api/ddi.html</code>. It writes into <code>target</code> only, so normal builds leave the source tree unchanged.
 * <p/>
 * Feature: Component Tests - Direct Device Integration API<br/>
 * Story: OpenAPI spec export
 */
// same springdoc settings as the ddi-server (application.properties) - so the spec matches the served one
@TestPropertySource(properties = {
        "springdoc.api-docs.version=openapi_3_0",
        "springdoc.show-oauth2-endpoints=true",
        "springdoc.show-login-endpoint=true",
        "springdoc.packages-to-scan=org.eclipse.hawkbit.ddi",
        "springdoc.paths-to-exclude=/system/**" })
class DdiOpenApiSpecExportTest extends AbstractDDiApiIntegrationTest {

    @Test
    void exportOpenApiSpec() {
        final JsonNode spec = OpenApiSpecExporter.export(mvc, "Direct Device Integration API", Path.of("target", "openapi", "ddi.json"));
        assertThat(spec.path("info").path("title").asString()).isEqualTo("Direct Device Integration API");
        assertThat(spec.path("servers")).hasSize(1);
        assertThat(spec.path("servers").path(0).path("url").asString()).isEqualTo("/");
    }
}
