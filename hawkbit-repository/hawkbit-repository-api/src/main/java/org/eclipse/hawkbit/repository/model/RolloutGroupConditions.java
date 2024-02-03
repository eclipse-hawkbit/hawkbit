/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import lombok.Data;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;

/**
 * Object which holds all {@link RolloutGroup} conditions together which can
 * easily built.
 */
@Data
public class RolloutGroupConditions {

    private RolloutGroupSuccessCondition successCondition;
    private String successConditionExp;
    private RolloutGroupSuccessAction successAction;
    private String successActionExp;
    private RolloutGroupErrorCondition errorCondition;
    private String errorConditionExp;
    private RolloutGroupErrorAction errorAction;
    private String errorActionExp;
}