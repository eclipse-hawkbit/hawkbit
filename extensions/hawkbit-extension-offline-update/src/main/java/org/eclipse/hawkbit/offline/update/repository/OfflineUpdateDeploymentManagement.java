/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update.repository;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.model.Action;

/**
 * This interface defines APIs in addition to {@link DeploymentManagement} for
 * processing an offline software update.
 */
public interface OfflineUpdateDeploymentManagement extends DeploymentManagement {

    /**
     * Finish an {@link Action} for the given actionId.
     *
     * @param actionId
     *            id of the action to be updated
     *
     * @return completed {@link Action}.
     */
    Action finishAction(final Long actionId);
}
