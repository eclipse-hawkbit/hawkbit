/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rollout.condition;

import java.util.List;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Success action which starts the next following {@link RolloutGroup}.
 */
@Component("startNextRolloutGroupAction")
public class StartNextGroupRolloutGroupSuccessAction implements RolloutGroupActionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(StartNextGroupRolloutGroupSuccessAction.class);

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private DeploymentManagement deploymentManagement;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Override
    public boolean verifyExpression(final String expression) {
        return true;
    }

    @Override
    public void eval(final Rollout rollout, final RolloutGroup rolloutGroup, final String expression) {
        systemSecurityContext.runAsSystem(() -> {
            startNextGroup(rollout, rolloutGroup);
            return null;
        });
    }

    private void startNextGroup(final Rollout rollout, final RolloutGroup rolloutGroup) {
        // retrieve all actions accroding to the parent group of the finished
        // rolloutGroup, so retrieve all child-group actions which need to be
        // started.
        final List<Action> rolloutGroupActions = deploymentManagement.findActionsByRolloutGroupParentAndStatus(rollout,
                rolloutGroup, Action.Status.SCHEDULED);
        logger.debug("{} Next actions to start for rollout {} and parent group {}", rolloutGroupActions.size(), rollout,
                rolloutGroup);
        rolloutGroupActions.forEach(action -> deploymentManagement.startScheduledAction(action));
        if (!rolloutGroupActions.isEmpty()) {
            // get all next scheduled groups based on the found actions and set
            // them in state running
            rolloutGroupActions.forEach(action -> {
                final RolloutGroup nextGroup = action.getRolloutGroup();
                logger.debug("Rolloutgroup {} is now running", nextGroup);
                nextGroup.setStatus(RolloutGroupStatus.RUNNING);
                rolloutGroupRepository.save(nextGroup);
            });
        } else {
            logger.info("No actions to start for next rolloutgroup of parent {}", rolloutGroup);
            // nothing for next group, just finish the group, this can happen
            // e.g. if targets has been deleted after the group has been
            // scheduled. If the group is empty now, we just finish the group if
            // there are not actions available for this group.
            final List<RolloutGroup> findByRolloutGroupParent = rolloutGroupRepository
                    .findByParentAndStatus(rolloutGroup, RolloutGroupStatus.SCHEDULED);
            findByRolloutGroupParent.forEach(nextGroup -> {
                logger.debug("Rolloutgroup {} is finished, starting next group", nextGroup);
                nextGroup.setStatus(RolloutGroupStatus.FINISHED);
                rolloutGroupRepository.save(nextGroup);
                // find the next group to set in running state
                startNextGroup(rollout, nextGroup);
            });
        }
    }
}
