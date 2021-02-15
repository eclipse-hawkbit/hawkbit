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
import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Story;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Description;

@Story("Retrieve all open action ids")
@Description("Tests for the MgmtTargetAssignmentResponseBody")
public class MgmtTargetAssignmentResponseBodyTest {

    private static final List<Long> ASSIGNED_ACTIONS = Arrays.asList(4L, 5L, 6L);
    private static final int ALREADY_ASSIGNED_COUNT = 3;
    private static final String CONTROLLER_ID = "target";

    @Test
    @Description("Tests that the ActionIds are serialized correctly in MgmtTargetAssignmentResponseBody")
    public void testActionIdsSerialization() throws IOException {
        final MgmtTargetAssignmentResponseBody responseBody = generateResponseBody();
        final ObjectMapper objectMapper = new ObjectMapper();
        final String responseBodyAsString = objectMapper.writeValueAsString(responseBody);
        final JsonNode jsonNode = objectMapper.readTree(responseBodyAsString);

        assertThat(jsonNode.has("assigned")).as("the assigned targets count").isTrue();
        assertThat(jsonNode.get("assigned").isNumber()).as("the assigned targets count").isTrue();
        assertThat(jsonNode.get("assigned").asLong()).as("the assigned targets count")
                .isEqualTo(ASSIGNED_ACTIONS.size());

        assertThat(jsonNode.has("alreadyAssigned")).as("the already assigned targets count").isTrue();
        assertThat(jsonNode.get("alreadyAssigned").isNumber()).as("the already assigned targets count").isTrue();
        assertThat(jsonNode.get("alreadyAssigned").asLong()).as("the already assigned targets count")
                .isEqualTo(ALREADY_ASSIGNED_COUNT);

        assertThat(jsonNode.has("total")).as("the total targets count").isTrue();
        assertThat(jsonNode.get("total").isNumber()).as("the total targets count").isTrue();
        assertThat(jsonNode.get("total").asLong()).as("the total targets count")
                .isEqualTo(ALREADY_ASSIGNED_COUNT + ASSIGNED_ACTIONS.size());

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
        assertThat(ASSIGNED_ACTIONS).as("The expected action ids")
                .contains(jsonNode.get("assignedActions").get(0).get("id").asLong());
    }

    private static MgmtTargetAssignmentResponseBody generateResponseBody() {
        MgmtTargetAssignmentResponseBody response = new MgmtTargetAssignmentResponseBody();
        response.setAssignedActions(
                ASSIGNED_ACTIONS.stream().map(id -> new MgmtActionId(CONTROLLER_ID, id)).collect(Collectors.toList()));
        response.setAlreadyAssigned(ALREADY_ASSIGNED_COUNT);
        return response;
    }
}
