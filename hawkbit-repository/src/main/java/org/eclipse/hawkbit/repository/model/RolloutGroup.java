/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup.RolloutGroupSuccessCondition;

public interface RolloutGroup extends NamedEntity {

    Rollout getRollout();

    void setRollout(Rollout rollout);

    RolloutGroupStatus getStatus();

    void setStatus(RolloutGroupStatus status);

    RolloutGroup getParent();

    void setParent(RolloutGroup parent);

    RolloutGroupSuccessCondition getSuccessCondition();

    void setSuccessCondition(RolloutGroupSuccessCondition finishCondition);

    String getSuccessConditionExp();

    void setSuccessConditionExp(String finishExp);

    RolloutGroupErrorCondition getErrorCondition();

    void setErrorCondition(RolloutGroupErrorCondition errorCondition);

    String getErrorConditionExp();

    void setErrorConditionExp(String errorExp);

    RolloutGroupErrorAction getErrorAction();

    void setErrorAction(RolloutGroupErrorAction errorAction);

    String getErrorActionExp();

    void setErrorActionExp(String errorActionExp);

    RolloutGroupSuccessAction getSuccessAction();

    String getSuccessActionExp();

    long getTotalTargets();

    void setTotalTargets(long totalTargets);

    void setSuccessAction(RolloutGroupSuccessAction successAction);

    void setSuccessActionExp(String successActionExp);

    /**
     * @return the totalTargetCountStatus
     */
    TotalTargetCountStatus getTotalTargetCountStatus();

    /**
     * @param totalTargetCountStatus
     *            the totalTargetCountStatus to set
     */
    void setTotalTargetCountStatus(TotalTargetCountStatus totalTargetCountStatus);

}