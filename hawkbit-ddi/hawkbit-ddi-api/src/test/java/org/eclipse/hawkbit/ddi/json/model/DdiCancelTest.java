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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiArtifact'
 * <p/>
 * Feature: Unit Tests - Direct Device Integration API<br/>
 * Story: Serializability of DDI api Models
 */
class DdiCancelTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Verify the correct serialization and deserialization of the model
     */
    @Test
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final String ddiCancelId = "1234";
        final DdiCancelActionToStop ddiCancelActionToStop = new DdiCancelActionToStop("1234");
        final DdiCancel ddiCancel = new DdiCancel(ddiCancelId, ddiCancelActionToStop);

        // Test
        final String serializedDdiCancel = OBJECT_MAPPER.writeValueAsString(ddiCancel);
        final DdiCancel deserializedDdiCancel = OBJECT_MAPPER.readValue(serializedDdiCancel, DdiCancel.class);
        assertThat(serializedDdiCancel).contains(ddiCancelId, ddiCancelActionToStop.getStopId());
        assertThat(deserializedDdiCancel.getId()).isEqualTo(ddiCancelId);
        assertThat(deserializedDdiCancel.getCancelAction().getStopId()).isEqualTo(ddiCancelActionToStop.getStopId());
    }

    /**
     * Verify the correct deserialization of a model with a additional unknown property
     */
    @Test
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiCancel = "{\"id\":\"1234\",\"cancelAction\":{\"stopId\":\"1234\"}, \"unknownProperty\": \"test\"}";

        // Test
        final DdiCancel ddiCancel = OBJECT_MAPPER.readValue(serializedDdiCancel, DdiCancel.class);
        assertThat(ddiCancel.getId()).isEqualTo("1234");
        assertThat(ddiCancel.getCancelAction().getStopId()).matches("1234");
    }

    /**
     * Verify that deserialization fails for known properties with a wrong datatype
     */
    @Test
    void shouldFailForObjectWithWrongDataTypes() {
        // Setup
        final String serializedDdiCancel = "{\"id\":[\"1234\"],\"cancelAction\":{\"stopId\":\"1234\"}}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiCancel, DdiCancel.class));
    }
}