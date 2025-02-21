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

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.hateoas.RepresentationModel;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "active", "initiator", "remark", "activatedAt" })
public class DdiAutoConfirmationState extends RepresentationModel<DdiAutoConfirmationState> {

    @NotNull
    @Schema(example = "true")
    private final boolean active;

    @Schema(example = "exampleUserId")
    private final String initiator;

    @Schema(example = "exampleRemark")
    private final String remark;

    @Schema(example = "1691065895439")
    private final Long activatedAt;

    @JsonCreator
    public DdiAutoConfirmationState(
            @JsonProperty("active") final boolean active,
            @JsonProperty("initiator") final String initiator,
            @JsonProperty("remark") final String remark,
            @JsonProperty("activatedAt") final Long activatedAt) {
        this.active = active;
        this.initiator = initiator;
        this.remark = remark;
        this.activatedAt = activatedAt;
    }
}