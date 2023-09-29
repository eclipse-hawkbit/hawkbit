/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A json annotated rest model for DistributionSet for PUT/POST.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDistributionSetRequestBodyPut {

    @JsonProperty
    @Schema(example = "dsOne")
    private String name;

    @JsonProperty
    @Schema(example = "Description of the distribution set.")
    private String description;

    @JsonProperty
    @Schema(example = "1.0.0")
    private String version;

    @JsonProperty
    @Schema(example = "false")
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
