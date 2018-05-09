/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * This interface provides methods to enable plugging in of different strategies
 * to handle rollout approval.
 */
public interface RolloutApprovalStrategy {

    /**
     * This method handles whether a rollout needs approval. Various factors may be
     * important according to the implementation, e.g. user roles of the rollout
     * creator, state of the system, ....
     *
     * @param rollout
     *            rollout to decide for whether approval is needed.
     * @return true if the rollout according to this strategy needs approval.
     */
    boolean isApprovalNeeded(Rollout rollout);

    /**
     * Depending on the implementation, creation of a approval task,
     * notification,... inside or outside of hawkbit may be necessary.
     * Implementations may also decide to provide an empty implementation for this
     * method.
     *
     * @param rollout
     *            rollout to create approval task for.
     */
    void onApprovalRequired(Rollout rollout);

    /**
     * Returns the user that made a decision to approve or deny the given rollout.
     * Depending on the implementation this may be different to the current user eg.
     * when the decision is made in an external system.
     * 
     * @param rollout
     *            target rollout
     * @return identifier of the user that decided on approval
     */
    String getApprovalUser(Rollout rollout);
}
