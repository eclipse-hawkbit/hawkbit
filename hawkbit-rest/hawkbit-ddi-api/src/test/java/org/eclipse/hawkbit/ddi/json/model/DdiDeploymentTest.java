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
import static org.eclipse.hawkbit.ddi.json.model.DdiDeployment.DdiMaintenanceWindowStatus.AVAILABLE;
import static org.eclipse.hawkbit.ddi.json.model.DdiDeployment.HandlingType.ATTEMPT;
import static org.eclipse.hawkbit.ddi.json.model.DdiDeployment.HandlingType.FORCED;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiDeployment'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiDeploymentTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        DdiDeployment ddiDeployment = new DdiDeployment(FORCED, ATTEMPT, Collections.emptyList(), AVAILABLE);

        // Test
        String serializedDdiDeployment = mapper.writeValueAsString(ddiDeployment);
        DdiDeployment deserializedDdiDeployment = mapper.readValue(serializedDdiDeployment, DdiDeployment.class);

        assertThat(serializedDdiDeployment).contains(ddiDeployment.getDownload().getName(),
                ddiDeployment.getMaintenanceWindow().getStatus());
        assertThat(deserializedDdiDeployment.getDownload().getName()).isEqualTo(ddiDeployment.getDownload().getName());
        assertThat(deserializedDdiDeployment.getUpdate().getName()).isEqualTo(ddiDeployment.getUpdate().getName());
        assertThat(deserializedDdiDeployment.getMaintenanceWindow().getStatus()).isEqualTo(
                ddiDeployment.getMaintenanceWindow().getStatus());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiDeployment = "{\"download\":\"forced\",\"update\":\"attempt\", "
                + "\"maintenanceWindow\":\"available\",\"chunks\":[],\"unknownProperty\":\"test\"}";

        // Test
        DdiDeployment ddiDeployment = mapper.readValue(serializedDdiDeployment, DdiDeployment.class);

        assertThat(ddiDeployment.getDownload().getName()).isEqualTo(FORCED.getName());
        assertThat(ddiDeployment.getUpdate().getName()).isEqualTo(ATTEMPT.getName());
        assertThat(ddiDeployment.getMaintenanceWindow().getStatus()).isEqualTo(AVAILABLE.getStatus());
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiDeployment = "{\"download\":[\"forced\"],\"update\":\"attempt\", "
                + "\"maintenanceWindow\":\"available\",\"chunks\":[]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiDeployment, DdiDeployment.class));
    }
}