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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Request Body for DistributionSetType PUT, i.e. update.
 */
@Data
@Accessors(chain = true)
@ToString
public class MgmtDistributionSetTypeRequestBodyPut {

    @JsonProperty
    @Schema(description = "The description of the entity", example = "Example description")
    private String description;

    @JsonProperty
    @Schema(description = "The colour of the entity", example = "rgb(86,37,99)")
    private String colour;
}