/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * {@link UpdateAction} ressource.
 *
 *
 *
 *
 *
 */
@ApiModel("Deployment or update action")
public class DeploymentBase extends ResourceSupport {

    @ApiModelProperty(value = ApiModelProperties.ITEM_ID, required = false)
    @JsonProperty("id")
    @NotNull
    private final String deplyomentId;

    @ApiModelProperty(value = ApiModelProperties.DEPLOYMENT, required = false)
    @NotNull
    private final Deployment deployment;

    /**
     * Constructor.
     *
     * @param id
     *            of the {@link UpdateAction}
     * @param deployment
     *            details.
     */
    public DeploymentBase(final String id, final Deployment deployment) {
        deplyomentId = id;
        this.deployment = deployment;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "DeploymentBase [id=" + deplyomentId + ", deployment=" + deployment + "]";
    }

}
