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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Model for defining Conditions and Actions
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractMgmtRolloutConditionsEntity extends MgmtNamedEntity {

    private MgmtRolloutCondition successCondition;
    private MgmtRolloutSuccessAction successAction;
    private MgmtRolloutCondition errorCondition;
    private MgmtRolloutErrorAction errorAction;
}
