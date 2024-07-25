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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Request Body for TargetType PUT.
 */
@Data
@Accessors(chain = true)
@ToString
public class MgmtTargetTypeRequestBodyPut {

    @JsonProperty(required = true)
    @Schema(description = "The name of the entity", example = "updatedTypeName")
    private String name;

    @JsonProperty
    @Schema(description = "The description of the entity", example = "an updated description")
    private String description;

    @JsonProperty
    @Schema(description = "The colour of the entity", example = "#aaafff")
    private String colour;
}
