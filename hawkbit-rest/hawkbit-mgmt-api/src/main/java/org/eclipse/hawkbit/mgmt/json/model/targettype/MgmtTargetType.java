/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.targettype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.mgmt.json.model.MgmtTypeEntity;

/**
 * A json annotated rest model for TargetType to RESTful API
 * representation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
    **_links**:
    * **compatibledistributionsettypes** - Link to the compatible distribution set types in this target type
    """, example = """
    {
      "createdBy" : "bumlux",
      "createdAt" : 1682408564546,
      "lastModifiedBy" : "bumlux",
      "lastModifiedAt" : 1682408564546,
      "name" : "TargetType",
      "description" : "TargetType description",
      "colour" : "#000000",
      "_links" : {
        "self" : {
          "href" : "https://management-api.host.com/rest/v1/targettypes/8"
        },
        "compatibledistributionsettypes" : {
          "href" : "https://management-api.host.com/rest/v1/targettypes/8/compatibledistributionsettypes"
        }
      },
      "id" : 8
    }""")
public class MgmtTargetType extends MgmtTypeEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(description = "The technical identifier of the entity", example = "26")
    private Long typeId;
}
