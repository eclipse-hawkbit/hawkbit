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

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Action fulfillment progress by means of gives the achieved amount of maximal
 * of possible levels.
 */
@Getter
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiProgress {

    @NotNull
    @Schema(description = "Achieved amount", example = "2")
    private final Integer cnt;

    @Schema(description = "Maximum levels", example = "5")
    private final Integer of;

    /**
     * Constructor.
     *
     * @param cnt achieved amount
     * @param of  maximum levels
     */
    @JsonCreator
    public DdiProgress(@JsonProperty("cnt") final Integer cnt, @JsonProperty("of") final Integer of) {
        this.cnt = cnt;
        this.of = of;
    }
}