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
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiActionHistory'
  * <p/>
 * Feature: Unit Tests - Direct Device Integration API<br/>
 * Story: Serializability of DDI api Models
 */
class DdiActionHistoryTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Verify the correct serialization and deserialization of the model
     */
    @Test    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final String actionStatus = "TestAction";
        final List<String> messages = Arrays.asList("Action status message 1", "Action status message 2");
        final DdiActionHistory ddiActionHistory = new DdiActionHistory(actionStatus, messages);

        // Test
        final String serializedDdiActionHistory = OBJECT_MAPPER.writeValueAsString(ddiActionHistory);
        final DdiActionHistory deserializedDdiActionHistory = OBJECT_MAPPER.readValue(serializedDdiActionHistory, DdiActionHistory.class);
        assertThat(serializedDdiActionHistory).contains(actionStatus, messages.get(0), messages.get(1));
        assertThat(deserializedDdiActionHistory.toString()).contains(actionStatus, messages.get(0), messages.get(1));
    }

    /**
     * Verify the correct deserialization of a model with a additional unknown property
     */
    @Test    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
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

    /**
     * Verify that deserialization fails for known properties with a wrong datatype
     */
    @Test    void shouldFailForObjectWithWrongDataTypes() {
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