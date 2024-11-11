/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
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
import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

/**
 * Test serializability of DDI api model 'DdiConfirmationBase'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("CHeck JSON serialization of DDI api confirmation models")
class DdiConfirmationBaseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        final String id = "1234";
        final DdiDeployment ddiDeployment = new DdiDeployment(FORCED, ATTEMPT, Collections.emptyList(), AVAILABLE);
        final String actionStatus = "TestAction";
        final DdiActionHistory ddiActionHistory = new DdiActionHistory(actionStatus,
                Arrays.asList("Action status message 1", "Action status message 2"));
        final DdiConfirmationBaseAction ddiConfirmationBaseAction = new DdiConfirmationBaseAction(id, ddiDeployment,
                ddiActionHistory);

        // Test
        String serializedDdiConfirmationBase = OBJECT_MAPPER.writeValueAsString(ddiConfirmationBaseAction);
        final DdiConfirmationBaseAction deserializedDdiConfigurationBase = OBJECT_MAPPER
                .readValue(serializedDdiConfirmationBase, DdiConfirmationBaseAction.class);

        assertThat(serializedDdiConfirmationBase).contains(id, FORCED.getName(), ATTEMPT.getName(),
                AVAILABLE.getStatus(), actionStatus);
        assertThat(deserializedDdiConfigurationBase.getConfirmation().getDownload())
                .isEqualTo(ddiDeployment.getDownload());
        assertThat(deserializedDdiConfigurationBase.getConfirmation().getUpdate()).isEqualTo(ddiDeployment.getUpdate());
        assertThat(deserializedDdiConfigurationBase.getConfirmation().getMaintenanceWindow())
                .isEqualTo(ddiDeployment.getMaintenanceWindow());
        assertThat(deserializedDdiConfigurationBase.getActionHistory().toString())
                .isEqualTo(ddiActionHistory.toString());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiConfirmationBase = "{" +
                "\"id\":\"1234\",\"confirmation\":{\"download\":\"forced\"," +
                "\"update\":\"attempt\",\"maintenanceWindow\":\"available\",\"chunks\":[]}," +
                "\"actionHistory\":{\"status\":\"TestAction\",\"messages\":[\"Action status message 1\"," +
                "\"Action status message 2\"]},\"links\":[],\"unknownProperty\":\"test\"" +
                "}";

        // Test
        final DdiConfirmationBaseAction ddiConfirmationBaseAction = OBJECT_MAPPER.readValue(serializedDdiConfirmationBase,
                DdiConfirmationBaseAction.class);

        assertThat(ddiConfirmationBaseAction.getConfirmation().getDownload().getName()).isEqualTo(FORCED.getName());
        assertThat(ddiConfirmationBaseAction.getConfirmation().getUpdate().getName()).isEqualTo(ATTEMPT.getName());
        assertThat(ddiConfirmationBaseAction.getConfirmation().getMaintenanceWindow().getStatus())
                .isEqualTo(AVAILABLE.getStatus());
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        final String serializedDdiConfirmationBase = "{" +
                "\"id\":[\"1234\"],\"confirmation\":{\"download\":\"forced\"," +
                "\"update\":\"attempt\",\"maintenanceWindow\":\"available\",\"chunks\":[]}," +
                "\"actionHistory\":{\"status\":\"TestAction\",\"messages\":[\"Action status message 1\"," +
                "\"Action status message 2\"]},\"links\":[]" +
                "}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> OBJECT_MAPPER.readValue(serializedDdiConfirmationBase, DdiConfirmationBaseAction.class));
    }
}