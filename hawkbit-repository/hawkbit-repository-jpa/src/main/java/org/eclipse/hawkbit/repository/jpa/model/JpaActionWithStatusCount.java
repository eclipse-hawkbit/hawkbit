/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.ActionWithStatusCount;

/**
 * Custom JPA Model for querying {@link Action} include the count of the
 * action's {@link ActionStatus}.
 *
 */
public class JpaActionWithStatusCount implements ActionWithStatusCount {
    private final Long actionStatusCount;
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
    // Exception squid:S00107 - needed this way for JPA to fill the view
    @SuppressWarnings("squid:S00107")
    public JpaActionWithStatusCount(final Long actionId, final ActionType actionType, final boolean active,
            final Long forcedTime, final Status status, final Long actionCreatedAt, final Long actionLastModifiedAt,
            final Long dsId, final String dsName, final String dsVersion, final Long actionStatusCount,
            final String rolloutName) {
        this.dsId = dsId;
        this.dsName = dsName;
        this.dsVersion = dsVersion;
        this.actionStatusCount = actionStatusCount;
        this.rolloutName = rolloutName;

        action = new JpaAction();
        action.setActionType(actionType);
        action.setActive(active);
        action.setForcedTime(forcedTime);
        action.setStatus(status);
        action.setId(actionId);
        action.setActionType(actionType);
        action.setCreatedAt(actionCreatedAt);
        action.setLastModifiedAt(actionLastModifiedAt);

    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public Long getDsId() {
        return dsId;
    }

    @Override
    public String getDsName() {
        return dsName;
    }

    @Override
    public String getDsVersion() {
        return dsVersion;
    }

    @Override
    public Long getActionStatusCount() {
        return actionStatusCount;
    }

    @Override
    public String getRolloutName() {
        return rolloutName;
    }
}
