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
import static org.eclipse.hawkbit.ddi.json.model.DdiResult.FinalResult.NONE;
import static org.eclipse.hawkbit.ddi.json.model.DdiStatus.ExecutionStatus.PROCEEDING;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test serializability of DDI api model 'DdiStatus'
 * <p/>
 * Feature: Unit Tests - Direct Device Integration API<br/>
 * Story: Serializability of DDI api Models
 */
class DdiStatusTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Verify the correct serialization and deserialization of the model
     */
    @ParameterizedTest
    @MethodSource("ddiStatusPossibilities")
    void shouldSerializeAndDeserializeObject(final DdiResult ddiResult, final DdiStatus ddiStatus) throws IOException {
        // Test
        final String serializedDdiStatus = OBJECT_MAPPER.writeValueAsString(ddiStatus);
        final DdiStatus deserializedDdiStatus = OBJECT_MAPPER.readValue(serializedDdiStatus, DdiStatus.class);
        assertThat(serializedDdiStatus).contains(ddiStatus.getExecution().getName(), ddiResult.getFinished().getName(),
                ddiResult.getProgress().getCnt().toString(), ddiResult.getProgress().getOf().toString());
        assertThat(deserializedDdiStatus.getExecution()).isEqualTo(ddiStatus.getExecution());
        assertThat(deserializedDdiStatus.getResult().getFinished()).isEqualTo(ddiStatus.getResult().getFinished());
        assertThat(deserializedDdiStatus.getResult().getProgress().getCnt()).isEqualTo(ddiStatus.getResult().getProgress().getCnt());
        assertThat(deserializedDdiStatus.getResult().getProgress().getOf()).isEqualTo(ddiStatus.getResult().getProgress().getOf());
        assertThat(deserializedDdiStatus.getDetails()).isEqualTo(ddiStatus.getDetails());
    }

    /**
     * Verify the correct deserialization of a model with a additional unknown property
     */
    @Test
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiStatus = "{\"execution\":\"proceeding\",\"result\":{\"finished\":\"none\"," +
                "\"progress\":{\"cnt\":30,\"of\":100}},\"details\":[],\"unknownProperty\":\"test\"}";

        // Test
        final DdiStatus ddiStatus = OBJECT_MAPPER.readValue(serializedDdiStatus, DdiStatus.class);
        assertThat(ddiStatus.getExecution()).isEqualTo(PROCEEDING);
        assertThat(ddiStatus.getCode()).isNull();
        assertThat(ddiStatus.getResult().getFinished()).isEqualTo(NONE);
        assertThat(ddiStatus.getResult().getProgress().getCnt()).isEqualTo(30);
        assertThat(ddiStatus.getResult().getProgress().getOf()).isEqualTo(100);
    }

    /**
     * Verify the correct deserialization of a model with a provided code (optional)
     */
    @Test
    void shouldDeserializeObjectWithOptionalCode() throws IOException {
        // Setup
        final String serializedDdiStatus = "{\"execution\":\"proceeding\",\"result\":{\"finished\":\"none\"," +
                "\"progress\":{\"cnt\":30,\"of\":100}},\"code\": 12,\"details\":[]}";

        // Test
        final DdiStatus ddiStatus = OBJECT_MAPPER.readValue(serializedDdiStatus, DdiStatus.class);
        assertThat(ddiStatus.getExecution()).isEqualTo(PROCEEDING);
        assertThat(ddiStatus.getCode()).isEqualTo(12);
        assertThat(ddiStatus.getResult().getFinished()).isEqualTo(NONE);
        assertThat(ddiStatus.getResult().getProgress().getCnt()).isEqualTo(30);
        assertThat(ddiStatus.getResult().getProgress().getOf()).isEqualTo(100);
    }

    /**
     * Verify that deserialization fails for known properties with a wrong datatype
     */
    @Test
    void shouldFailForObjectWithWrongDataTypes() {
        // Setup
        final String serializedDdiStatus = "{\"execution\":[\"proceeding\"],\"result\":{\"finished\":\"none\"," +
                "\"progress\":{\"cnt\":30,\"of\":100}},\"details\":[]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiStatus, DdiStatus.class));
    }

    private static Stream<Arguments> ddiStatusPossibilities() {
        final DdiProgress ddiProgress = new DdiProgress(30, 100);
        final DdiResult ddiResult = new DdiResult(NONE, ddiProgress);
        return Stream.of(
                Arguments.of(ddiResult, new DdiStatus(PROCEEDING, ddiResult, null, Collections.emptyList())),
                Arguments.of(ddiResult, new DdiStatus(PROCEEDING, ddiResult, null, Collections.singletonList("testMessage"))),
                Arguments.of(ddiResult, new DdiStatus(PROCEEDING, ddiResult, 12, Collections.emptyList())));
    }
}