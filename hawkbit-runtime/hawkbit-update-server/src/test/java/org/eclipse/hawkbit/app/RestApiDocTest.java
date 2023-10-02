/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.app;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles({"maven"})
class RestApiDocTest {
    private static final String MANAGEMENT_PREFIX = "mgmt-openapi";
    private static final String DDI_PREFIX = "ddi-openapi";
    private static final String TARGET_DIRECTORY = "target/rest-api/";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void openapiJson() throws IOException {
        ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + port + "/v3/api-docs", String.class);
        String openapiDoc = response.getBody();
        assertThat(openapiDoc).isNotNull();
        splitDocumentation(openapiDoc);
    }

    private void splitDocumentation(String json) throws IOException {
        processDocumentation(json, true);
        processDocumentation(json, false);
    }

    private void processDocumentation(String json, boolean isMgmt) throws IOException {
        JsonNode rootNode = objectMapper.readTree(json);
        updateJsonNodeForApi(rootNode, isMgmt);
        saveDocumentation(rootNode, isMgmt);
    }

    private void updateJsonNodeForApi(JsonNode rootNode, boolean isMgmt) {
        removeTags(rootNode, isMgmt);
        removePaths(rootNode, isMgmt);
        removeComponents(rootNode, isMgmt);
    }

    private void removeTags(JsonNode rootNode, boolean isMgmt) {
        ArrayNode tagsNode = (ArrayNode) rootNode.get("tags");
        ArrayNode modifiedTagsNode = objectMapper.createArrayNode();

        for (JsonNode tagNode : tagsNode) {
            String tagName = tagNode.get("name").asText();
            if (isMgmt != tagName.startsWith("DDI")) {
                modifiedTagsNode.add(tagNode);
            }
        }

        ((ObjectNode) rootNode).set("tags", modifiedTagsNode);
    }
    private void removePaths(JsonNode rootNode, boolean isMgmt) {
        ObjectNode pathsNode = (ObjectNode) rootNode.get("paths");
        List<String> fieldsToRemove = new ArrayList<>();
        pathsNode.fieldNames().forEachRemaining(fieldName -> {
            JsonNode pathNode = pathsNode.get(fieldName);
            pathNode.fieldNames().forEachRemaining(path -> {
                JsonNode methodNode = pathNode.get(path);
                JsonNode tagsNode = methodNode.get("tags");
                if (tagsNode != null) {
                    for (JsonNode tagNode : tagsNode) {
                        String tag = tagNode.asText();
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

    private void removeComponents(JsonNode rootNode, boolean isMgmt) {
        ObjectNode schemasNode = (ObjectNode) rootNode.get("components").get("schemas");

        List<String> fieldsToRemove = new ArrayList<>();
        schemasNode.fieldNames().forEachRemaining(fieldName -> {
            if (shouldDeleteComponent(fieldName, isMgmt)) {
                fieldsToRemove.add(fieldName);
            }
        });
        fieldsToRemove.forEach(schemasNode::remove);
    }

    private boolean shouldDeleteComponent(String fieldName, boolean isMgmt) {
        if (isMgmt) {
            return fieldName.startsWith("Ddi");
        }
        return !(fieldName.startsWith("Ddi") || fieldName.equals("Link") || fieldName.equals("ExceptionInfo"));

    }

    private void saveDocumentation(JsonNode rootNode, boolean isMgmt) throws IOException {
        String prefix = isMgmt ? MANAGEMENT_PREFIX : DDI_PREFIX;
        saveAsJson(rootNode, prefix);
        saveAsYaml(rootNode, prefix);
    }

    private void saveAsJson(JsonNode rootNode, String prefix) throws IOException {
        Path targetPath = getTargetPath(prefix, ".json");
        Files.writeString(targetPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));
    }

    private void saveAsYaml(JsonNode rootNode, String prefix) throws IOException {
        YAMLMapper yamlMapper = new YAMLMapper();
        Path targetPath = getTargetPath(prefix, ".yaml");
        Files.writeString(targetPath, yamlMapper.writeValueAsString(rootNode));
    }

    private Path getTargetPath(String prefix, String extension) throws IOException {
        Path targetPath = Paths.get(TARGET_DIRECTORY + prefix + extension);
        Files.createDirectories(targetPath.getParent());
        return targetPath;
    }
}

