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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiConfigData'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiConfigDataTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        Map<String, String> data = new HashMap<>();
        data.put("test", "data");
        DdiConfigData ddiConfigData = new DdiConfigData(data, DdiUpdateMode.REPLACE);

        // Test
        String serializedDdiConfigData = mapper.writeValueAsString(ddiConfigData);
        DdiConfigData deserializedDdiConfigData = mapper.readValue(serializedDdiConfigData, DdiConfigData.class);

        assertThat(serializedDdiConfigData).contains("test", "data");
        assertThat(deserializedDdiConfigData.getMode()).isEqualTo(DdiUpdateMode.REPLACE);

    }

    @Test
    @Description("Verify the correct deserialization of a model with an additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiConfigData = "{\"data\":{\"test\":\"data\"},\"mode\":\"replace\",\"unknownProperty\":\"test\"}";

        // Test
        DdiConfigData ddiConfigData = mapper.readValue(serializedDdiConfigData, DdiConfigData.class);

        assertThat(ddiConfigData.getMode()).isEqualTo(DdiUpdateMode.REPLACE);
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiConfigData = "{\"data\":{\"test\":\"data\"},\"mode\":[\"replace\"],\"unknownProperty\":\"test\"}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiConfigData, DdiConfigData.class));
    }

    @Test
    @Description("Verify the correct deserialization of a model with removed unused status property")
    public void shouldDeserializeObjectWithStatusProperty() throws IOException {
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
        DdiConfigData ddiConfigData = mapper.readValue(serializedDdiConfigData, DdiConfigData.class);

        assertThat(ddiConfigData.getMode()).isEqualTo(DdiUpdateMode.REPLACE);
    }
}