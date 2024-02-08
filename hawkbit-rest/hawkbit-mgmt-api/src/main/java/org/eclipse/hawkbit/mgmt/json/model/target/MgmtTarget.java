/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.MgmtPollStatus;

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
@Schema(example = """
    {
      "createdBy" : "bumlux",
      "createdAt" : 1682408574854,
      "lastModifiedBy" : "bumlux",
      "lastModifiedAt" : 1682408574863,
      "name" : "targetExist",
      "controllerId" : "targetExist",
      "updateStatus" : "pending",
      "securityToken" : "f1e3d34db13038900b7158e62a6582c8",
      "requestAttributes" : true,
      "_links" : {
        "self" : {
          "href" : "https://management-api.host.com/rest/v1/targets/targetExist"
        }
      }
    }""")
public class MgmtTarget extends MgmtNamedEntity {

    @JsonProperty(required = true)
    @Schema(description = "Controller Id", example = "123")
    private String controllerId;
    @JsonProperty
    @Schema(description = "If the target is in sync", example = "in_sync")
    private String updateStatus;
    @JsonProperty
    @Schema(description = "Timestamp of the last controller request", example = "1691065941102")
    private Long lastControllerRequestAt;
    @JsonProperty
    @Schema(description = "Install timestamp", example = "1691065941155")
    private Long installedAt;
    @JsonProperty
    @Schema(description = "Target IP address", example = "192.168.0.1")
    private String ipAddress;
    @JsonProperty
    @Schema(description = "Target address", example = "http://192.168.0.1")
    private String address;
    @JsonProperty
    @Schema(description = "Poll status")
    private MgmtPollStatus pollStatus;
    @JsonProperty
    @Schema(description = "Security token", example = "38e6a19932b014040ba061795186514e")
    @ToString.Exclude
    private String securityToken;
    @JsonProperty
    @Schema(description = "Request attributes", example = "true")
    private boolean requestAttributes;
    @JsonProperty
    @Schema(description = "Target type id", example = "19")
    private Long targetType;
    @JsonProperty
    @Schema(description = "Target type name", example = "defaultType")
    private String targetTypeName;
    @JsonProperty
    @Schema(description = "If the auto confirm is active", example = "false")
    private Boolean autoConfirmActive;
}