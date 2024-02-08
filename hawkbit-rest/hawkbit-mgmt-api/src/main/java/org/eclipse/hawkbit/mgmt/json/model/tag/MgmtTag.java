/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.tag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for Tag to RESTful API representation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
    **_links**:
    * **assignedDistributionSets** - Links to assigned distribution sets
    """, example = """
    {
      "createdBy" : "bumlux",
      "createdAt" : 1682408561990,
      "lastModifiedBy" : "bumlux",
      "lastModifiedAt" : 1682408561992,
      "name" : "DsTag",
      "description" : "My name is DsTag",
      "colour" : "default",
      "_links" : {
        "self" : {
          "href" : "https://management-api.host.com/rest/v1/distributionsettags/6"
        },
        "assignedDistributionSets" : {
          "href" : "https://management-api.host.com/rest/v1/distributionsettags/6/assigned?offset=0&limit=50"
        }
      },
      "id" : 6
    }""")
public class MgmtTag extends MgmtNamedEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(description = "The technical identifier of the entity", example = "2")
    private Long tagId;

    @JsonProperty
    @Schema(description = "The colour of the entity", example = "red")
    private String colour;
}