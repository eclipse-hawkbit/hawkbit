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

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cancel action to be provided to the target.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiCancel {

    @Schema(example = "11")
    private final String id;

    @NotNull
    private final DdiCancelActionToStop cancelAction;

    /**
     * Parameterized constructor.
     *
     * @param id
     *            of the cancel action
     * @param cancelAction
     *            the action
     */
    @JsonCreator
    public DdiCancel(@JsonProperty("id") final String id,
            @JsonProperty("cancelAction") final DdiCancelActionToStop cancelAction) {
        this.id = id;
        this.cancelAction = cancelAction;
    }

    public String getId() {
        return id;
    }

    public DdiCancelActionToStop getCancelAction() {
        return cancelAction;
    }

    @Override
    public String toString() {
        return "Cancel [id=" + id + ", cancelAction=" + cancelAction + "]";
    }

}
