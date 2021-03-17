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

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiMetadata'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiMetadataTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        String key = "testKey";
        String value = "testValue";
        DdiMetadata ddiMetadata = new DdiMetadata(key, value);

        // Test
        String serializedDdiMetadata = mapper.writeValueAsString(ddiMetadata);
        DdiMetadata deserializedDdiMetadata = mapper.readValue(serializedDdiMetadata, DdiMetadata.class);

        assertThat(serializedDdiMetadata).contains(key, value);
        assertThat(deserializedDdiMetadata.getKey()).isEqualTo(ddiMetadata.getKey());
        assertThat(deserializedDdiMetadata.getValue()).isEqualTo(ddiMetadata.getValue());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiMetadata = "{\"key\":\"testKey\",\"value\":\"testValue\",\"unknownProperty\":\"test\"}";

        // Test
        DdiMetadata ddiMetadata = mapper.readValue(serializedDdiMetadata, DdiMetadata.class);

        assertThat(ddiMetadata.getKey()).isEqualTo("testKey");
        assertThat(ddiMetadata.getValue()).isEqualTo("testValue");
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiMetadata = "{\"key\":[\"testKey\"],\"value\":\"testValue\"}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiMetadata, DdiMetadata.class));
    }
}