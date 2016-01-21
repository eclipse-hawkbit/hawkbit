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

import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeAssigmentRest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body for DistributionSetType POST.
 *
 */
public class DistributionSetTypeRequestBodyPost {

    @JsonProperty(required = true)
    private String name;

    @JsonProperty
    private String description;

    @JsonProperty
    private String key;

    @JsonProperty
    private List<SoftwareModuleTypeAssigmentRest> mandatorymodules;

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
     *
     * @return updated body
     */
    public DistributionSetTypeRequestBodyPost setName(final String name) {
        this.name = name;
        return this;
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
     *
     * @return updated body
     */
    public DistributionSetTypeRequestBodyPost setDescription(final String description) {
        this.description = description;
        return this;
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
     *
     * @return updated body
     */
    public DistributionSetTypeRequestBodyPost setKey(final String key) {
        this.key = key;
        return this;
    }

    /**
     * @return the mandatory modules
     */
    public List<SoftwareModuleTypeAssigmentRest> getMandatorymodules() {
        return mandatorymodules;
    }

    /**
     * @param mandatorymodules
     *            the mandatory modules to set
     *
     * @return updated body
     */
    public DistributionSetTypeRequestBodyPost setMandatorymodules(
            final List<SoftwareModuleTypeAssigmentRest> mandatorymodules) {
        this.mandatorymodules = mandatorymodules;
        return this;
    }

    /**
     * @return the optional modules
     */
    public List<SoftwareModuleTypeAssigmentRest> getOptionalmodules() {
        return optionalmodules;
    }

    /**
     * @param optionalmodules
     *            the optional modules to set
     *
     * @return updated body
     */
    public DistributionSetTypeRequestBodyPost setOptionalmodules(
            final List<SoftwareModuleTypeAssigmentRest> optionalmodules) {
        this.optionalmodules = optionalmodules;
        return this;
    }

}
