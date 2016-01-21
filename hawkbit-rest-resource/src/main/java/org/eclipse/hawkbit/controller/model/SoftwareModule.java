/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;
import org.springframework.hateoas.Link;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 
 *
 *
 */
@ApiModel(ApiModelProperties.SOFTWARE_MODUL)
public class SoftwareModule {

    @ApiModelProperty(value = ApiModelProperties.SOFTWARE_MODUL_TYPE, required = true)
    @NotNull
    private final Long id;

    @ApiModelProperty(value = ApiModelProperties.SOFTWARE_MODUL_TYPE, required = true)
    @NotNull
    private final String type;

    @ApiModelProperty(value = ApiModelProperties.SOFTWARE_MODULE_VERSION, required = true)
    @NotNull
    private final String version;

    @ApiModelProperty(value = ApiModelProperties.SOFTWARE_MODULE_NAME, required = true)
    @NotNull
    private final String name;

    @ApiModelProperty(value = ApiModelProperties.SOFTWARE_MODULE_ARTIFACT_LINKS)
    private final List<Link> artifactLinks;

    /**
     * Constructor.
     * 
     * @param id
     *            of the software module
     * @param type
     *            of the deployment software module
     * @param version
     *            of the software module
     * @param name
     *            of the software module
     * @param artifactLinks
     *            the links to the artifacts of the software module
     */
    public SoftwareModule(final Long id, final String type, final String version, final String name,
            final List<Link> artifactLinks) {
        this.id = id;
        this.type = type;
        this.version = version;
        this.name = name;
        this.artifactLinks = artifactLinks;
    }

    @JsonProperty("links")
    public List<Link> getArtifactLinks() {
        return artifactLinks;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

}
