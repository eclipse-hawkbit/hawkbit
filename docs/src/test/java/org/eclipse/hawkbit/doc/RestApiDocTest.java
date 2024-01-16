/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.doc;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.eclipse.hawkbit.repository.test.util.SharedSqlTestDatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({SharedSqlTestDatabaseExtension.class})
class RestApiDocTest {
    private static final String MANAGEMENT_PREFIX = "mgmt";
    private static final String DDI_PREFIX = "ddi";
    private static final String TARGET_DIRECTORY = "content/rest-api/";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void openapiJson() throws IOException {
        final ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + port + "/v3/api-docs", String.class);
        final String openapiDoc = response.getBody();
        assertThat(openapiDoc).isNotNull();
        splitDocumentation(openapiDoc);
    }

    private static void splitDocumentation(final String json) throws IOException {
        processDocumentation(json, true);
        processDocumentation(json, false);
    }

    private static void processDocumentation(final String json, final boolean isMgmt) throws IOException {
        final JsonNode rootNode = OBJECT_MAPPER.readTree(json);
        updateJsonNodeForApi(rootNode, isMgmt);
        saveDocumentation(rootNode, isMgmt);
    }

    private static void updateJsonNodeForApi(final JsonNode rootNode, final boolean isMgmt) {
        removeTags(rootNode, isMgmt);
        removePaths(rootNode, isMgmt);
        removeComponents(rootNode, isMgmt);
    }

    private static void removeTags(final JsonNode rootNode, final boolean isMgmt) {
        final ArrayNode tagsNode = (ArrayNode) rootNode.get("tags");
        final ArrayNode modifiedTagsNode = OBJECT_MAPPER.createArrayNode();

        for (final JsonNode tagNode : tagsNode) {
            String tagName = tagNode.get("name").asText();
            if (isMgmt != tagName.startsWith("DDI")) {
                modifiedTagsNode.add(tagNode);
            }
        }

        ((ObjectNode) rootNode).set("tags", modifiedTagsNode);
    }
    private static void removePaths(final JsonNode rootNode, final boolean isMgmt) {
        final ObjectNode pathsNode = (ObjectNode) rootNode.get("paths");
        final List<String> fieldsToRemove = new ArrayList<>();
        pathsNode.fieldNames().forEachRemaining(fieldName -> {
            final JsonNode pathNode = pathsNode.get(fieldName);
            pathNode.fieldNames().forEachRemaining(path -> {
                final JsonNode methodNode = pathNode.get(path);
                final JsonNode tagsNode = methodNode.get("tags");
                if (tagsNode != null) {
                    for (JsonNode tagNode : tagsNode) {
                        final String tag = tagNode.asText();
                        if (isMgmt == tag.startsWith("DDI")) {
                            fieldsToRemove.add(fieldName);
                            break;
                        }
                    }
                }
            });
        });
        fieldsToRemove.forEach(pathsNode::remove);
    }

    private static void removeComponents(final JsonNode rootNode, final boolean isMgmt) {
        final ObjectNode schemasNode = (ObjectNode) rootNode.get("components").get("schemas");

        List<String> fieldsToRemove = new ArrayList<>();
        schemasNode.fieldNames().forEachRemaining(fieldName -> {
            if (shouldDeleteComponent(fieldName, isMgmt)) {
                fieldsToRemove.add(fieldName);
            }
        });
        fieldsToRemove.forEach(schemasNode::remove);
    }

    private static boolean shouldDeleteComponent(final String fieldName, final boolean isMgmt) {
        if (isMgmt) {
            return fieldName.startsWith("Ddi");
        }
        return !(fieldName.startsWith("Ddi") || fieldName.equals("Link") || fieldName.equals("Links") || fieldName.equals("ExceptionInfo"));

    }

    private static void saveDocumentation(final JsonNode rootNode, final boolean isMgmt) throws IOException {
        final String prefix = isMgmt ? MANAGEMENT_PREFIX : DDI_PREFIX;
        saveAsJson(rootNode, prefix);
        saveAsYaml(rootNode, prefix);
    }

    private static void saveAsJson(final JsonNode rootNode, final String prefix) throws IOException {
        final Path targetPath = getTargetPath(prefix, ".json");
        Files.writeString(targetPath, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));
    }

    private static void saveAsYaml(final JsonNode rootNode, final String prefix) throws IOException {
        final YAMLMapper yamlMapper = new YAMLMapper();
        final Path targetPath = getTargetPath(prefix, ".yaml");
        Files.writeString(targetPath, yamlMapper.writeValueAsString(rootNode));
    }

    private static Path getTargetPath(final String prefix, final String extension) throws IOException {
        final Path targetPath = Paths.get(TARGET_DIRECTORY + prefix + extension);
        Files.createDirectories(targetPath.getParent());
        return targetPath;
    }
}

