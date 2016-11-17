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

/**
 * A custom view on {@link Target} with {@link ActionType}.
 * 
 *
 *
 */
public class TargetWithActionType {

    private final String controllerId;
    private final ActionType actionType;
    private final long forceTime;

    public TargetWithActionType(final String controllerId) {
        this.controllerId = controllerId;
        this.actionType = ActionType.FORCED;
        this.forceTime = 0;
    }

    public TargetWithActionType(final String controllerId, final ActionType actionType, final long forceTime) {
        this.controllerId = controllerId;
        this.actionType = actionType;
        this.forceTime = forceTime;
    }

    public ActionType getActionType() {
        if (actionType != null) {
            return actionType;
        }
        // default value
        return ActionType.FORCED;
    }

    public long getForceTime() {
        if (actionType == ActionType.TIMEFORCED) {
            return forceTime;
        }
        return RepositoryModelConstants.NO_FORCE_TIME;
    }

    public String getControllerId() {
        return controllerId;
    }
}
