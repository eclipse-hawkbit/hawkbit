/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import java.util.Arrays;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Deserializes a single object or a List of {@link MgmtDistributionSetAssignment}s
 */
public class MgmtDistributionSetAssignmentsDeserializer extends StdDeserializer<MgmtDistributionSetAssignments> {

    public MgmtDistributionSetAssignmentsDeserializer() {
        super(MgmtDistributionSetAssignments.class);
    }

    @Override
    public MgmtDistributionSetAssignments deserialize(final JsonParser jp, final DeserializationContext ctx) {
        final MgmtDistributionSetAssignments assignments = new MgmtDistributionSetAssignments();
        final JsonNode node = jp.readValueAsTree();
        if (node.isArray()) {
            assignments.addAll(Arrays.asList(ctx.readTreeAsValue(node, MgmtDistributionSetAssignment[].class)));
        } else {
            assignments.add(ctx.readTreeAsValue(node, MgmtDistributionSetAssignment.class));
        }
        return assignments;
    }
}