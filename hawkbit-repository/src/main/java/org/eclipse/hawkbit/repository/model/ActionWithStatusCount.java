/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;

/**
 * Custom JPA Model for querying {@link Action} include the count of the
 * action's {@link ActionStatus}.
 *
 */
// TODO: create interface
public class ActionWithStatusCount {
    private final Long actionStatusCount;
    private final Long actionId;
    private final ActionType actionType;
    private final boolean actionActive;
    private final long actionForceTime;
    private final Status actionStatus;
    private final Long actionCreatedAt;
    private final Long actionLastModifiedAt;
    private final Long dsId;
    private final String dsName;
    private final String dsVersion;
    private final JpaAction action;
    private final String rolloutName;

    /**
     * JPA constructor, the parameter are the result set columns of the custom
     * query.
     *
     * @param actionId
     *            the ID of the action
     * @param actionType
     *            the action type
     * @param active
     *            the active attribute of the action
     * @param forcedTime
     *            the forced time attribute of the action
     * @param status
     *            the status attribute of the action
     * @param actionCreatedAt
     *            the createdAt timestamp of the action
     * @param actionLastModifiedAt
     *            the last modified timestamp of the action
     * @param dsId
     *            the ID of the distributionset
     * @param dsName
     *            the name of the distributionset
     * @param dsVersion
     *            the version of the distributionset
     * @param actionStatusCount
     *            the count of the action status for this action
     * @param rolloutName
     *            the rollout name
     */

    public ActionWithStatusCount(final Long actionId, final ActionType actionType, final boolean active,
            final long forcedTime, final Status status, final Long actionCreatedAt, final Long actionLastModifiedAt,
            final Long dsId, final String dsName, final String dsVersion, final Long actionStatusCount,
            final String rolloutName) {
        this.actionId = actionId;
        this.actionType = actionType;
        actionActive = active;
        actionForceTime = forcedTime;
        actionStatus = status;
        this.actionCreatedAt = actionCreatedAt;
        this.actionLastModifiedAt = actionLastModifiedAt;
        this.dsId = dsId;
        this.dsName = dsName;
        this.dsVersion = dsVersion;
        this.actionStatusCount = actionStatusCount;
        this.rolloutName = rolloutName;

        action = new JpaAction();
        action.setActionType(actionType);
        action.setActive(actionActive);
        action.setForcedTime(actionForceTime);
        action.setStatus(actionStatus);
        action.setId(actionId);
    }

    public Action getAction() {
        return action;
    }

    public Long getActionId() {
        return actionId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public boolean isActionActive() {
        return actionActive;
    }

    public long getActionForceTime() {
        return actionForceTime;
    }

    public Status getActionStatus() {
        return actionStatus;
    }

    public Long getActionCreatedAt() {
        return actionCreatedAt;
    }

    public Long getActionLastModifiedAt() {
        return actionLastModifiedAt;
    }

    public Long getDsId() {
        return dsId;
    }

    public String getDsName() {
        return dsName;
    }

    public String getDsVersion() {
        return dsVersion;
    }

    public Long getActionStatusCount() {
        return actionStatusCount;
    }

    public String getRolloutName() {
        return rolloutName;
    }

}
