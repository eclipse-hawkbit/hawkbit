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
 * Test serializability of DDI api model 'DdiCancelActionToStop'
 */
@Feature("Model Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiCancelActionToStopTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
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
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiCancelActionToStop = "{\"stopId\":\"12345\",\"unknownProperty\":\"test\"}";

        // Test
        DdiCancelActionToStop ddiCancelActionToStop = mapper.readValue(serializedDdiCancelActionToStop,
                DdiCancelActionToStop.class);

        assertThat(ddiCancelActionToStop.getStopId()).contains("12345");
    }

    @Test(expected = com.fasterxml.jackson.databind.exc.MismatchedInputException.class)
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiCancelActionToStop = "{\"stopId\": [\"12345\"]}";

        // Test
        mapper.readValue(serializedDdiCancelActionToStop, DdiCancelActionToStop.class);
    }
}