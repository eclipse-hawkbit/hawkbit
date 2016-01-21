/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.distributionsettype;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeAssigmentRest;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request Body for DistributionSetType POST.
 *
 *
 *
 *
 */
public class DistributionSetTypeRequestBodyCreate {

    @ApiModelProperty(value = ApiModelProperties.NAME, required = true)
    @JsonProperty(required = true)
    private String name;

    @ApiModelProperty(value = ApiModelProperties.DESCRPTION)
    @JsonProperty
    private String description;

    @ApiModelProperty(value = ApiModelProperties.DS_TYPE_KEY)
    @JsonProperty
    private String key;

    @ApiModelProperty(value = ApiModelProperties.DS_TYPE_MANDATORY_MODULES)
    @JsonProperty
    private List<SoftwareModuleTypeAssigmentRest> mandatorymodules;

    @ApiModelProperty(value = ApiModelProperties.DS_TYPE_OPTIONAL_MODULES)
    @JsonProperty
    private List<SoftwareModuleTypeAssigmentRest> optionalmodules;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * @return the mandatorymodules
     */
    public List<SoftwareModuleTypeAssigmentRest> getMandatorymodules() {
        return mandatorymodules;
    }

    /**
     * @param mandatorymodules
     *            the mandatorymodules to set
     */
    public void setMandatorymodules(final List<SoftwareModuleTypeAssigmentRest> mandatorymodules) {
        this.mandatorymodules = mandatorymodules;
    }

    /**
     * @return the optionalmodules
     */
    public List<SoftwareModuleTypeAssigmentRest> getOptionalmodules() {
        return optionalmodules;
    }

    /**
     * @param optionalmodules
     *            the optionalmodules to set
     */
    public void setOptionalmodules(final List<SoftwareModuleTypeAssigmentRest> optionalmodules) {
        this.optionalmodules = optionalmodules;
    }

}
