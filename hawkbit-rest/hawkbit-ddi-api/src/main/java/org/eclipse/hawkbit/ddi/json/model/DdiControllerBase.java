/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link DdiControllerBase} resource content.
 */
@NoArgsConstructor // needed for json deserialization
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
    **_links**: Actions that the server has for the target
    * **deploymentBase** - Detailed deployment operation
    * **installedBase** - Detailed operation of last successfully finished action
    * **configData** - Link which is provided whenever the provisioning target or device is supposed to push its configuration data (aka. "controller attributes") to the server. Only shown for the initial configuration, after a successful update action, or if requested explicitly (e.g. via the management UI)    
    """,
        example =  """
    {
      "config" : {
        "polling" : {
          "sleep" : "12:00:00"
        }
      },
      "_links" : {
        "deploymentBase" : {
          "href" : "https://management-api.host.com/TENANT_ID/controller/v1/CONTROLLER_ID/deploymentBase/5?c=-2127183556"
        },
        "installedBase" : {
          "href" : "https://management-api.host.com/TENANT_ID/controller/v1/CONTROLLER_ID/installedBase/4"
        },
        "configData" : {
          "href" : "https://management-api.host.com/TENANT_ID/controller/v1/CONTROLLER_ID/configData"
        }
      }
    }""")
public class DdiControllerBase extends RepresentationModel<DdiControllerBase> {

    @JsonProperty
    private DdiConfig config;

    /**
     * Constructor.
     *
     * @param config
     *            configuration of the SP target
     */
    public DdiControllerBase(final DdiConfig config) {
        this.config = config;
    }
}