/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

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
public class MgmtTargetRequestBody {

    @JsonProperty(required = true)
    @Schema(description = "The name of the entity", example = "controllerName")
    private String name;

    @Schema(description = "The description of the entity", example = "Example description of a target")
    private String description;

    @JsonProperty(required = true)
    @Schema(description = "Controller ID", example = "123")
    private String controllerId;

    @Schema(description = "The last known address URI of the target. Includes information of the target is " +
            "connected either directly (DDI) through HTTP or indirectly (DMF) through amqp",
            example = "https://192.168.0.1")
    private String address;

    @Schema(description = "Pre-Shared key that allows targets to authenticate at Direct Device Integration API if " +
            "enabled in the tenant settings", example = "2345678DGGDGFTDzztgf")
    private String securityToken;

    @Schema(description = "Request re-transmission of target attributes", example = "true")
    private Boolean requestAttributes;

    @Schema(description = "ID of the target type", example = "10")
    private Long targetType;
}