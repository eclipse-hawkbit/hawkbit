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
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiDeploymentBase'
 * <p/>
 * Feature: Unit Tests - Direct Device Integration API<br/>
 * Story: Serializability of DDI api Models
 */
class DdiDeploymentBaseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Verify the correct serialization and deserialization of the model
     */
    @Test
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final String id = "1234";
        final DdiDeployment ddiDeployment = new DdiDeployment(FORCED, ATTEMPT, Collections.emptyList(), AVAILABLE);
        final String actionStatus = "TestAction";
        final DdiActionHistory ddiActionHistory = new DdiActionHistory(
                actionStatus, List.of("Action status message 1", "Action status message 2"));
        final DdiDeploymentBase ddiDeploymentBase = new DdiDeploymentBase(id, ddiDeployment, ddiActionHistory);

        // Test
        final String serializedDdiDeploymentBase = OBJECT_MAPPER.writeValueAsString(ddiDeploymentBase);
        final DdiDeploymentBase deserializedDdiDeploymentBase = OBJECT_MAPPER.readValue(serializedDdiDeploymentBase, DdiDeploymentBase.class);
        assertThat(serializedDdiDeploymentBase).contains(id, FORCED.getName(), ATTEMPT.getName(), AVAILABLE.getStatus(), actionStatus);
        assertThat(deserializedDdiDeploymentBase.getDeployment().getDownload()).isEqualTo(ddiDeployment.getDownload());
        assertThat(deserializedDdiDeploymentBase.getDeployment().getUpdate()).isEqualTo(ddiDeployment.getUpdate());
        assertThat(deserializedDdiDeploymentBase.getDeployment().getMaintenanceWindow()).isEqualTo(ddiDeployment.getMaintenanceWindow());
        assertThat(deserializedDdiDeploymentBase.getActionHistory()).hasToString(ddiActionHistory.toString());
    }

    /**
     * Verify the correct deserialization of a model with a additional unknown property
     */
    @Test
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiDeploymentBase = "{\"id\":\"1234\",\"deployment\":{\"download\":\"forced\"," +
                "\"update\":\"attempt\",\"maintenanceWindow\":\"available\",\"chunks\":[]}," +
                "\"actionHistory\":{\"status\":\"TestAction\",\"messages\":[\"Action status message 1\"," +
                "\"Action status message 2\"]},\"links\":[],\"unknownProperty\":\"test\"}";

        // Test
        final DdiDeploymentBase ddiDeploymentBase = OBJECT_MAPPER.readValue(serializedDdiDeploymentBase, DdiDeploymentBase.class);
        assertThat(ddiDeploymentBase.getDeployment().getDownload().getName()).isEqualTo(FORCED.getName());
        assertThat(ddiDeploymentBase.getDeployment().getUpdate().getName()).isEqualTo(ATTEMPT.getName());
        assertThat(ddiDeploymentBase.getDeployment().getMaintenanceWindow().getStatus()).isEqualTo(AVAILABLE.getStatus());
    }

    /**
     * Verify that deserialization fails for known properties with a wrong datatype
     */
    @Test
    void shouldFailForObjectWithWrongDataTypes() {
        // Setup
        final String serializedDdiDeploymentBase = "{\"id\":[\"1234\"],\"deployment\":{\"download\":\"forced\"," +
                "\"update\":\"attempt\",\"maintenanceWindow\":\"available\",\"chunks\":[]}," +
                "\"actionHistory\":{\"status\":\"TestAction\",\"messages\":[\"Action status message 1\"," +
                "\"Action status message 2\"]},\"links\":[]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiDeploymentBase, DdiDeploymentBase.class));
    }
}