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
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiActionHistory'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiActionHistoryTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final String actionStatus = "TestAction";
        final List<String> messages = Arrays.asList("Action status message 1", "Action status message 2");
        final DdiActionHistory ddiActionHistory = new DdiActionHistory(actionStatus, messages);

        // Test
        final String serializedDdiActionHistory = OBJECT_MAPPER.writeValueAsString(ddiActionHistory);
        final DdiActionHistory deserializedDdiActionHistory = OBJECT_MAPPER.readValue(serializedDdiActionHistory,
                DdiActionHistory.class);

        assertThat(serializedDdiActionHistory).contains(actionStatus, messages.get(0), messages.get(1));
        assertThat(deserializedDdiActionHistory.toString()).contains(actionStatus, messages.get(0), messages.get(1));
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiActionHistory = """
                {
                    "status": "SomeAction",
                    "messages": [ "Some message"],
                    "unknownProperty": "test"
                }""";

        // Test
        final DdiActionHistory ddiActionHistory = OBJECT_MAPPER.readValue(serializedDdiActionHistory, DdiActionHistory.class);

        assertThat(ddiActionHistory.toString()).contains("SomeAction", "Some message");
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        final String serializedDdiActionFeedback = """
                {
                    "status": [SomeAction],
                    "messages": ["Some message"]
                }""";

        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> OBJECT_MAPPER.readValue(serializedDdiActionFeedback, DdiActionHistory.class));
    }
}