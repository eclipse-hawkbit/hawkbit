/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ddi.json.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiChunk'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiChunkTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        String part = "1234";
        String version = "1.0";
        String name = "Dummy-Artifact";
        List<DdiArtifact> dummyArtifacts = Collections.emptyList();
        DdiChunk ddiChunk = new DdiChunk(part, version, name, dummyArtifacts, null);

        // Test
        String serializedDdiChunk = mapper.writeValueAsString(ddiChunk);
        DdiChunk deserializedDdiChunk = mapper.readValue(serializedDdiChunk, DdiChunk.class);

        assertThat(serializedDdiChunk).contains(part, version, name);
        assertThat(deserializedDdiChunk.getPart()).isEqualTo(part);
        assertThat(deserializedDdiChunk.getVersion()).isEqualTo(version);
        assertThat(deserializedDdiChunk.getName()).isEqualTo(name);
        assertThat(deserializedDdiChunk.getArtifacts().size()).isEqualTo(0);
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiChunk = "{\"part\":\"1234\",\"version\":\"1.0\",\"name\":\"Dummy-Artifact\",\"artifacts\":[],\"unknownProperty\":\"test\"}";

        // Test
        DdiChunk ddiChunk = mapper.readValue(serializedDdiChunk, DdiChunk.class);

        assertThat(ddiChunk.getPart()).isEqualTo("1234");
        assertThat(ddiChunk.getVersion()).isEqualTo("1.0");
        assertThat(ddiChunk.getName()).isEqualTo("Dummy-Artifact");
        assertThat(ddiChunk.getArtifacts().size()).isEqualTo(0);
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiChunk = "{\"part\":[\"1234\"],\"version\":\"1.0\",\"name\":\"Dummy-Artifact\",\"artifacts\":[]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiChunk, DdiChunk.class));
    }
}