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
import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;

/**
 * Test serialization of DDI api model 'DdiActionFeedback'
  * <p/>
 * Feature: Unit Tests - Direct Device Integration API<br/>
 * Story: Serialization of DDI api Models
 */
class DdiActionFeedbackTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Verify the correct serialization and deserialization of the model with minimal payload
     */
    @Test    void shouldSerializeAndDeserializeObjectWithoutOptionalValues() throws IOException {
        // Setup
        final DdiStatus ddiStatus = new DdiStatus(DdiStatus.ExecutionStatus.CLOSED, null, null, Collections.emptyList());
        final DdiActionFeedback ddiActionFeedback = new DdiActionFeedback(ddiStatus);

        // Test
        final String serializedDdiActionFeedback = mapper.writeValueAsString(ddiActionFeedback);
        final DdiActionFeedback deserializedDdiActionFeedback = mapper.readValue(serializedDdiActionFeedback,
                DdiActionFeedback.class);

        assertThat(deserializedDdiActionFeedback.getStatus()).hasToString(ddiStatus.toString());
    }

    /**
     * Verify the correct serialization and deserialization of the model with all values provided
     */
    @Test    void shouldSerializeAndDeserializeObjectWithOptionalValues() throws IOException {
        // Setup
        final Long timestamp = System.currentTimeMillis();
        final DdiResult ddiResult = new DdiResult(DdiResult.FinalResult.SUCCESS, new DdiProgress(10, 10));
        final DdiStatus ddiStatus = new DdiStatus(DdiStatus.ExecutionStatus.CLOSED, ddiResult, 200, Collections.singletonList("myMessage"));
        final DdiActionFeedback ddiActionFeedback = new DdiActionFeedback(ddiStatus, timestamp);

        // Test
        final String serializedDdiActionFeedback = mapper.writeValueAsString(ddiActionFeedback);
        final DdiActionFeedback deserializedDdiActionFeedback = mapper.readValue(serializedDdiActionFeedback, DdiActionFeedback.class);

        assertThat(deserializedDdiActionFeedback.getTimestamp()).isEqualTo(timestamp);
        assertThat(deserializedDdiActionFeedback.getStatus()).hasToString(ddiStatus.toString());
    }

    /**
     * Verify that deserialization fails for known properties with a wrong datatype
     */
    @Test    void shouldFailForObjectWithWrongDataTypes() {
        // Setup
        final String serializedDdiActionFeedback = """
            {
              "timestamp" : "1627997501890",
              "status" : {
                "execution" : "[closed]",
                "result" : null,
                "details" : []
              }
            }
            """;
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiActionFeedback, DdiActionFeedback.class));
    }

    /**
     * Verify that deserialization works if optional fields are not parsed
     */
    @Test    void shouldConvertItWithoutOptionalFieldTimestamp() throws JsonProcessingException {
        // Setup
        final String serializedDdiActionFeedback = """
            {
              "status" : {
                "result" : {
                  "finished" : "none"
                },
                "execution" : "download",
                "details" : [ "Some message" ]
              }
            }
            """;

        assertThat(mapper.readValue(serializedDdiActionFeedback, DdiActionFeedback.class)).satisfies(deserializedDdiActionFeedback -> {
            assertThat(deserializedDdiActionFeedback.getTimestamp()).isNotNull();
            assertThat(deserializedDdiActionFeedback.getStatus()).isNotNull();
            assertThat(deserializedDdiActionFeedback.getStatus().getResult()).isNotNull();
            assertThat(deserializedDdiActionFeedback.getStatus().getResult().getFinished()).isEqualTo(DdiResult.FinalResult.NONE);
            assertThat(deserializedDdiActionFeedback.getStatus().getResult().getProgress()).isNull();
            assertThat(deserializedDdiActionFeedback.getStatus().getCode()).isNull();
            assertThat(deserializedDdiActionFeedback.getStatus().getExecution()).isEqualTo(DdiStatus.ExecutionStatus.DOWNLOAD);
            assertThat(deserializedDdiActionFeedback.getStatus().getDetails()).hasSize(1);
        });
    }
}