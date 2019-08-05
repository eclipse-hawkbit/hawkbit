/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;

import io.qameta.allure.Story;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Description;

@Story("Retrieve all open action ids")
@Description("Tests for the MgmtTargetAssignmentResponseBody")
public class MgmtTargetAssignmentResponseBodyTest {

    @Test
    @Description("Tests that the ActionIds are serialized correctly in MgmtTargetAssignmentResponseBody")
    public void testActionIdsSerialization() throws IOException {
        final MgmtTargetAssignmentResponseBody responseBody = generateResponseBody();
        final ObjectMapper objectMapper = new ObjectMapper();
        final String responseBodyAsString = objectMapper.writeValueAsString(responseBody);
        final JsonNode jsonNode = objectMapper.readTree(responseBodyAsString);

        assertThat(jsonNode.has("assigned")).as("the assigned targets count").isTrue();
        assertThat(jsonNode.get("assigned").isNumber()).as("the assigned targets count").isTrue();
        assertThat(jsonNode.get("assigned").asLong()).as("the assigned targets count").isEqualTo(3);

        assertThat(jsonNode.has("alreadyAssigned")).as("the already assigned targets count").isTrue();
        assertThat(jsonNode.get("alreadyAssigned").isNumber()).as("the already assigned targets count").isTrue();
        assertThat(jsonNode.get("alreadyAssigned").asLong()).as("the already assigned targets count").isEqualTo(3);

        assertThat(jsonNode.has("total")).as("the total targets count").isTrue();
        assertThat(jsonNode.get("total").isNumber()).as("the total targets count").isTrue();
        assertThat(jsonNode.get("total").asLong()).as("the total targets count").isEqualTo(6);

        assertThat(jsonNode.has("assignedActions")).as("The created actions in result of this assignment").isTrue();
        assertThat(jsonNode.get("assignedActions").isArray()).as("The created actions in result of this assignment")
                .isTrue();
        assertThat(jsonNode.get("assignedActions").size()).as("The created actions in result of this assignment")
                .isEqualTo(3);

        assertThat(jsonNode.get("assignedActions").get(0).isObject())
                .as("A created action in result of this assignment").isTrue();
        assertThat(jsonNode.get("assignedActions").get(0).has("id")).as("A created action in result of this assignment")
                .isTrue();
        assertThat(jsonNode.get("assignedActions").get(0).get("id").isNumber())
                .as("A created action in result of this assignment").isTrue();
        assertThat(Arrays.asList(4L, 5L, 6L)).as("The expected action ids")
                .contains(jsonNode.get("assignedActions").get(0).get("id").asLong());
    }

    private static MgmtTargetAssignmentResponseBody generateResponseBody() {
        MgmtTargetAssignmentResponseBody response = new MgmtTargetAssignmentResponseBody();
        final String targetId = "target";
        response.setAssignedActions(Arrays.asList(new MgmtActionId(targetId, 4L), new MgmtActionId(targetId, 5L),
                new MgmtActionId(targetId, 6L)));
        response.setAssigned(3);
        response.setAlreadyAssigned(3);
        return response;
    }
}
