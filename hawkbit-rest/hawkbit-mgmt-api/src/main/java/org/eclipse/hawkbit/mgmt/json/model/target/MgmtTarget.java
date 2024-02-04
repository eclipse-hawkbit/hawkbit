/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import java.net.URI;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.MgmtPollStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for Target to RESTful API representation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTarget extends MgmtNamedEntity {

    @JsonProperty(required = true)
    @Schema(example = "123")
    private String controllerId;
    @JsonProperty
    @Schema(example = "in_sync")
    private String updateStatus;
    @JsonProperty
    @Schema(example = "1691065941102")
    private Long lastControllerRequestAt;
    @JsonProperty
    @Schema(example = "1691065941155")
    private Long installedAt;
    @JsonProperty
    @Schema(example = "192.168.0.1")
    private String ipAddress;
    @JsonProperty
    @Schema(example = "http://192.168.0.1")
    private String address;
    @JsonProperty
    private MgmtPollStatus pollStatus;
    @JsonProperty
    @Schema(example = "38e6a19932b014040ba061795186514e")
    @ToString.Exclude
    private String securityToken;
    @JsonProperty
    @Schema(example = "true")
    private boolean requestAttributes;
    @JsonProperty
    @Schema(example = "19")
    private Long targetType;
    @JsonProperty
    @Schema(example = "defaultType")
    private String targetTypeName;
    @JsonProperty
    @Schema(example = "false")
    private Boolean autoConfirmActive;
}