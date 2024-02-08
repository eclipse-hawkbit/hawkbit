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
@Schema(example = """
    {
      "createdBy" : "bumlux",
      "createdAt" : 1682408575234,
      "lastModifiedBy" : "bumlux",
      "lastModifiedAt" : 1682408575234,
      "name" : "filter1",
      "query" : "name==a",
      "autoAssignDistributionSet" : 16,
      "autoAssignActionType" : null,
      "autoAssignWeight" : null,
      "confirmationRequired" : null,
      "_links" : {
        "self" : {
          "href" : "https://management-api.host.com/rest/v1/targetfilters/2"
        }
      },
      "id" : 2
    }""")
public class MgmtTargetFilterQuery extends MgmtBaseEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(example = "2")
    private Long filterId;

    @JsonProperty
    @Schema(example = "filterName")
    private String name;

    @JsonProperty
    @Schema(example = "name==*")
    private String query;

    @JsonProperty
    @Schema(example = "15")

    private Long autoAssignDistributionSet;

    @JsonProperty
    private MgmtActionType autoAssignActionType;

    @JsonProperty
    @Schema(example = "")
    private Integer autoAssignWeight;

    @JsonProperty
    @Schema(example = "false")
    private Boolean confirmationRequired;
}