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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

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
        Long id = 123L;
        String time = "20190809T121314";
        DdiStatus ddiStatus = new DdiStatus(DdiStatus.ExecutionStatus.CLOSED,
                new DdiResult(DdiResult.FinalResult.SUCCESS, null), null);
        Map<String, String> data = new HashMap<>();
        data.put("test", "data");
        DdiConfigData ddiConfigData = new DdiConfigData(id, time, ddiStatus, data, DdiUpdateMode.REPLACE);

        // Test
        String serializedDdiConfigData = mapper.writeValueAsString(ddiConfigData);
        DdiConfigData deserializedDdiConfigData = mapper.readValue(serializedDdiConfigData, DdiConfigData.class);

        assertThat(serializedDdiConfigData).contains(id.toString(), time, ddiStatus.getExecution().getName(),
                ddiStatus.getResult().getFinished().getName(), "test", "data");
        assertThat(deserializedDdiConfigData.getId()).isEqualTo(id);
        assertThat(deserializedDdiConfigData.getTime()).isEqualTo(time);
        assertThat(deserializedDdiConfigData.getStatus().getExecution()).isEqualTo(DdiStatus.ExecutionStatus.CLOSED);
        assertThat(deserializedDdiConfigData.getStatus().getResult().getFinished()).isEqualTo(
                DdiResult.FinalResult.SUCCESS);
        assertThat(deserializedDdiConfigData.getMode()).isEqualTo(DdiUpdateMode.REPLACE);

    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiConfigData = "{\"id\":123,\"time\":\"20190809T121314\","
                + "\"status\":{\"execution\":\"closed\",\"result\":{\"finished\":\"success\",\"progress\":null},"
                + "\"details\":[]},\"data\":{\"test\":\"data\"},\"mode\":\"replace\",\"unknownProperty\":\"test\"}";

        // Test
        DdiConfigData ddiConfigData = mapper.readValue(serializedDdiConfigData, DdiConfigData.class);

        assertThat(ddiConfigData.getId()).isEqualTo(123);
        assertThat(ddiConfigData.getTime()).isEqualTo("20190809T121314");
        assertThat(ddiConfigData.getStatus().getExecution()).isEqualTo(DdiStatus.ExecutionStatus.CLOSED);
        assertThat(ddiConfigData.getStatus().getResult().getFinished()).isEqualTo(DdiResult.FinalResult.SUCCESS);
        assertThat(ddiConfigData.getMode()).isEqualTo(DdiUpdateMode.REPLACE);
    }

    @Test(expected = MismatchedInputException.class)
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiConfigData = "{\"id\":[123],\"time\":\"20190809T121314\","
                + "\"status\":{\"execution\":\"closed\",\"result\":{\"finished\":\"success\",\"progress\":null},"
                + "\"details\":[]},\"data\":{\"test\":\"data\"},\"mode\":\"replace\",\"unknownProperty\":\"test\"}";

        // Test
        mapper.readValue(serializedDdiConfigData, DdiConfigData.class);
    }
}