/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

import org.eclipse.hawkbit.repository.jpa.model.JpaRollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.Action.ActionType;

public interface Rollout extends NamedEntity {

    DistributionSet getDistributionSet();

    void setDistributionSet(DistributionSet distributionSet);

    List<RolloutGroup> getRolloutGroups();

    void setRolloutGroups(List<RolloutGroup> rolloutGroups);

    String getTargetFilterQuery();

    void setTargetFilterQuery(String targetFilterQuery);

    RolloutStatus getStatus();

    void setStatus(RolloutStatus status);

    long getLastCheck();

    void setLastCheck(long lastCheck);

    ActionType getActionType();

    void setActionType(ActionType actionType);

    long getForcedTime();

    void setForcedTime(long forcedTime);

    long getTotalTargets();

    void setTotalTargets(long totalTargets);

    int getRolloutGroupsTotal();

    void setRolloutGroupsTotal(int rolloutGroupsTotal);

    int getRolloutGroupsCreated();

    void setRolloutGroupsCreated(int rolloutGroupsCreated);

    TotalTargetCountStatus getTotalTargetCountStatus();

    void setTotalTargetCountStatus(TotalTargetCountStatus totalTargetCountStatus);

}