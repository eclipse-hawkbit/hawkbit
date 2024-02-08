/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for DistributionSet to RESTful API
 * representation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(example = """
    {
      "createdBy" : "bumlux",
      "createdAt" : 1682408575642,
      "lastModifiedBy" : "bumlux",
      "lastModifiedAt" : 1682408575643,
      "name" : "DS",
      "description" : "Updated Desc: 2LaONizh7WZp0on6XEOZI9AwEYIjj77YZskEmA2LVrKtAOXj9vvqACopEghLMqt6DIWpIahn6XM4jUlRZ1T5SZS2NWMuWHGoFIg1",
      "version" : "1.0",
      "modules" : [ {
        "createdBy" : "bumlux",
        "createdAt" : 1682408575640,
        "lastModifiedBy" : "bumlux",
        "lastModifiedAt" : 1682408575644,
        "name" : "Firmware",
        "description" : "Updated Desc: 2LaONizh7WZp0on6XEOZI9AwEYIjj77YZskEmA2LVrKtAOXj9vvqACopEghLMqt6DIWpIahn6XM4jUlRZ1T5SZS2NWMuWHGoFIg1",
        "version" : "1.0.5",
        "type" : "os",
        "typeName" : "OS",
        "vendor" : "vendor Limited Inc, California",
        "deleted" : false,
        "encrypted" : false,
        "_links" : {
          "self" : {
            "href" : "https://management-api.host.com/rest/v1/softwaremodules/76"
          }
        },
        "id" : 76
    }""")
public class MgmtDistributionSet extends MgmtNamedEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(description = "The technical identifier of the entity", example = "51")
    private Long dsId;

    @JsonProperty
    @Schema(description = "Package version", example = "1.4.2")
    private String version;

    @JsonProperty
    @Schema(description = """
        True if DS is a required migration step for another DS. As a result the DS’s assignment will not be cancelled
        when another DS is assigned (note: updatable only if DS is not yet assigned to a target)""", example = "false")
    private boolean requiredMigrationStep;

    @JsonProperty
    @Schema(description = "The type of the distribution set", example = "test_default_ds_type")
    private String type;

    @JsonProperty
    @Schema(description = "The type name of the distribution set",
            example = "OS (FW) mandatory, runtime (FW) and app (SW) optional")
    private String typeName;

    @JsonProperty
    @Schema(description = """
        True of the distribution set software module setup is complete as defined by the
        distribution set type""", example = "true")
    private Boolean complete;

    @JsonProperty
    @Schema(description = "Deleted flag, used for soft deleted entities", example = "false")
    private boolean deleted;

    @JsonProperty
    @Schema(description = "True by default and false after the distribution set is invalidated by the user",
            example = "true")
    private boolean valid;

    @JsonProperty
    private List<MgmtSoftwareModule> modules = new ArrayList<>();
}