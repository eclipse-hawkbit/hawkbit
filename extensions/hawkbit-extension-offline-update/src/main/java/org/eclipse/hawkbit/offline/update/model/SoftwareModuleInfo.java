/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JSON annotated REST model that represents a software module for the
 * {@link OfflineUpdateData}.
 *
 * Note : This object follows a similar pattern as
 * {@link MgmtSoftwareModuleRequestBodyPost}, however it also adds the details
 * pertaining to all the {@link Artifact}s for a software module.
 */
@JsonInclude(Include.NON_NULL)
public class SoftwareModuleInfo {

    @JsonIgnore
    private Long id;

    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonProperty(value = "description")
    private String description;

    @JsonProperty(value = "version", required = true)
    private String version;

    @JsonProperty(value = "type", required = true)
    private String type;

    @JsonProperty(value = "vendor")
    private String vendor;

    @JsonProperty(value = "artifacts")
    private List<Artifact> artifacts = new ArrayList<>();

    /**
     * Returns id of {@link SoftwareModuleInfo}.
     *
     * @return id
     */
    @JsonIgnore
    public Long getId() {
        return id;
    }

    /**
     * Sets the id of {@link SoftwareModuleInfo}.
     *
     * @param id.
     */
    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the name of {@link SoftwareModuleInfo}.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of a {@link SoftwareModuleInfo}.
     *
     * @param name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the description of {@link SoftwareModuleInfo}.
     *
     * @return description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of {@link SoftwareModuleInfo}.
     *
     * @param description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the version of {@link SoftwareModuleInfo}.
     *
     * @return version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of {@link SoftwareModuleInfo}.
     *
     * @param version.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the type of {@link SoftwareModuleInfo}.
     *
     * @return type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of {@link SoftwareModuleInfo}.
     *
     * @param type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the vendor of {@link SoftwareModuleInfo}.
     *
     * @return vendor.
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Sets the vendor of {@link SoftwareModuleInfo}.
     *
     * @param vendor.
     */
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    /**
     * Returns the list of {@link Artifact} of {@link SoftwareModuleInfo}.
     *
     * @return List of {@link Artifact}.
     */
    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    /**
     * Sets a list of {@link Artifact} of {@link SoftwareModuleInfo}.
     *
     * @param List
     *            of {@link Artifact}.
     */
    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }
}
