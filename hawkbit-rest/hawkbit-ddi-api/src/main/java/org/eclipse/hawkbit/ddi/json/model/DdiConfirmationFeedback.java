/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * New update actions require confirmation when confirmation flow is switched on. This is the feedback channel for
 * confirmation messages for DDI API. The confirmation message has a mandatory field confirmation with possible values:
 * "confirmed" and "denied".
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiConfirmationFeedback {

    @NotNull
    @Valid
    private final Confirmation confirmation;

    private final Integer code;

    private final List<String> details;


    /**
     * Constructs an confirmation-feedback
     *
     * @param confirmation  confirmation value for the action. Valid values are "Confirmed" and "Denied
     * @param code  code for confirmation
     * @param details messages
     */
    @JsonCreator public DdiConfirmationFeedback(
            @JsonProperty(value = "confirmation", required = true) final Confirmation confirmation,
            @JsonProperty(value = "code") final Integer code,
            @JsonProperty(value = "details") final List<String> details) {
        this.confirmation = confirmation;
        this.code = code;
        this.details = details;
    }

    public Confirmation getConfirmation() {
        return confirmation;
    }

    public Integer getCode() {
        return code;
    }

    public List<String> getDetails() {
        if (details == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(details);
    }

    public enum Confirmation {
        /**
         * Confirm the action.
         */
        CONFIRMED("confirmed"),

        /**
         * Deny the action.
         */
        DENIED("denied");


        private final String name;

        Confirmation(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }

    @Override
    public String toString() {
        return "DdiConfirmationFeedback [confirmation=" + confirmation + ", code=" + code + ", details=" + details +"]";
    }
}
