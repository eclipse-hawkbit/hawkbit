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
 * Test serializability of DDI api model 'DdiCancelActionToStop'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiCancelActionToStopTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        String stopId = "1234";
        DdiCancelActionToStop ddiCancelActionToStop = new DdiCancelActionToStop(stopId);
        // Test
        String serializedDdiCancelActionToStop = mapper.writeValueAsString(ddiCancelActionToStop);
        DdiCancelActionToStop deserializedDdiCancelActionToStop = mapper.readValue(serializedDdiCancelActionToStop,
                DdiCancelActionToStop.class);

        assertThat(serializedDdiCancelActionToStop).contains(stopId);
        assertThat(deserializedDdiCancelActionToStop.getStopId()).isEqualTo(stopId);
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiCancelActionToStop = "{\"stopId\":\"12345\",\"unknownProperty\":\"test\"}";

        // Test
        DdiCancelActionToStop ddiCancelActionToStop = mapper.readValue(serializedDdiCancelActionToStop,
                DdiCancelActionToStop.class);

        assertThat(ddiCancelActionToStop.getStopId()).contains("12345");
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiCancelActionToStop = "{\"stopId\": [\"12345\"]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiCancelActionToStop, DdiCancelActionToStop.class));
    }
}