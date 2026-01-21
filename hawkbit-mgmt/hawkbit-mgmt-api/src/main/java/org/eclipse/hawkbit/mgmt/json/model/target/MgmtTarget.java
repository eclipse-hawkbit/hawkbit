/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.MgmtPollStatus;

/**
 * A json annotated rest model for Target to RESTful API representation.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
        **_links**:
        * **assignedDS** - Links to assigned distribution sets
        * **installedDS** - Links to installed distribution sets
        * **attributes** - Links to attributes of the target
        * **actions** - Links to actions of the target
        * **metadata** - List of metadata
        * **targetType** - The link to the target type
        * **autoConfirm** - The link to the detailed auto confirm state
        """, example = """
        {
           "createdBy" : "bumlux",
           "createdAt" : 1682408577979,
           "lastModifiedBy" : "bumlux",
           "lastModifiedAt" : 1682408577988,
           "name" : "137",
           "description" : "My name is 137",
           "controllerId" : "137",
           "updateStatus" : "in_sync",
           "lastControllerRequestAt" : 1682408577978,
           "installedAt" : 1682408577987,
           "ipAddress" : "192.168.0.1",
           "address" : "http://192.168.0.1",
           "pollStatus" : {
             "lastRequestAt" : 1682408577978,
             "nextExpectedRequestAt" : 1682451777978,
             "overdue" : false
           },
           "securityToken" : "949f1c3487125467464a960d750373c1",
           "requestAttributes" : true,
           "targetType" : 13,
           "targetTypeName" : "defaultType",
           "autoConfirmActive" : false,
           "_links" : {
             "self" : {
               "href" : "https://management-api.host.com/rest/v1/targets/137"
             },
             "assignedDS" : {
               "href" : "https://management-api.host.com/rest/v1/targets/137/assignedDS"
             },
             "installedDS" : {
               "href" : "https://management-api.host.com/rest/v1/targets/137/installedDS"
             },
             "attributes" : {
               "href" : "https://management-api.host.com/rest/v1/targets/137/attributes"
             },
             "actions" : {
               "href" : "https://management-api.host.com/rest/v1/targets/137/actions?offset=0&limit=50&sort=id%3ADESC"
             },
             "metadata" : {
               "href" : "https://management-api.host.com/rest/v1/targets/137/metadata?offset=0&limit=50"
             },
             "targetType" : {
               "href" : "https://management-api.host.com/rest/v1/targettypes/13"
             },
             "autoConfirm" : {
               "href" : "https://management-api.host.com/rest/v1/targets/137/autoConfirm"
             }
           }
         }""")
public class MgmtTarget extends MgmtNamedEntity {

    @JsonProperty(required = true)
    @Schema(description = "Controller ID", example = "123")
    private String controllerId;

    @Schema(description = "Target group", example = "Europe/East")
    private String group;

    @Schema(description = "If the target is in sync", example = "in_sync")
    private String updateStatus;

    @Schema(description = "Timestamp of the last controller request", example = "1691065941102")
    private Long lastControllerRequestAt;

    @Schema(description = "Install timestamp", example = "1691065941155")
    private Long installedAt;

    @Schema(description = "Last known IP address of the target. Only presented if IP address of the target " +
            "itself is known (connected directly through DDI API)", example = "192.168.0.1")
    private String ipAddress;

    @Schema(description = "The last known address URI of the target. Includes information of the target is " +
            "connected either directly (DDI) through HTTP or indirectly (DMF) through amqp.",
            example = "http://192.168.0.1")
    private String address;

    @Schema(description = "Poll status")
    private MgmtPollStatus pollStatus;

    @Schema(description = "Pre-Shared key that allows targets to authenticate at Direct Device Integration " +
            "API if enabled in the tenant settings", example = "38e6a19932b014040ba061795186514e")
    @ToString.Exclude // note - it is included only if the received has the needed permissions
    private String securityToken;

    @Schema(description = "Request re-transmission of target attributes", example = "true")
    private boolean requestAttributes;

    @Schema(description = "ID of the target type", example = "19")
    private Long targetType;

    @Schema(description = "Name of the target type", example = "defaultType")
    private String targetTypeName;

    @Schema(description = "Present if user consent flow active. Indicates if auto-confirm is active", example = "false")
    private Boolean autoConfirmActive;
}