/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.targetfilter;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Request body for target PUT/POST commands.
 */
@Data
@Accessors(chain = true)
@ToString
public class MgmtTargetFilterQueryRequestBody {

    @JsonProperty(required = true)
    @Schema(example = "filterName")
    private String name;

    @JsonProperty(required = true)
    @Schema(example = "controllerId==example-target-*")
    private String query;
}