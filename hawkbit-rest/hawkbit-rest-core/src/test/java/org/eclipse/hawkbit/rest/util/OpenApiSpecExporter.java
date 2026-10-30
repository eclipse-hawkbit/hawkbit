/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Exports an OpenAPI group document (as served by springdoc under <code>/v3/api-docs/{group}</code>) into a file.
 * <p/>
 * The output is pretty-printed in a fixed format (2-space indent, <code>\n</code> line endings independent of the OS, trailing newline,
 * UTF-8) and keeps the order of the served document - so re-exporting an unchanged API produces a byte-identical file.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OpenApiSpecExporter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DefaultIndenter INDENTER = new DefaultIndenter("  ", "\n");
    private static final DefaultPrettyPrinter PRETTY_PRINTER = new DefaultPrettyPrinter(
            Separators.createDefaultInstance().withObjectNameValueSpacing(Separators.Spacing.AFTER))
            .withObjectIndenter(INDENTER)
            .withArrayIndenter(INDENTER);

    /**
     * Fetches the OpenAPI document of the group, asserts it's a valid OpenAPI 3.0 document with paths and writes it to the output file.
     *
     * @param mvc the mock MVC to fetch the document with
     * @param group the springdoc group name (as set in the <code>GroupedOpenApi</code>), URL encoded as URI template variable
     * @param output the file to write to, parent directories are created if missing
     * @return the fetched document
     */
    @SneakyThrows
    public static JsonNode export(final MockMvc mvc, final String group, final Path output) {
        final String body = mvc.perform(get("/v3/api-docs/{group}", group))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        final JsonNode spec = OBJECT_MAPPER.readTree(body);
        assertThat(spec.path("openapi").asString()).as("OpenAPI version").startsWith("3.0");
        assertThat(spec.path("paths").isEmpty()).as("Paths are empty").isFalse();

        final Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, OBJECT_MAPPER.writer().with(PRETTY_PRINTER).writeValueAsString(spec) + "\n", StandardCharsets.UTF_8);
        return spec;
    }
}
