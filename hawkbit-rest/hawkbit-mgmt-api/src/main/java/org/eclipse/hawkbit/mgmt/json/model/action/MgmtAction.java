/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.action;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.MgmtBaseEntity;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindow;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for Action to RESTful API representation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtAction extends MgmtBaseEntity {

    /**
     * API definition for action in update mode.
     */
    public static final String ACTION_UPDATE = "update";
    /**
     * API definition for action in canceling.
     */
    public static final String ACTION_CANCEL = "cancel";
    /**
     * API definition for action completed.
     */
    public static final String ACTION_FINISHED = "finished";
    /**
     * API definition for action still active.
     */
    public static final String ACTION_PENDING = "pending";

    @JsonProperty("id")
    @Schema(example = "7")
    private Long actionId;
    @JsonProperty
    @Schema(example = "update")
    private String type;
    @JsonProperty
    @Schema(example = "finished")
    private String status;
    @JsonProperty
    @Schema(example = "finished")
    private String detailStatus;
    @JsonProperty
    @Schema(example = "1691065903238")
    private Long forceTime;
    @JsonProperty(value = "forceType")
    private MgmtActionType actionType;
    @JsonProperty
    @Schema(example = "600")
    private Integer weight;
    @JsonProperty
    @Schema(hidden = true)
    private MgmtMaintenanceWindow maintenanceWindow;
    @JsonProperty
    @Schema(example = "1")
    private Long rollout;
    @JsonProperty
    @Schema(example = "rollout")
    private String rolloutName;
    @JsonProperty
    @Schema(example = "200")
    private Integer lastStatusCode;
}