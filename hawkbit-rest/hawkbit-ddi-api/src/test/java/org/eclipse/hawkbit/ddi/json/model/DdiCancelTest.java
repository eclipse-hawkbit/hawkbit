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

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiArtifact'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiCancelTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        String ddiCancelId = "1234";
        DdiCancelActionToStop ddiCancelActionToStop = new DdiCancelActionToStop("1234");
        DdiCancel ddiCancel = new DdiCancel(ddiCancelId, ddiCancelActionToStop);

        // Test
        String serializedDdiCancel = mapper.writeValueAsString(ddiCancel);
        DdiCancel deserializedDdiCancel = mapper.readValue(serializedDdiCancel, DdiCancel.class);

        assertThat(serializedDdiCancel).contains(ddiCancelId, ddiCancelActionToStop.getStopId());
        assertThat(deserializedDdiCancel.getId()).isEqualTo(ddiCancelId);
        assertThat(deserializedDdiCancel.getCancelAction().getStopId()).isEqualTo(ddiCancelActionToStop.getStopId());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiCancel = "{\"id\":\"1234\",\"cancelAction\":{\"stopId\":\"1234\"}, \"unknownProperty\": \"test\"}";

        // Test
        DdiCancel ddiCancel = mapper.readValue(serializedDdiCancel, DdiCancel.class);

        assertThat(ddiCancel.getId()).isEqualTo("1234");
        assertThat(ddiCancel.getCancelAction().getStopId()).matches("1234");
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiCancel = "{\"id\":[\"1234\"],\"cancelAction\":{\"stopId\":\"1234\"}}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiCancel, DdiCancel.class));
    }
}