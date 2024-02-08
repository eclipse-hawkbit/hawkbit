/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionsettype;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.MgmtTypeEntity;

/**
 * A json annotated rest model for SoftwareModuleType to RESTful API
 * representation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        description = """
    **_links**:
    * **mandatorymodules** - Link to mandatory software modules types in this distribution set type
    * **optionalmodules** - Link to optional software modules types in this distribution set type
    """, example = """
     {
       "createdBy" : "bumlux",
       "createdAt" : 1682408579418,
       "lastModifiedBy" : "bumlux",
       "lastModifiedAt" : 1682408579459,
       "name" : "OS (FW) mandatory, runtime (FW) and app (SW) optional",
       "description" : "Desc1234",
       "key" : "test_default_ds_type",
       "deleted" : false,
       "colour" : "rgb(86,37,99)",
       "_links" : {
         "self" : {
           "href" : "https://management-api.host.com/rest/v1/distributionsettypes/14"
         },
         "mandatorymodules" : {
           "href" : "https://management-api.host.com/rest/v1/distributionsettypes/14/mandatorymoduletypes"
         },
         "optionalmodules" : {
           "href" : "https://management-api.host.com/rest/v1/distributionsettypes/14/optionalmoduletypes"
         }
       },
       "id" : 14
     }""")
public class MgmtDistributionSetType extends MgmtTypeEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(description = "The technical identifier of the entity", example = "99")
    private Long moduleId;
}