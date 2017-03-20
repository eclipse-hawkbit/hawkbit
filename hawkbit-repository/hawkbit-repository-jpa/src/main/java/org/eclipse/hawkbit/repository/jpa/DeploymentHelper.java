/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;

/**
 * Utility class for deployment related topics.
 *
 */
public final class DeploymentHelper {

    private DeploymentHelper() {
        // utility class
    }

    /**
     * Internal helper method used only inside service level. As a result is no
     * additional security necessary.
     *
     * @param target
     *            to update
     * @param status
     *            of the target
     * @param setInstalledDate
     *            to set
     * @param targetInfoRepository
     *            for the operation
     *
     * @return updated target
     */
    static JpaTarget updateTargetInfo(@NotNull final JpaTarget target, @NotNull final TargetUpdateStatus status,
            final boolean setInstalledDate) {
        target.setUpdateStatus(status);

        if (setInstalledDate) {
            target.setInstallationDate(System.currentTimeMillis());
        }
        return target;
    }

    /**
     * This method is called, when cancellation has been successful. It sets the
     * action to canceled, resets the meta data of the target and in case there
     * is a new action this action is triggered.
     *
     * @param action
     *            the action which is set to canceled
     * @param actionRepository
     *            for the operation
     * @param targetRepository
     *            for the operation
     * @param targetInfoRepository
     *            for the operation
     */
    static void successCancellation(final JpaAction action, final ActionRepository actionRepository,
            final TargetRepository targetRepository) {

        // set action inactive
        action.setActive(false);
        action.setStatus(Status.CANCELED);

        final JpaTarget target = (JpaTarget) action.getTarget();
        final List<Action> nextActiveActions = actionRepository.findByTargetAndActiveOrderByIdAsc(target, true).stream()
                .filter(a -> !a.getId().equals(action.getId())).collect(Collectors.toList());

        if (nextActiveActions.isEmpty()) {
            target.setAssignedDistributionSet(target.getInstalledDistributionSet());
            updateTargetInfo(target, TargetUpdateStatus.IN_SYNC, false);
        } else {
            target.setAssignedDistributionSet(nextActiveActions.get(0).getDistributionSet());
        }
        targetRepository.save(target);
    }

}
