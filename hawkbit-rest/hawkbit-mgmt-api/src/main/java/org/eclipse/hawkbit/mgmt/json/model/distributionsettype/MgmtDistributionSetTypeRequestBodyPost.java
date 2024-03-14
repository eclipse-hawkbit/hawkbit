/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionsettype;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeAssigment;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body for DistributionSetType POST.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MgmtDistributionSetTypeRequestBodyPost extends MgmtDistributionSetTypeRequestBodyPut {

    @JsonProperty(required = true)
    @Schema(description = "The name of the entity", example = "Example type name")
    private String name;

    @JsonProperty(required = true)
    @Schema(description = "Functional key of the distribution set type", example = "Example key")
    private String key;

    @JsonProperty
    @Schema(description = "Mandatory module type IDs")
    private List<MgmtSoftwareModuleTypeAssigment> mandatorymodules;
    @JsonProperty
    @Schema(description = "Optional module type IDs")
    private List<MgmtSoftwareModuleTypeAssigment> optionalmodules;

    @Override
    public MgmtDistributionSetTypeRequestBodyPost setDescription(final String description) {
        super.setDescription(description);
        return this;
    }

    @Override
    public MgmtDistributionSetTypeRequestBodyPost setColour(final String colour) {
        super.setColour(colour);
        return this;
    }
}