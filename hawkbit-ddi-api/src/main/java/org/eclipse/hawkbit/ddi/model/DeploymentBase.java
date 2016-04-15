/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.model;

import javax.validation.constraints.NotNull;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link UpdateAction} resource.
 */
public class DeploymentBase extends ResourceSupport {

    @JsonProperty("id")
    @NotNull
    private final String deplyomentId;

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

    @Override
    public String toString() {
        return "DeploymentBase [id=" + deplyomentId + ", deployment=" + deployment + "]";
    }

}
