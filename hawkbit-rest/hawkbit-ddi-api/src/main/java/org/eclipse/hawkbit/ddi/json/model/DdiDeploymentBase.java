/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import javax.validation.constraints.NotNull;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Update action resource.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "id", "deployment", "actionHistory" })
public class DdiDeploymentBase extends RepresentationModel<DdiDeploymentBase> {

    @JsonProperty("id")
    @NotNull
    private String id;

    @JsonProperty("deployment")
    @NotNull
    private DdiDeployment deployment;

    /**
     * Action history containing current action status and a list of feedback
     * messages received earlier from the controller.
     */
    @JsonProperty("actionHistory")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DdiActionHistory actionHistory;

    /**
     * Constructor.
     */
    public DdiDeploymentBase() {
        // needed for json create.
    }

    /**
     * Constructor.
     *
     * @param id
     *            of the update action
     * @param deployment
     *            details
     * @param actionHistory
     *            containing current action status and a list of feedback
     *            messages received earlier from the controller.
     */
    public DdiDeploymentBase(final String id, final DdiDeployment deployment, final DdiActionHistory actionHistory) {
        this.id = id;
        this.deployment = deployment;
        this.actionHistory = actionHistory;
    }

    public DdiDeployment getDeployment() {
        return deployment;
    }

    /**
     * Returns the action history containing current action status and a list of
     * feedback messages received earlier from the controller.
     *
     * @return {@link DdiActionHistory}
     */
    public DdiActionHistory getActionHistory() {
        return actionHistory;
    }

    @Override
    public String toString() {
        return "DeploymentBase [id=" + id + ", deployment=" + deployment + " actionHistory=" + actionHistory + "]";
    }

}
