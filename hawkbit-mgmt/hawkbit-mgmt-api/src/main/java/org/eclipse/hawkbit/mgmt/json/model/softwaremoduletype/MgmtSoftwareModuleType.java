/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.MgmtTypeEntity;

/**
 * A json annotated rest model for SoftwareModuleType to RESTful API
 * representation.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(example = """
        {
          "createdBy" : "system",
          "createdAt" : 1682408579390,
          "lastModifiedBy" : "bumlux",
          "lastModifiedAt" : 1682408579394,
          "name" : "Application",
          "description" : "Updated description.",
          "key" : "application",
          "maxAssignments" : 2147483647,
          "deleted" : false,
          "_links" : {
            "self" : {
              "href" : "https://management-api.host.com/rest/v1/softwaremoduletypes/4"
            }
          },
          "id" : 4
        }""")
public class MgmtSoftwareModuleType extends MgmtTypeEntity {

    @JsonProperty(required = true)
    @Schema(description = "The technical identifier of the entity", example = "83")
    private Long id;

    @Schema(description = "Software modules of that type can be assigned at this maximum number " +
            "(e.g. operating system only once)", example = "1")
    private int maxAssignments;
}