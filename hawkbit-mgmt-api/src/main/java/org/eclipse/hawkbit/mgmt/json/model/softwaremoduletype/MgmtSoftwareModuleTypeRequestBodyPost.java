/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body for SoftwareModuleType POST.
 *
 */
public class MgmtSoftwareModuleTypeRequestBodyPost {

    @JsonProperty(required = true)
    private String name;

    @JsonProperty
    private String description;

    @JsonProperty
    private String key;

    @JsonProperty
    private int maxAssignments;

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
    public MgmtSoftwareModuleTypeRequestBodyPost setDescription(final String description) {
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
