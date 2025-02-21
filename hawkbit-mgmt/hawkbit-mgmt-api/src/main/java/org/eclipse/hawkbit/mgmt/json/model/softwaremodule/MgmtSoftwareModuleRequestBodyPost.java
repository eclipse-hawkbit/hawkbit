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
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Request Body for SoftwareModule POST.
 */
@Data
@Accessors(chain = true)
@ToString
public class MgmtSoftwareModuleRequestBodyPost {

    @JsonProperty(required = true)
    @Schema(example = "SM Name")
    private String name;

    @JsonProperty(required = true)
    @Schema(example = "1.0.0")
    private String version;

    @JsonProperty(required = true)
    @Schema(example = "os")
    private String type;

    @Schema(example = "SM Description")
    private String description;

    @Schema(example = "Vendor Limited, California")
    private String vendor;

    @Schema(example = "false")
    private boolean encrypted;
}