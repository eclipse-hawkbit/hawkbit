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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiActionHistory'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiActionHistoryTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
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
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiActionHistory = "{\"status\":\"SomeAction\", \"messages\":[\"Some message\"], \"unknownProperty\": \"test\"}";

        // Test
        DdiActionHistory ddiActionHistory = mapper.readValue(serializedDdiActionHistory, DdiActionHistory.class);

        assertThat(ddiActionHistory.toString()).contains("SomeAction", "Some message");
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiActionFeedback = "{\"status\": [SomeAction], \"messages\": [\"Some message\"]}";

        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiActionFeedback, DdiActionHistory.class));
    }
}