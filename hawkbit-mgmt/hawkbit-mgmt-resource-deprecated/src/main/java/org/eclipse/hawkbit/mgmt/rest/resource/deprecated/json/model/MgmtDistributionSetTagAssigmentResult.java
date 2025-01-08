/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.deprecated.json.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;

/**
 * A json annotated rest model for DSAssigmentResult to RESTful API representation.
 *
 * @deprecated since 0.6.0 with deprecation of toggle assignments
 */
@Data
@Accessors(chain = true)
@ToString
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Deprecated(forRemoval = true, since = "0.6.0")
public class MgmtDistributionSetTagAssigmentResult {

    @JsonProperty
    @Schema(description = "Assigned distribution sets")
    private List<MgmtDistributionSet> assignedDistributionSets;

    @JsonProperty
    @Schema(description = "Unassigned distribution sets")
    private List<MgmtDistributionSet> unassignedDistributionSets;
}