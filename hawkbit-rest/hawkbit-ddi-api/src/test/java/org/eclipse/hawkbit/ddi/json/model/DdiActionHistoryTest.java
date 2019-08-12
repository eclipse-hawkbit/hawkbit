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
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiActionHistory'
 */
@Feature("Model Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiActionHistoryTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        String actionStatus = "TestAction";
        List<String> messages = Arrays.asList("Action status message 1", "Action status message 2");
        DdiActionHistory ddiActionHistory = new DdiActionHistory(actionStatus, messages);

        // Test
        String serializedDdiActionHistory = mapper.writeValueAsString(ddiActionHistory);
        DdiActionHistory deserializedDdiActionHistory = mapper.readValue(serializedDdiActionHistory,
                DdiActionHistory.class);

        assertThat(serializedDdiActionHistory).contains(actionStatus, messages.get(0), messages.get(1));
        assertThat(deserializedDdiActionHistory.toString()).contains(actionStatus, messages.get(0), messages.get(1));
    }

    @Test
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiActionHistory = "{\"status\":\"SomeAction\", \"messages\":[\"Some message\"], \"unknownProperty\": \"test\"}";

        // Test
        DdiActionHistory ddiActionHistory = mapper.readValue(serializedDdiActionHistory, DdiActionHistory.class);

        assertThat(ddiActionHistory.toString()).contains("SomeAction", "Some message");
    }

    @Test(expected = com.fasterxml.jackson.databind.exc.MismatchedInputException.class)
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiActionFeedback = "{\"status\": [SomeAction], \"messages\": [\"Some message\"]}";

        // Test
        mapper.readValue(serializedDdiActionFeedback, DdiActionHistory.class);
    }
}