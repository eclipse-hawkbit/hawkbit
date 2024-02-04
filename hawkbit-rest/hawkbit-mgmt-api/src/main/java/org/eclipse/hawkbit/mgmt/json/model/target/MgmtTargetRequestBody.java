/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Request body for target PUT/POST commands.
 */
@Data
@Accessors(chain = true)
public class MgmtTargetRequestBody {

    @JsonProperty(required = true)
    @Schema(example = "controllerName")
    private String name;
    @Schema(example = "Example description of a target")
    private String description;
    @JsonProperty(required = true)
    @Schema(example = "12345")
    private String controllerId;
    @JsonProperty
    @Schema(example = "https://192.168.0.1")
    private String address;
    @JsonProperty
    @Schema(example = "2345678DGGDGFTDzztgf")
    private String securityToken;
    @JsonProperty
    @Schema(example = "false")
    private Boolean requestAttributes;
    @JsonProperty
    @Schema(example = "10")
    private Long targetType;
}