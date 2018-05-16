/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.rollout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * An action that runs when the error condition is met
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutErrorAction {

    private ErrorAction action = ErrorAction.PAUSE;
    private String expression;

    /**
     * Creates a rollout error action
     * 
     * @param action
     *            the action to run when th error condition is met
     * @param expression
     *            the expression for the action
     */
    public MgmtRolloutErrorAction(ErrorAction action, String expression) {
        this.action = action;
        this.expression = expression;
    }

    /**
     * Default constructor
     */
    public MgmtRolloutErrorAction() {
        // Instantiate default error action
    }

    /**
     * @return the action
     */
    public ErrorAction getAction() {
        return action;
    }

    /**
     * @param action
     *            the action to set
     */
    public void setAction(final ErrorAction action) {
        this.action = action;
    }

    /**
     * @return the expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * @param expression
     *            the expression to set
     */
    public void setExpression(final String expression) {
        this.expression = expression;
    }

    /**
     * Possible actions
     */
    public enum ErrorAction {
        PAUSE;
    }
}
