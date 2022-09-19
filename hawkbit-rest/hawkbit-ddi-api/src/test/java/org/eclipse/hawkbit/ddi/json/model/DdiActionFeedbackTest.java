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
public class DdiActionFeedbackTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final String time = Instant.now().toString();
        final DdiStatus ddiStatus = new DdiStatus(DdiStatus.ExecutionStatus.CLOSED, null, Lists.emptyList());
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
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        final String serializedDdiActionFeedback = "{\"time\":\"20190809T121314\",\"status\":{\"execution\": [closed],\"result\":null,\"details\":[]}}";

        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiActionFeedback, DdiActionFeedback.class));
    }
}
