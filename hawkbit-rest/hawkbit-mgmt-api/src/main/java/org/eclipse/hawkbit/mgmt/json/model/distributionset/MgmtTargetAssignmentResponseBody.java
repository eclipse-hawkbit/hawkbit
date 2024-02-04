/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response Body of Target for assignment operations.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetAssignmentResponseBody extends RepresentationModel<MgmtTargetAssignmentResponseBody> {

    private int alreadyAssigned;
    private List<MgmtActionId> assignedActions;

    /**
     * @return the count of assigned targets
     */
    @JsonProperty("assigned")
    public int getAssigned() {
        return assignedActions == null ? 0 : assignedActions.size();
    }

    /**
     * @return the total
     */
    @JsonProperty("total")
    public int getTotal() {
        return getAssigned() + alreadyAssigned;
    }
}