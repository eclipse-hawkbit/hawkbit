/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Request Body for SoftwareModuleType POST.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class MgmtSoftwareModuleTypeRequestBodyPost extends MgmtSoftwareModuleTypeRequestBodyPut {

    @JsonProperty(required = true)
    @Schema(example = "Example name")
    private String name;
    @JsonProperty(required = true)
    @Schema(example = "Example key")
    private String key;
    @JsonProperty
    @Schema(example = "1")
    private int maxAssignments;

    @Override
    public MgmtSoftwareModuleTypeRequestBodyPost setDescription(final String description) {
        super.setDescription(description);
        return this;
    }

    @Override
    public MgmtSoftwareModuleTypeRequestBodyPost setColour(final String colour) {
        super.setColour(colour);
        return this;
    }
}