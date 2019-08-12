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
 * Test serializability of DDI api model 'DdiControllerBase'
 */
@Feature("Model Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiControllerBaseTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        // Setup
        DdiPolling ddiPolling = new DdiPolling("10");
        DdiConfig ddiConfig = new DdiConfig(ddiPolling);
        DdiControllerBase ddiControllerBase = new DdiControllerBase(ddiConfig);

        // Test
        String serializedDdiControllerBase = mapper.writeValueAsString(ddiControllerBase);
        DdiControllerBase deserializedDdiControllerBase = mapper.readValue(serializedDdiControllerBase,
                DdiControllerBase.class);

        assertThat(serializedDdiControllerBase).contains(ddiPolling.getSleep());
        assertThat(deserializedDdiControllerBase.getConfig().getPolling().getSleep()).isEqualTo(ddiPolling.getSleep());
    }

    @Test
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiControllerBase = "{\"config\":{\"polling\":{\"sleep\":\"123\"}},\"links\":[],\"unknownProperty\":\"test\"}";

        // Test
        DdiControllerBase ddiControllerBase = mapper.readValue(serializedDdiControllerBase, DdiControllerBase.class);

        assertThat(ddiControllerBase.getConfig().getPolling().getSleep()).isEqualTo("123");
    }

    @Test(expected = com.fasterxml.jackson.databind.exc.MismatchedInputException.class)
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiControllerBase = "{\"config\":{\"polling\":{\"sleep\":[\"123\"]}},\"links\":[],\"unknownProperty\":\"test\"}";

        // Test
        mapper.readValue(serializedDdiControllerBase, DdiControllerBase.class);
    }
}