/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MgmtTargetAssignmentResponseBodyTest {

    @Test
    public void testActionIdsSerialization() throws IOException {
        final MgmtTargetAssignmentResponseBody responseBody = generateResponseBody();
        final ObjectMapper objectMapper = new ObjectMapper();
        final String responseBodyAsString = objectMapper.writeValueAsString(responseBody);
        final JsonNode jsonNode = objectMapper.readTree(responseBodyAsString);

        assertTrue(jsonNode.has("assigned"));
        assertTrue(jsonNode.get("assigned").isNumber());
        assertEquals(3, jsonNode.get("assigned").asLong());

        assertTrue(jsonNode.has("alreadyAssigned"));
        assertTrue(jsonNode.get("alreadyAssigned").isNumber());
        assertEquals(3, jsonNode.get("alreadyAssigned").asLong());

        assertTrue(jsonNode.has("total"));
        assertTrue(jsonNode.get("total").isNumber());
        assertEquals(6, jsonNode.get("total").asLong());

        assertTrue(jsonNode.has("assignedActions"));
        assertTrue(jsonNode.get("assignedActions").isArray());
        assertEquals(3, jsonNode.get("assignedActions").size());
        assertTrue(jsonNode.get("assignedActions").get(0).isObject());
        assertTrue(jsonNode.get("assignedActions").get(0).has("id"));
        assertTrue(jsonNode.get("assignedActions").get(0).get("id").isNumber());
        assertTrue(Arrays.asList(4L, 5L, 6L).contains(jsonNode.get("assignedActions").get(0).get("id").asLong()));

        assertTrue(jsonNode.has("alreadyAssignedActions"));
        assertTrue(jsonNode.get("alreadyAssignedActions").isArray());
        assertEquals(3, jsonNode.get("alreadyAssignedActions").size());
        assertTrue(jsonNode.get("alreadyAssignedActions").get(0).isObject());
        assertTrue(jsonNode.get("alreadyAssignedActions").get(0).has("id"));
        assertTrue(jsonNode.get("alreadyAssignedActions").get(0).get("id").isNumber());
        assertTrue(
                Arrays.asList(1L, 2L, 3L).contains(jsonNode.get("alreadyAssignedActions").get(0).get("id").asLong()));
    }

    private MgmtTargetAssignmentResponseBody generateResponseBody() {
        MgmtTargetAssignmentResponseBody response = new MgmtTargetAssignmentResponseBody();
        response.setAlreadyAssignedActions(
                Arrays.asList(new MgmtActionId(1L), new MgmtActionId(2L), new MgmtActionId(3L)));
        response.setAssignedActions(Arrays.asList(new MgmtActionId(4L), new MgmtActionId(5L), new MgmtActionId(6L)));
        response.setAssigned(3);
        response.setAlreadyAssigned(3);
        return response;
    }
}
