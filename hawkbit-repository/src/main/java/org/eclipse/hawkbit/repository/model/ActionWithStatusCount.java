/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;

/**
 * Custom JPA Model for querying {@link Action} include the count of the
 * action's {@link ActionStatus}.
 *
 *
 *
 *
 */
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
    private final Action action;

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
     */
    public ActionWithStatusCount(final Long actionId, final ActionType actionType, final boolean active,
            final long forcedTime, final Status status, final Long actionCreatedAt, final Long actionLastModifiedAt,
            final Long dsId, final String dsName, final String dsVersion, final Long actionStatusCount) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.actionActive = active;
        this.actionForceTime = forcedTime;
        this.actionStatus = status;
        this.actionCreatedAt = actionCreatedAt;
        this.actionLastModifiedAt = actionLastModifiedAt;
        this.dsId = dsId;
        this.dsName = dsName;
        this.dsVersion = dsVersion;
        this.actionStatusCount = actionStatusCount;

        this.action = new Action();
        this.action.setActionType(actionType);
        this.action.setActive(actionActive);
        this.action.setForcedTime(actionForceTime);
        this.action.setStatus(actionStatus);
        this.action.setId(actionId);
    }

    /**
     * @return the action
     */
    public Action getAction() {
        return action;
    }

    /**
     * @return the actionId
     */
    public Long getActionId() {
        return actionId;
    }

    /**
     * @return the actionType
     */
    public ActionType getActionType() {
        return actionType;
    }

    /**
     * @return the actionActive
     */
    public boolean isActionActive() {
        return actionActive;
    }

    /**
     * @return the actionForceTime
     */
    public long getActionForceTime() {
        return actionForceTime;
    }

    /**
     * @return the actionStatus
     */
    public Status getActionStatus() {
        return actionStatus;
    }

    /**
     * @return the actionCreatedAt
     */
    public Long getActionCreatedAt() {
        return actionCreatedAt;
    }

    /**
     * @return the actionLastModifiedAt
     */
    public Long getActionLastModifiedAt() {
        return actionLastModifiedAt;
    }

    /**
     * @return the dsId
     */
    public Long getDsId() {
        return dsId;
    }

    /**
     * @return the dsName
     */
    public String getDsName() {
        return dsName;
    }

    /**
     * @return the dsVersion
     */
    public String getDsVersion() {
        return dsVersion;
    }

    /**
     * @return the actionStatusCount
     */
    public Long getActionStatusCount() {
        return actionStatusCount;
    }
}
