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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiConfigData'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
class DdiConfigDataTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final Map<String, String> data = new HashMap<>();
        data.put("test", "data");
        final DdiConfigData ddiConfigData = new DdiConfigData(data, DdiUpdateMode.REPLACE);

        // Test
        final String serializedDdiConfigData = OBJECT_MAPPER.writeValueAsString(ddiConfigData);
        final DdiConfigData deserializedDdiConfigData = OBJECT_MAPPER.readValue(serializedDdiConfigData, DdiConfigData.class);
        assertThat(serializedDdiConfigData).contains("test", "data");
        assertThat(deserializedDdiConfigData.getMode()).isEqualTo(DdiUpdateMode.REPLACE);
    }

    @Test
    @Description("Verify the correct deserialization of a model with an additional unknown property")
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiConfigData = "{\"data\":{\"test\":\"data\"},\"mode\":\"replace\",\"unknownProperty\":\"test\"}";

        // Test
        final DdiConfigData ddiConfigData = OBJECT_MAPPER.readValue(serializedDdiConfigData, DdiConfigData.class);
        assertThat(ddiConfigData.getMode()).isEqualTo(DdiUpdateMode.REPLACE);
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    void shouldFailForObjectWithWrongDataTypes() {
        // Setup
        final String serializedDdiConfigData = "{\"data\":{\"test\":\"data\"},\"mode\":[\"replace\"],\"unknownProperty\":\"test\"}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiConfigData, DdiConfigData.class));
    }

    @Test
    @Description("Verify the correct deserialization of a model with removed unused status property")
    void shouldDeserializeObjectWithStatusProperty() throws IOException {
        // We formerly falsely required a 'status' property object when using the
        // configData endpoint. It was removed as a requirement from code and
        // documentation, as it was unused. This test ensures we still behave correctly
        // (and just ignore the 'status' property) if it is still provided by the
        // client.

        // Setup
        String serializedDdiConfigData = "{\"id\":123,\"time\":\"20190809T121314\","
                + "\"status\":{\"execution\":\"closed\",\"result\":{\"finished\":\"success\",\"progress\":null},"
                + "\"details\":[]},\"data\":{\"test\":\"data\"},\"mode\":\"replace\"}";

        // Test
        DdiConfigData ddiConfigData = OBJECT_MAPPER.readValue(serializedDdiConfigData, DdiConfigData.class);

        assertThat(ddiConfigData.getMode()).isEqualTo(DdiUpdateMode.REPLACE);
    }
}