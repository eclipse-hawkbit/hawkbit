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
 * 
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutErrorAction {

    private ErrorAction action = ErrorAction.PAUSE;
    private String expression = null;

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

    public enum ErrorAction {
        PAUSE;
    }
}
