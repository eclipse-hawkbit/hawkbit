/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.rollout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An action that runs when the error condition is met
 */
@NoArgsConstructor
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutErrorAction {

    public enum ErrorAction {
        PAUSE
    }

    private ErrorAction action = ErrorAction.PAUSE;
    @Schema(example = "80")
    private String expression;

    /**
     * Creates a rollout error action
     * 
     * @param action the action to run when th error condition is met
     * @param expression the expression for the action
     */
    public MgmtRolloutErrorAction(ErrorAction action, String expression) {
        this.action = action;
        this.expression = expression;
    }
}