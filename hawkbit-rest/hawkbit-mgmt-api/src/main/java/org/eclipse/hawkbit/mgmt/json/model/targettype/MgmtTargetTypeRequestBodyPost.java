/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.targettype;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeAssignment;

/**
 * Request Body for TargetType POST.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MgmtTargetTypeRequestBodyPost extends MgmtTargetTypeRequestBodyPut {

    @JsonProperty
    @Schema(description = "Target type key", example = "id.t23")
    private String key;

    @JsonProperty
    @Schema(description = "Array of distribution set types that are compatible to that target type")
    private List<MgmtDistributionSetTypeAssignment> compatibledistributionsettypes;
}