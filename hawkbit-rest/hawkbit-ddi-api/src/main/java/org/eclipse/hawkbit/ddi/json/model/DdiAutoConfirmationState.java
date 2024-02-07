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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

@NoArgsConstructor // needed for json create
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "active", "initiator", "remark", "activatedAt" })
public class DdiAutoConfirmationState extends RepresentationModel<DdiAutoConfirmationState> {

    @NotNull
    @Schema(example = "true")
    private boolean active;
    @Schema(example = "exampleUserId")
    private String initiator;
    @Schema(example = "exampleRemark")
    private String remark;

    @Schema(example = "1691065895439")
    private Long activatedAt;

    public static DdiAutoConfirmationState active(final long activatedAt) {
        final DdiAutoConfirmationState state = new DdiAutoConfirmationState();
        state.setActive(true);
        state.setActivatedAt(activatedAt);
        return state;
    }

    public static DdiAutoConfirmationState disabled() {
        return new DdiAutoConfirmationState();
    }
}