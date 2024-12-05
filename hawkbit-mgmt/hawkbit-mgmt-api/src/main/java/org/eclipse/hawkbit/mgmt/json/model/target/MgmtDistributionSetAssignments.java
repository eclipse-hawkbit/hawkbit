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

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NoArgsConstructor;

/**
 * Class to hold multiple distribution set assignments. A JSON object
 * representing a single {@link MgmtDistributionSetAssignment} can be
 * deserialized to an object of this class.
 */
@NoArgsConstructor
@JsonDeserialize(using = MgmtDistributionSetAssignmentsDeserializer.class)
public class MgmtDistributionSetAssignments extends ArrayList<MgmtDistributionSetAssignment> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for an object that contains a single distribution set
     * assignment
     *
     * @param assignment the assignment
     */
    public MgmtDistributionSetAssignments(final MgmtDistributionSetAssignment assignment) {
        add(assignment);
    }

    /**
     * Constructor for an object that contains multiple distribution set
     * assignments
     *
     * @param assignments the assignments
     */
    public MgmtDistributionSetAssignments(final List<MgmtDistributionSetAssignment> assignments) {
        super(assignments);
    }

    @Override
    @JsonIgnore
    public boolean isEmpty() {
        return super.isEmpty();
    }
}