/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request Body for SoftwareModuleType POST.
 *
 */
public class MgmtSoftwareModuleTypeRequestBodyPost extends MgmtSoftwareModuleTypeRequestBodyPut {

    @JsonProperty(required = true)
    @Schema(example = "Example name")
    private String name;

    @JsonProperty(required = true)
    @Schema(example = "Example key")
    private String key;

    @JsonProperty
    @Schema(example = "1")
    private int maxAssignments;

    @Override
    public MgmtSoftwareModuleTypeRequestBodyPost setDescription(final String description) {
        super.setDescription(description);
        return this;
    }

    @Override
    public MgmtSoftwareModuleTypeRequestBodyPost setColour(final String colour) {
        super.setColour(colour);
        return this;
    }

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
    public MgmtSoftwareModuleTypeRequestBodyPost setName(final String name) {
        this.name = name;
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
     * @return updated body
     */
    public MgmtSoftwareModuleTypeRequestBodyPost setKey(final String key) {
        this.key = key;
        return this;
    }

    /**
     * @return the maxAssignments
     */
    public int getMaxAssignments() {
        return maxAssignments;
    }

    /**
     * @param maxAssignments
     *            the maxAssignments to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleTypeRequestBodyPost setMaxAssignments(final int maxAssignments) {
        this.maxAssignments = maxAssignments;
        return this;
    }

}
