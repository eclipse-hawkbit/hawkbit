/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Representation of an Action Id as a Json Object with link to the Action resource
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtActionId extends ResourceSupport {

    private long actionId;

    public MgmtActionId() {
    }

    /**
     * Constructor
     * @param actionId
     *              the actionId
     * @param targetId
     *              the controller Id
     */
    public MgmtActionId(final String targetId, final long actionId) {
        this.actionId = actionId;
        add(linkTo(methodOn(MgmtTargetRestApi.class).getAction(targetId, actionId)).withSelfRel());
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
        return super.equals(obj) && actionId == ((MgmtActionId) obj).getActionId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), actionId);
    }
}
