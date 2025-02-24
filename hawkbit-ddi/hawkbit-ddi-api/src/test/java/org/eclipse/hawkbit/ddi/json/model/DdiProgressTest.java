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
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiProgress'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
class DdiProgressTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final DdiProgress ddiProgress = new DdiProgress(30, 100);

        // Test
        final String serializedDdiProgress = OBJECT_MAPPER.writeValueAsString(ddiProgress);
        final DdiProgress deserializedDdiProgress = OBJECT_MAPPER.readValue(serializedDdiProgress, DdiProgress.class);
        assertThat(serializedDdiProgress).contains(ddiProgress.getCnt().toString(), ddiProgress.getOf().toString());
        assertThat(deserializedDdiProgress.getCnt()).isEqualTo(ddiProgress.getCnt());
        assertThat(deserializedDdiProgress.getOf()).isEqualTo(ddiProgress.getOf());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiProgress = "{\"cnt\":30,\"of\":100,\"unknownProperty\":\"test\"}";

        // Test
        final DdiProgress ddiProgress = OBJECT_MAPPER.readValue(serializedDdiProgress, DdiProgress.class);
        assertThat(ddiProgress.getCnt()).isEqualTo(30);
        assertThat(ddiProgress.getOf()).isEqualTo(100);
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    void shouldFailForObjectWithWrongDataTypes() {
        // Setup
        final String serializedDdiProgress = "{\"cnt\":[30],\"of\":100}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiProgress, DdiProgress.class));
    }
}