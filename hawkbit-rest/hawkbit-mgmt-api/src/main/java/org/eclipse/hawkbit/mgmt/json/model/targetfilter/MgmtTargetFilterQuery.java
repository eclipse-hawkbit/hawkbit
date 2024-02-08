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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.MgmtBaseEntity;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for Target Filter Queries to RESTful API representation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
    **_links**:
    * **autoAssignDS** - Link to manage the auto assign distribution set
    """, example = """
    {
       "createdBy" : "bumlux",
       "createdAt" : 1682408566380,
       "lastModifiedBy" : "bumlux",
       "lastModifiedAt" : 1682408566385,
       "name" : "filter1",
       "query" : "name==*",
       "autoAssignDistributionSet" : 3,
       "autoAssignActionType" : "forced",
       "autoAssignWeight" : null,
       "confirmationRequired" : null,
       "_links" : {
         "self" : {
           "href" : "https://management-api.host.com/rest/v1/targetfilters/5"
         },
         "autoAssignDS" : {
           "href" : "https://management-api.host.com/rest/v1/targetfilters/5/autoAssignDS"
         }
       },
       "id" : 5
     }""")
public class MgmtTargetFilterQuery extends MgmtBaseEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(description = "The technical identifier of the entity", example = "2")
    private Long filterId;

    @JsonProperty
    @Schema(description = "The name of the entity", example = "filterName")
    private String name;

    @JsonProperty
    @Schema(description = "Target filter query expression", example = "name==*")
    private String query;

    @JsonProperty
    @Schema(example = "15")
    private Long autoAssignDistributionSet;

    @JsonProperty
    @Schema(description = "Auto assign distribution set id")
    private MgmtActionType autoAssignActionType;

    @JsonProperty
    @Schema(description = "Weight of the resulting Actions", example = "600")
    private Integer autoAssignWeight;

    @JsonProperty
    @Schema(description = "(Available with user consent flow active) Defines, if the confirmation is required for an " +
            "action. Confirmation is required per default.", example = "false")
    private Boolean confirmationRequired;
}