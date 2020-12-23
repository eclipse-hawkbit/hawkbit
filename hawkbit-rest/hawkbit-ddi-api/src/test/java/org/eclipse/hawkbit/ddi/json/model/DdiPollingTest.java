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
 * Test serializability of DDI api model 'DdiPolling'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiPollingTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        DdiPolling ddiPolling = new DdiPolling("10");

        // Test
        String serializedDdiPolling = mapper.writeValueAsString(ddiPolling);
        DdiPolling deserializedDdiPolling = mapper.readValue(serializedDdiPolling, DdiPolling.class);

        assertThat(serializedDdiPolling).contains(ddiPolling.getSleep());
        assertThat(deserializedDdiPolling.getSleep()).isEqualTo(ddiPolling.getSleep());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiPolling = "{\"sleep\":\"10\",\"unknownProperty\":\"test\"}";

        // Test
        DdiPolling ddiPolling = mapper.readValue(serializedDdiPolling, DdiPolling.class);

        assertThat(ddiPolling.getSleep()).isEqualTo("10");
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiPolling = "{\"sleep\":[\"10\"]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiPolling, DdiPolling.class));
    }
}