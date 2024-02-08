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

@NoArgsConstructor
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutCondition {

    public enum Condition {
        THRESHOLD
    }

    @Schema(description = "The type of the condition")
    private Condition condition = Condition.THRESHOLD;
    @Schema(description = "The expression according to the condition, e.g. the value of threshold in percentage",
            example = "50")
    private String expression = "100";

    public MgmtRolloutCondition(final Condition condition, final String expression) {
        this.condition = condition;
        this.expression = expression;
    }
}