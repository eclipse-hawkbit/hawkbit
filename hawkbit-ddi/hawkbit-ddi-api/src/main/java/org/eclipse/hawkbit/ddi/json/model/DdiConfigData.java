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

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Feedback channel for ConfigData action.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(example = """
        {
           "mode" : "merge",
           "data" : {
             "VIN" : "JH4TB2H26CC000000",
             "hwRevision" : "2"
           }
         }""")
public class DdiConfigData {

    @NotEmpty
    @Schema(description = "Link which is provided whenever the provisioning target or device is supposed to push its configuration data (aka. \"controller attributes\") to the server. Only shown for the initial configuration, after a successful update action, or if requested explicitly (e.g. via the management UI).")
    private final Map<String, String> data;

    @Schema(description = "Optional parameter to specify the update mode that should be applied when updating target attributes. Valid values are 'merge', 'replace', and 'remove'. Defaults to 'merge'.")
    private final DdiUpdateMode mode;

    /**
     * Constructor.
     *
     * @param data contains the attributes.
     * @param mode defines the mode of the update (replace, merge, remove)
     */
    @JsonCreator
    public DdiConfigData(
            @JsonProperty(value = "data") final Map<String, String> data,
            @JsonProperty(value = "mode") final DdiUpdateMode mode) {
        this.data = data;
        this.mode = mode;
    }
}