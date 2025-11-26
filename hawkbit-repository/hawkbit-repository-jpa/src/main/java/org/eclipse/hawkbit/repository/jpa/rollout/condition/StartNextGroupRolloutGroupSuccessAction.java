/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rollout.condition;

import static org.eclipse.hawkbit.context.AccessContext.asSystem;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;

/**
 * Success action which starts the next following {@link RolloutGroup}.
 */
@Slf4j
public class StartNextGroupRolloutGroupSuccessAction implements RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupSuccessAction> {

    private final RolloutGroupRepository rolloutGroupRepository;
    private final DeploymentManagement deploymentManagement;

    public StartNextGroupRolloutGroupSuccessAction(
            final RolloutGroupRepository rolloutGroupRepository, final DeploymentManagement deploymentManagement) {
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.deploymentManagement = deploymentManagement;
    }

    @Override
    public RolloutGroup.RolloutGroupSuccessAction getAction() {
        return RolloutGroup.RolloutGroupSuccessAction.NEXTGROUP;
    }

    // Note - the exec could be called by JpaRolloutsExecutor and by JpaRolloutsManagement#triggerNextGroup
    // this means it could be called by concurrently.
    @Override
    public void exec(final Rollout rollout, final RolloutGroup rolloutGroup) {
        // as system so to assume needed permissions. When called the permission to start next group are assumed anyway
        asSystem(() -> {
            // retrieve all actions according to the parent group of the finished rolloutGroup,
            // so retrieve all child-group actions which need to be started.
            deploymentManagement.startScheduledActionsByRolloutGroupParent(
                    rollout.getId(), rollout.getDistributionSet().getId(), rolloutGroup.getId());
            log.debug("Next actions started for rollout {} and parent group {}", rollout, rolloutGroup);
            if (!rolloutGroupRepository
                    .findByParentIdAndStatus(rolloutGroup.getId(), RolloutGroupStatus.SCHEDULED).isEmpty()) {
                // get next scheduled group and set them in state running
                // there could be a case that the next group is empty (e.g. targets has been deleted after the group has been scheduled)
                // but then, on the next run it will be finished and next will be started.
                rolloutGroupRepository.setStatusForChildren(RolloutGroupStatus.RUNNING, rolloutGroup);
                log.debug("Next group set to RUNNING for rollout {} and parent group {}", rollout, rolloutGroup);
            }
            return null;
        });
    }
}