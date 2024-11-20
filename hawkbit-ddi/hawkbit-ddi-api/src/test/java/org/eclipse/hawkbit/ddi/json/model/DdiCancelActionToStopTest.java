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
 * Test serializability of DDI api model 'DdiCancelActionToStop'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiCancelActionToStopTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final String stopId = "1234";
        final DdiCancelActionToStop ddiCancelActionToStop = new DdiCancelActionToStop(stopId);

        // Test
        final String serializedDdiCancelActionToStop = OBJECT_MAPPER.writeValueAsString(ddiCancelActionToStop);
        final DdiCancelActionToStop deserializedDdiCancelActionToStop = OBJECT_MAPPER.readValue(
                serializedDdiCancelActionToStop, DdiCancelActionToStop.class);
        assertThat(serializedDdiCancelActionToStop).contains(stopId);
        assertThat(deserializedDdiCancelActionToStop.getStopId()).isEqualTo(stopId);
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiCancelActionToStop = "{\"stopId\":\"12345\",\"unknownProperty\":\"test\"}";

        // Test
        final DdiCancelActionToStop ddiCancelActionToStop = OBJECT_MAPPER.readValue(
                serializedDdiCancelActionToStop, DdiCancelActionToStop.class);
        assertThat(ddiCancelActionToStop.getStopId()).contains("12345");
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        final String serializedDdiCancelActionToStop = "{\"stopId\": [\"12345\"]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiCancelActionToStop, DdiCancelActionToStop.class));
    }
}