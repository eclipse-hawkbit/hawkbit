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
import static org.eclipse.hawkbit.ddi.json.model.DdiResult.FinalResult.NONE;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiResult'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiResultTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        DdiProgress ddiProgress = new DdiProgress(30, 100);
        DdiResult ddiResult = new DdiResult(NONE, ddiProgress);

        // Test
        String serializedDdiResult = mapper.writeValueAsString(ddiResult);
        DdiResult deserializedDdiResult = mapper.readValue(serializedDdiResult, DdiResult.class);

        assertThat(serializedDdiResult).contains(NONE.getName(), ddiProgress.getCnt().toString(),
                ddiProgress.getOf().toString());
        assertThat(deserializedDdiResult.getFinished()).isEqualTo(ddiResult.getFinished());
        assertThat(deserializedDdiResult.getProgress().getCnt()).isEqualTo(ddiProgress.getCnt());
        assertThat(deserializedDdiResult.getProgress().getOf()).isEqualTo(ddiProgress.getOf());
    }

    @Test
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiResult = "{\"finished\":\"none\",\"progress\":{\"cnt\":30,\"of\":100},\"unknownProperty\":\"test\"}";

        // Test
        DdiResult ddiResult = mapper.readValue(serializedDdiResult, DdiResult.class);

        assertThat(ddiResult.getFinished()).isEqualTo(NONE);
        assertThat(ddiResult.getProgress().getCnt()).isEqualTo(30);
        assertThat(ddiResult.getProgress().getOf()).isEqualTo(100);
    }

    @Test(expected = MismatchedInputException.class)
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiResult = "{\"finished\":[\"none\"],\"progress\":{\"cnt\":30,\"of\":100}}";

        // Test
        mapper.readValue(serializedDdiResult, DdiResult.class);
    }
}