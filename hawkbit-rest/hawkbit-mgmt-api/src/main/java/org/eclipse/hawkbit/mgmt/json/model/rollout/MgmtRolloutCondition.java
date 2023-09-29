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

/**
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutCondition {

    private Condition condition = Condition.THRESHOLD;
    @Schema(example = "50")
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
        THRESHOLD
    }
}
