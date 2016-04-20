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
public class MgmtRolloutCondition {

    private Condition condition = Condition.THRESHOLD;
    private String expression = "100";

    /**
     * 
     */
    public MgmtRolloutCondition() {
        // needed for jackson json creator.
    }

    public MgmtRolloutCondition(final Condition condition, final String expression) {
        this.condition = condition;
        this.expression = expression;
    }

    /**
     * @return the condition
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * @param condition
     *            the condition to set
     */
    public void setCondition(final Condition condition) {
        this.condition = condition;
    }

    /**
     * @return the expession
     */
    public String getExpression() {
        return expression;
    }

    /**
     * @param expession
     *            the expession to set
     */
    public void setExpression(final String expession) {
        this.expression = expession;
    }

    public enum Condition {
        THRESHOLD;
    }
}
