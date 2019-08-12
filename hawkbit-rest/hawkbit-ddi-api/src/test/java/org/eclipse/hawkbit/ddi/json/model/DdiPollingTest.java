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

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiPolling'
 */
@Feature("Model Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiPollingTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
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
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiPolling = "{\"sleep\":\"10\",\"unknownProperty\":\"test\"}";

        // Test
        DdiPolling ddiPolling = mapper.readValue(serializedDdiPolling, DdiPolling.class);

        assertThat(ddiPolling.getSleep()).isEqualTo("10");
    }

    @Test(expected = com.fasterxml.jackson.databind.exc.MismatchedInputException.class)
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiPolling = "{\"sleep\":[\"10\"]}";

        // Test
        mapper.readValue(serializedDdiPolling, DdiPolling.class);
    }
}