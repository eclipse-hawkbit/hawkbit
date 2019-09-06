/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Deserializes a single object or a List of
 * {@link MgmtDistributionSetAssignment}s
 */
public class MgmtDistributionSetAssignmentsDeserializer extends StdDeserializer<MgmtDistributionSetAssignments> {
    private static final long serialVersionUID = 1L;

    /**
     * Mandatory constructor
     */
    public MgmtDistributionSetAssignmentsDeserializer() {
        this(null);
    }

    protected MgmtDistributionSetAssignmentsDeserializer(final Class<?> vc) {
        super(vc);
    }
    
    @Override
    public MgmtDistributionSetAssignments deserialize(final JsonParser jp, final DeserializationContext ctx)
            throws IOException {
        final MgmtDistributionSetAssignments assignments = new MgmtDistributionSetAssignments();
        final ObjectCodec codec = jp.getCodec();
        final JsonNode node = codec.readTree(jp);
        if (node.isArray()) {
            assignments.addAll(Arrays.asList(codec.treeToValue(node, MgmtDistributionSetAssignment[].class)));
        } else {
            assignments.add(codec.treeToValue(node, MgmtDistributionSetAssignment.class));
        }
        return assignments;
    }
}
