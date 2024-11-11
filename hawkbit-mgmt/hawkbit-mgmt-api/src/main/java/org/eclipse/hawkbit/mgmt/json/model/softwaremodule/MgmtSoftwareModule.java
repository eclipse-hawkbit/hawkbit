/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremodule;

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

/**
 * A json annotated rest model for SoftwareModule to RESTful API representation.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
        **_links**:
        * **type** - The software module type of the entity
        * **artifacts** - List of artifacts of given software module
        * **metadata** - List of metadata
        """, example = """
        {
          "createdBy" : "bumlux",
          "createdAt" : 1682408572790,
          "lastModifiedBy" : "bumlux",
          "lastModifiedAt" : 1682408572791,
          "name" : "os",
          "description" : "a description",
          "version" : "1.0",
          "type" : "os",
          "typeName" : "OS",
          "vendor" : "Vendor Limited, California",
          "encrypted" : false,
          "locked" : true,
          "deleted" : false,
          "_links" : {
            "self" : {
              "href" : "https://management-api.host.com/rest/v1/softwaremodules/6"
            },
            "artifacts" : {
              "href" : "https://management-api.host.com/rest/v1/softwaremodules/6/artifacts"
            },
            "type" : {
              "href" : "https://management-api.host.com/rest/v1/softwaremoduletypes/13"
            },
            "metadata" : {
              "href" : "https://management-api.host.com/rest/v1/softwaremodules/6/metadata?offset=0&limit=50"
            }
          },
          "id" : 6
        }""")
public class MgmtSoftwareModule extends MgmtNamedEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(description = "The technical identifier of the entity", example = "6")
    private Long moduleId;

    @JsonProperty(required = true)
    @Schema(description = "Package version", example = "1.0.0")
    private String version;

    @JsonProperty(required = true)
    @Schema(description = "The software module type of the entity", example = "os")
    private String type;

    @Schema(description = "The software module type name of the entity", example = "OS")
    private String typeName;

    @JsonProperty
    @Schema(description = "The software vendor", example = "Vendor Limited, California")
    private String vendor;

    @JsonProperty
    @Schema(description = "If the software module is encrypted", example = "false")
    private boolean encrypted;

    @JsonProperty
    @Schema(description = "If the software module is locked", example = "true")
    private boolean locked;

    @JsonProperty
    @Schema(description = "If the software module is deleted", example = "false")
    private boolean deleted;
}