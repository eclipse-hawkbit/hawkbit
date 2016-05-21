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

import org.eclipse.hawkbit.repository.model.Action.ActionType;

/**
 * Software update operations in large scale IoT scenarios with hundred of
 * thousands of devices require special handling.
 * 
 * That includes secure handling of large volumes of devices at rollout creation
 * time. Monitoring of the rollout progress. Emergency rollout shutdown in case
 * of problems on to many devices and reporting capabilities for a complete
 * understanding of the rollout progress at each point in time.
 *
 */
public interface Rollout extends NamedEntity {

    /**
     * @return {@link DistributionSet} that is rolled out
     */
    DistributionSet getDistributionSet();

    /**
     * @param distributionSet
     *            that is rolled out
     */
    void setDistributionSet(DistributionSet distributionSet);

    /**
     * @return list of deployment groups of the rollout.
     */
    List<RolloutGroup> getRolloutGroups();

    /**
     * @return rsql query that identifies the targets that are part of this
     *         rollout.
     */
    String getTargetFilterQuery();

    /**
     * @param targetFilterQuery
     *            that identifies the targets that are part of this rollout.
     */
    void setTargetFilterQuery(String targetFilterQuery);

    /**
     * @return status of the rollout
     */
    RolloutStatus getStatus();

    ActionType getActionType();

    void setActionType(ActionType actionType);

    long getForcedTime();

    void setForcedTime(long forcedTime);

    long getTotalTargets();

    int getRolloutGroupsTotal();

    int getRolloutGroupsCreated();

    TotalTargetCountStatus getTotalTargetCountStatus();

    /**
     *
     * State machine for rollout.
     *
     */
    public enum RolloutStatus {

        /**
         * Rollouts is being created.
         */
        CREATING,

        /**
         * Rollout is ready to start.
         */
        READY,

        /**
         * Rollout is paused.
         */
        PAUSED,

        /**
         * Rollout is starting.
         */
        STARTING,

        /**
         * Rollout is stopped.
         */
        STOPPED,

        /**
         * Rollout is running.
         */
        RUNNING,

        /**
         * Rollout is finished.
         */
        FINISHED,

        /**
         * Rollout could not be created due to errors, might be a database
         * problem during asynchronous creating.
         */
        ERROR_CREATING,

        /**
         * Rollout could not be started due to errors, might be database problem
         * during asynchronous starting.
         */
        ERROR_STARTING;
    }

}