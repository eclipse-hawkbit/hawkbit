/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.ddi.json.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiConfig'
 * <p/>
 * Feature: Unit Tests - Direct Device Integration API<br/>
 * Story: Serializability of DDI api Models
 */
class DdiConfigTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Verify the correct serialization and deserialization of the model
     */
    @Test
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final DdiPolling ddiPolling = new DdiPolling("10");
        final DdiConfig ddiConfig = new DdiConfig(ddiPolling);

        // Test
        final String serializedDdiConfig = OBJECT_MAPPER.writeValueAsString(ddiConfig);
        final DdiConfig deserializedDdiConfig = OBJECT_MAPPER.readValue(serializedDdiConfig, DdiConfig.class);
        assertThat(serializedDdiConfig).contains("10");
        assertThat(deserializedDdiConfig.getPolling().getSleep()).isEqualTo("10");
    }

    /**
     * Verify the correct deserialization of a model with a additional unknown property
     */
    @Test
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiConfig = "{\"polling\":{\"sleep\":\"123\"},\"unknownProperty\":\"test\"}";

        // Test
        final DdiConfig ddiConfig = OBJECT_MAPPER.readValue(serializedDdiConfig, DdiConfig.class);
        assertThat(ddiConfig.getPolling().getSleep()).isEqualTo("123");
    }

    /**
     * Verify that deserialization fails for known properties with a wrong datatype
     */
    @Test
    void shouldFailForObjectWithWrongDataTypes() {
        // Setup
        final String serializedDdiConfig = "{\"polling\":{\"sleep\":[\"10\"]}}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiConfig, DdiConfig.class));
    }
}