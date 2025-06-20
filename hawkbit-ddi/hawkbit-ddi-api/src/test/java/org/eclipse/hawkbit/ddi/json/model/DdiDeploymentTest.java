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
import static org.eclipse.hawkbit.ddi.json.model.DdiDeployment.DdiMaintenanceWindowStatus.AVAILABLE;
import static org.eclipse.hawkbit.ddi.json.model.DdiDeployment.HandlingType.ATTEMPT;
import static org.eclipse.hawkbit.ddi.json.model.DdiDeployment.HandlingType.FORCED;

import java.io.IOException;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiDeployment'
  * <p/>
 * Feature: Unit Tests - Direct Device Integration API<br/>
 * Story: Serializability of DDI api Models
 */
class DdiDeploymentTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Verify the correct serialization and deserialization of the model
     */
    @Test
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final DdiDeployment ddiDeployment = new DdiDeployment(FORCED, ATTEMPT, Collections.emptyList(), AVAILABLE);

        // Test
        final String serializedDdiDeployment = OBJECT_MAPPER.writeValueAsString(ddiDeployment);
        final DdiDeployment deserializedDdiDeployment = OBJECT_MAPPER.readValue(serializedDdiDeployment, DdiDeployment.class);

        assertThat(serializedDdiDeployment).contains(ddiDeployment.getDownload().getName(), ddiDeployment.getMaintenanceWindow().getStatus());
        assertThat(deserializedDdiDeployment.getDownload().getName()).isEqualTo(ddiDeployment.getDownload().getName());
        assertThat(deserializedDdiDeployment.getUpdate().getName()).isEqualTo(ddiDeployment.getUpdate().getName());
        assertThat(deserializedDdiDeployment.getMaintenanceWindow().getStatus()).isEqualTo(ddiDeployment.getMaintenanceWindow().getStatus());
    }

    /**
     * Verify the correct deserialization of a model with a additional unknown property
     */
    @Test
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiDeployment = "{\"download\":\"forced\",\"update\":\"attempt\", " +
                "\"maintenanceWindow\":\"available\",\"chunks\":[],\"unknownProperty\":\"test\"}";

        // Test
        final DdiDeployment ddiDeployment = OBJECT_MAPPER.readValue(serializedDdiDeployment, DdiDeployment.class);
        assertThat(ddiDeployment.getDownload().getName()).isEqualTo(FORCED.getName());
        assertThat(ddiDeployment.getUpdate().getName()).isEqualTo(ATTEMPT.getName());
        assertThat(ddiDeployment.getMaintenanceWindow().getStatus()).isEqualTo(AVAILABLE.getStatus());
    }

    /**
     * Verify that deserialization fails for known properties with a wrong datatype
     */
    @Test
    void shouldFailForObjectWithWrongDataTypes() {
        // Setup
        final String serializedDdiDeployment = "{\"download\":[\"forced\"],\"update\":\"attempt\", " +
                "\"maintenanceWindow\":\"available\",\"chunks\":[]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiDeployment, DdiDeployment.class));
    }
}