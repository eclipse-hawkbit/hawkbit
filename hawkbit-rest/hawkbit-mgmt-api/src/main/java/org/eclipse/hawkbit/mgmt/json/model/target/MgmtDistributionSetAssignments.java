/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = MgmtDistributionSetAssignmentsDeserializer.class)
public class MgmtDistributionSetAssignments extends ArrayList<MgmtDistributionSetAssignment> {
    private static final long serialVersionUID = 1L;

    public MgmtDistributionSetAssignments() {
        super();
    }

    public MgmtDistributionSetAssignments(final MgmtDistributionSetAssignment assignment) {
        super();
        add(assignment);
    }

    public MgmtDistributionSetAssignments(final List<MgmtDistributionSetAssignment> assignments) {
        super(assignments);
    }

}
