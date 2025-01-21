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

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiResult'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
class DdiResultTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final DdiProgress ddiProgress = new DdiProgress(30, 100);
        final DdiResult ddiResult = new DdiResult(NONE, ddiProgress);

        // Test
        final String serializedDdiResult = OBJECT_MAPPER.writeValueAsString(ddiResult);
        final DdiResult deserializedDdiResult = OBJECT_MAPPER.readValue(serializedDdiResult, DdiResult.class);
        assertThat(serializedDdiResult).contains(NONE.getName(), ddiProgress.getCnt().toString(), ddiProgress.getOf().toString());
        assertThat(deserializedDdiResult.getFinished()).isEqualTo(ddiResult.getFinished());
        assertThat(deserializedDdiResult.getProgress().getCnt()).isEqualTo(ddiProgress.getCnt());
        assertThat(deserializedDdiResult.getProgress().getOf()).isEqualTo(ddiProgress.getOf());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiResult = "{\"finished\":\"none\",\"progress\":{\"cnt\":30,\"of\":100},\"unknownProperty\":\"test\"}";

        // Test
        final DdiResult ddiResult = OBJECT_MAPPER.readValue(serializedDdiResult, DdiResult.class);
        assertThat(ddiResult.getFinished()).isEqualTo(NONE);
        assertThat(ddiResult.getProgress().getCnt()).isEqualTo(30);
        assertThat(ddiResult.getProgress().getOf()).isEqualTo(100);
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    void shouldFailForObjectWithWrongDataTypes() {
        // Setup
        final String serializedDdiResult = "{\"finished\":[\"none\"],\"progress\":{\"cnt\":30,\"of\":100}}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiResult, DdiResult.class));
    }
}