/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Allow a target to declare running distribution set version
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiAssignedVersion {

    @Schema(description = "Distribution Set name", example = "linux")
    private final String name;

    @Schema(description = "Distribution set version", example = "1.2.3")
    private final String version;

    /**
     * Constructor
     *
     * @param name
     *            Distribution set name
     * @param version
     *            Distribution set version
     */
    @JsonCreator
    public DdiAssignedVersion(@JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "version", required = true) String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "DdiInstalledVersion{" + "name='" + name + '\'' + ", version='" + version + '\'' + '}';
    }
}
