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
 * Test serializability of DDI api model 'DdiControllerBase'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
class DdiControllerBaseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final DdiPolling ddiPolling = new DdiPolling("10");
        final DdiConfig ddiConfig = new DdiConfig(ddiPolling);
        final DdiControllerBase ddiControllerBase = new DdiControllerBase(ddiConfig);

        // Test
        final String serializedDdiControllerBase = OBJECT_MAPPER.writeValueAsString(ddiControllerBase);
        final DdiControllerBase deserializedDdiControllerBase = OBJECT_MAPPER.readValue(serializedDdiControllerBase, DdiControllerBase.class);
        assertThat(serializedDdiControllerBase).contains(ddiPolling.getSleep());
        assertThat(deserializedDdiControllerBase.getConfig().getPolling().getSleep()).isEqualTo(ddiPolling.getSleep());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiControllerBase = "{\"config\":{\"polling\":{\"sleep\":\"123\"}},\"links\":[],\"unknownProperty\":\"test\"}";

        // Test
        final DdiControllerBase ddiControllerBase = OBJECT_MAPPER.readValue(serializedDdiControllerBase, DdiControllerBase.class);
        assertThat(ddiControllerBase.getConfig().getPolling().getSleep()).isEqualTo("123");
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    void shouldFailForObjectWithWrongDataTypes() {
        // Setup
        final String serializedDdiControllerBase = "{\"config\":{\"polling\":{\"sleep\":[\"123\"]}},\"links\":[],\"unknownProperty\":\"test\"}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiControllerBase, DdiControllerBase.class));
    }
}