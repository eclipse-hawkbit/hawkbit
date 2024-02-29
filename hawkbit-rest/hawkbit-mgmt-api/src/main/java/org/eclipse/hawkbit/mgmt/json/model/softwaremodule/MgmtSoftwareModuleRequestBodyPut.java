/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremodule;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Request Body for SoftwareModule PUT.
 */
@Data
@Accessors(chain = true)
public class MgmtSoftwareModuleRequestBodyPut {

    @JsonProperty
    @Schema(example = "SM Description")
    private String description;

    @JsonProperty
    @Schema(example = "SM Vendor Name")
    private String vendor;

    @JsonProperty
    @Schema(description = "Put it to true only if want to lock the software module. Otherwise skip it. " +
            "Shall not be false!",
            example = "true")
    private Boolean locked;
}