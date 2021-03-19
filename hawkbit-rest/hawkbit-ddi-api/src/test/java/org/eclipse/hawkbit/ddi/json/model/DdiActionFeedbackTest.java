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

import org.assertj.core.util.Lists;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiActionFeedback'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiActionFeedbackTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        Long id = 123L;
        String time = "20190809T121314";
        DdiStatus ddiStatus = new DdiStatus(DdiStatus.ExecutionStatus.CLOSED, null, Lists.emptyList());
        DdiActionFeedback ddiActionFeedback = new DdiActionFeedback(id, time, ddiStatus);

        // Test
        String serializedDdiActionFeedback = mapper.writeValueAsString(ddiActionFeedback);
        DdiActionFeedback deserializedDdiActionFeedback = mapper.readValue(serializedDdiActionFeedback,
                DdiActionFeedback.class);

        assertThat(serializedDdiActionFeedback).contains(id.toString(), time);
        assertThat(deserializedDdiActionFeedback.getId()).isEqualTo(id);
        assertThat(deserializedDdiActionFeedback.getTime()).isEqualTo(time);
        assertThat(deserializedDdiActionFeedback.getStatus().toString()).isEqualTo(ddiStatus.toString());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiActionFeedback = "{\"id\":1, \"time\":\"20190809T121314\", \"status\":{\"execution\":\"closed\", \"result\":null, \"details\":[]}, \"unknownProperty\": \"test\"}";

        // Test
        DdiActionFeedback ddiActionFeedback = mapper.readValue(serializedDdiActionFeedback, DdiActionFeedback.class);

        assertThat(ddiActionFeedback.getId()).isEqualTo(1L);
        assertThat(ddiActionFeedback.getTime()).matches("20190809T121314");
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiActionFeedback = "{\"id\": [1],\"time\":\"20190809T121314\",\"status\":{\"execution\":\"closed\",\"result\":null,\"details\":[]}}";

        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiActionFeedback, DdiActionFeedback.class));
    }
}