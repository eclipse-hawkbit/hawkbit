/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for DistributionSet for PUT/POST.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDistributionSetRequestBodyPut {

    @JsonProperty
    private String name;

    @JsonProperty
    private String description;

    @JsonProperty
    private String version;

    @JsonProperty
    private Boolean requiredMigrationStep;

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
    public MgmtDistributionSetRequestBodyPut setName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the requiredMigrationStep
     */
    public Boolean isRequiredMigrationStep() {
        return requiredMigrationStep;
    }

    /**
     * @param requiredMigrationStep
     *            the requiredMigrationStep to set
     *
     * @return updated body
     */
    public MgmtDistributionSetRequestBodyPut setRequiredMigrationStep(final Boolean requiredMigrationStep) {
        this.requiredMigrationStep = requiredMigrationStep;

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
    public MgmtDistributionSetRequestBodyPut setDescription(final String description) {
        this.description = description;

        return this;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version
     *            the version to set
     *
     * @return updated body
     */
    public MgmtDistributionSetRequestBodyPut setVersion(final String version) {
        this.version = version;

        return this;
    }
}
