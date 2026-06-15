/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
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
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiActivateAutoConfirmation {

    @Schema(description = "Individual value (e.g. username) stored as initiator and automatically used as confirmed" +
            " user in future actions", example = "exampleUser")
    private final String initiator;

    @Schema(description = "Individual value to attach a remark which will be persisted when automatically " +
            "confirming future actions", example = "exampleRemark")
    private final String remark;

    @JsonCreator
    public DdiActivateAutoConfirmation(
            @Nullable @JsonProperty(value = "initiator") final String initiator,
            @Nullable @JsonProperty(value = "remark") final String remark) {
        this.initiator = initiator;
        this.remark = remark;
    }
}