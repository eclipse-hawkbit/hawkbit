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
import static org.eclipse.hawkbit.ddi.json.model.DdiResult.FinalResult.NONE;
import static org.eclipse.hawkbit.ddi.json.model.DdiStatus.ExecutionStatus.PROCEEDING;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiStatus'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiStatusTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        DdiProgress ddiProgress = new DdiProgress(30, 100);
        DdiResult ddiResult = new DdiResult(NONE, ddiProgress);
        DdiStatus ddiStatus = new DdiStatus(PROCEEDING, ddiResult, Collections.emptyList());

        // Test
        String serializedDdiStatus = mapper.writeValueAsString(ddiStatus);
        DdiStatus deserializedDdiStatus = mapper.readValue(serializedDdiStatus, DdiStatus.class);

        assertThat(serializedDdiStatus).contains(ddiStatus.getExecution().getName(), ddiResult.getFinished().getName(),
                ddiResult.getProgress().getCnt().toString(), ddiResult.getProgress().getOf().toString());
        assertThat(deserializedDdiStatus.getExecution()).isEqualTo(ddiStatus.getExecution());
        assertThat(deserializedDdiStatus.getResult().getFinished()).isEqualTo(ddiStatus.getResult().getFinished());
        assertThat(deserializedDdiStatus.getResult().getProgress().getCnt()).isEqualTo(
                ddiStatus.getResult().getProgress().getCnt());
        assertThat(deserializedDdiStatus.getResult().getProgress().getOf()).isEqualTo(
                ddiStatus.getResult().getProgress().getOf());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiStatus = "{\"execution\":\"proceeding\",\"result\":{\"finished\":\"none\","
                + "\"progress\":{\"cnt\":30,\"of\":100}},\"details\":[],\"unknownProperty\":\"test\"}";

        // Test
        DdiStatus ddiStatus = mapper.readValue(serializedDdiStatus, DdiStatus.class);

        assertThat(ddiStatus.getExecution()).isEqualTo(PROCEEDING);
        assertThat(ddiStatus.getResult().getFinished()).isEqualTo(NONE);
        assertThat(ddiStatus.getResult().getProgress().getCnt()).isEqualTo(30);
        assertThat(ddiStatus.getResult().getProgress().getOf()).isEqualTo(100);
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiStatus = "{\"execution\":[\"proceeding\"],\"result\":{\"finished\":\"none\","
                + "\"progress\":{\"cnt\":30,\"of\":100}},\"details\":[]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiStatus, DdiStatus.class));
    }
}