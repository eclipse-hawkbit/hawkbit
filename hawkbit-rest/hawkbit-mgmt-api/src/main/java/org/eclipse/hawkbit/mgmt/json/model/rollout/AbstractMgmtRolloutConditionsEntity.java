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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;

/**
 * Model for defining Conditions and Actions
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractMgmtRolloutConditionsEntity extends MgmtNamedEntity {

    @Schema(description = "The success condition which takes in place to evaluate if a rollout group is successful " +
            "and so the next group can be started")
    private MgmtRolloutCondition successCondition;

    @Schema(description = "The success action which takes in place to execute in case the success action is fulfilled")
    private MgmtRolloutSuccessAction successAction;

    @Schema(description = "The error condition which takes in place to evaluate if a rollout group encounter errors")
    private MgmtRolloutCondition errorCondition;

    @Schema(description = "The error action which is executed if the error condition is fulfilled")
    private MgmtRolloutErrorAction errorAction;
}