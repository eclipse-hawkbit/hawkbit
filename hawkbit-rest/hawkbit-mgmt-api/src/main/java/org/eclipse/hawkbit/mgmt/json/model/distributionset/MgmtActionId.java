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

import java.util.Objects;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representation of an Action Id as a Json Object with link to the Action
 * resource
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtActionId extends RepresentationModel<MgmtActionId> {

    private long actionId;

    public MgmtActionId() {
    }

    /**
     * Constructor
     * 
     * @param actionId
     *            the actionId
     * @param controllerId
     *            the controller Id
     */
    public MgmtActionId(final String controllerId, final long actionId) {
        this.actionId = actionId;
        add(linkTo(methodOn(MgmtTargetRestApi.class).getAction(controllerId, actionId)).withSelfRel().expand());
    }

    @JsonProperty("id")
    public long getActionId() {
        return actionId;
    }

    public void setActionId(final long actionId) {
        this.actionId = actionId;
    }

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj) && this.getClass().isInstance(obj) && actionId == ((MgmtActionId) obj).getActionId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), actionId);
    }
}
