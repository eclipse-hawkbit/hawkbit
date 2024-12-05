/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.springframework.hateoas.RepresentationModel;

/**
 * Representation of an Action Id as a Json Object with link to the Action
 * resource
 */
@NoArgsConstructor
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """
        {
          "id" : 13,
          "_links" : {
             "self" : {
               "href" : "https://management-api.host.com/rest/v1/targets/target2/actions/13"
             }
          }
        }""")
public class MgmtActionId extends RepresentationModel<MgmtActionId> {

    @JsonProperty("id")
    @Schema(description = "ID of the action")
    private long actionId;

    /**
     * Constructor
     *
     * @param actionId the actionId
     * @param controllerId the controller Id
     */
    public MgmtActionId(final String controllerId, final long actionId) {
        this.actionId = actionId;
        add(linkTo(methodOn(MgmtTargetRestApi.class).getAction(controllerId, actionId)).withSelfRel().expand());
    }
}