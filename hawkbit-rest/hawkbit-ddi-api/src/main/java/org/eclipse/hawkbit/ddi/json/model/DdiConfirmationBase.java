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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Confirmation base response.
 * Set order to place links at last.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "autoConfirm" })
public class DdiConfirmationBase extends RepresentationModel<DdiConfirmationBase> {

    @JsonProperty("autoConfirm")
    @NotNull
    private DdiAutoConfirmationState autoConfirm;

    /**
     * Constructor.
     */
    public DdiConfirmationBase() {
        // needed for json create.
    }

    /**
     * Constructor.
     *
     * @param autoConfirmState
     */
    public DdiConfirmationBase(final DdiAutoConfirmationState autoConfirmState) {
        this.autoConfirm = autoConfirmState;
    }

    public DdiAutoConfirmationState getAutoConfirm() {
        return autoConfirm;
    }
}
