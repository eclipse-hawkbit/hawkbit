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
import java.time.Instant;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serialization of DDI api model 'DdiActionFeedback'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serialization of DDI api Models")
class DdiActionFeedbackTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model with minimal payload")
    void shouldSerializeAndDeserializeObjectWithoutOptionalValues() throws IOException {
        // Setup
        final DdiStatus ddiStatus = new DdiStatus(DdiStatus.ExecutionStatus.CLOSED, null, null, Lists.emptyList());
        final DdiActionFeedback ddiActionFeedback = new DdiActionFeedback(null, ddiStatus);

        // Test
        final String serializedDdiActionFeedback = mapper.writeValueAsString(ddiActionFeedback);
        final DdiActionFeedback deserializedDdiActionFeedback = mapper.readValue(serializedDdiActionFeedback,
                DdiActionFeedback.class);

        assertThat(deserializedDdiActionFeedback.getStatus()).hasToString(ddiStatus.toString());
    }

    @Test
    @Description("Verify the correct serialization and deserialization of the model with all values provided")
    void shouldSerializeAndDeserializeObjectWithOptionalValues() throws IOException {
        // Setup
        final String time = Instant.now().toString();
        final DdiResult ddiResult = new DdiResult(DdiResult.FinalResult.SUCCESS, new DdiProgress(10,10));
        final DdiStatus ddiStatus = new DdiStatus(DdiStatus.ExecutionStatus.CLOSED, ddiResult, 200, Collections.singletonList("myMessage"));
        final DdiActionFeedback ddiActionFeedback = new DdiActionFeedback(time, ddiStatus);

        // Test
        final String serializedDdiActionFeedback = mapper.writeValueAsString(ddiActionFeedback);
        final DdiActionFeedback deserializedDdiActionFeedback = mapper.readValue(serializedDdiActionFeedback,
              DdiActionFeedback.class);

        assertThat(serializedDdiActionFeedback).contains(time);
        assertThat(deserializedDdiActionFeedback.getTime()).isEqualTo(time);
        assertThat(deserializedDdiActionFeedback.getStatus()).hasToString(ddiStatus.toString());
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        final String serializedDdiActionFeedback = "{\"time\":\"20190809T121314\",\"status\":{\"execution\": [closed],\"result\":null,\"details\":[]}}";

        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
              () -> mapper.readValue(serializedDdiActionFeedback, DdiActionFeedback.class));
    }

    @Test
    @Description("Verify that deserialization works if optional fields are not parsed")
    void shouldConvertItWithoutOptionalFieldTime() throws JsonProcessingException {
        // Setup
        final String serializedDdiActionFeedback = "{\n" + //
                "  \"status\" : {\n" + //
                "    \"result\" : {\n" + //
                "      \"finished\" : \"none\"\n" + //
                "    },\n" + //
                "    \"execution\" : \"download\",\n" + //
                "    \"details\" : [ \"Some message\" ]\n" + //
                "  }\n" + //
                "}";//

        assertThat(mapper.readValue(serializedDdiActionFeedback, DdiActionFeedback.class)).satisfies(deserializedDdiActionFeedback ->  {
            assertThat(deserializedDdiActionFeedback.getTime()).isNull();
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
