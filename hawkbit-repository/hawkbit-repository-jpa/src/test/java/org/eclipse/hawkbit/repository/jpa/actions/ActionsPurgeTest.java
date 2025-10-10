/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.actions;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ActionsPurgeTest extends AbstractJpaIntegrationTest {

    @Test
    void testManualAssignmentsActionsPurge() {
        Target target = testdataFactory.createTarget();

        for (int i = 0; i < 20; i++) {
            DistributionSet distributionSet = testdataFactory.createDistributionSet("ds_" + i);
            assignDistributionSet(distributionSet.getId(), target.getControllerId());
        }

        long actions = deploymentManagement.countActionsByTarget(target.getControllerId());
        // quota in tests is set to 20 ...
        Assertions.assertEquals(20, actions);

        // extract the first 5 action ids
        List<Action> firstSample = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
        List<Long> shouldBePurgedActionsList = firstSample.stream().map(Identifiable::getId).limit(5).toList();

        DistributionSet exceededQuotaDsAssign = testdataFactory.createDistributionSet("exceededQuotaAssignment");

        // should throw quota exception if not explicitly configured to purge actions
        Assertions.assertThrows(AssignmentQuotaExceededException.class,
                () -> assignDistributionSet(exceededQuotaDsAssign.getId(), target.getControllerId()));

        // set purge config to 25 %
        tenantConfigurationManagement.addOrUpdateConfiguration("percentage.of.actions.cleaned.on.quota.hit", 25);

        // assign again
        assignDistributionSet(exceededQuotaDsAssign.getId(), target.getControllerId());
        // 16 actions should be present
        actions = deploymentManagement.countActionsByTarget(target.getControllerId());
        Assertions.assertEquals(16, actions);

        List<Action> actionsList = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
        // first 5 should have been purged so the first actionId should be the last purged action id + 1
        Assertions.assertEquals(shouldBePurgedActionsList.get(shouldBePurgedActionsList.size() - 1) + 1, actionsList.get(0).getId());
        Assertions.assertEquals(firstSample.get(firstSample.size() - 1).getId() + 1, actionsList.get(15).getId());
    }

    @Test
    void testRolloutAssignmentsActionsPurge() {
        final Target target = testdataFactory.createTarget();
        for (int i = 0; i < 20; i++) {
            DistributionSet distributionSet = testdataFactory.createDistributionSet();
            Rollout rollout = testdataFactory.createRolloutByVariables("rollout-" + i, "Description", 1, "controllerId==" + target
                    .getControllerId(),
                    distributionSet, "50", "50");
            rolloutManagement.start(rollout.getId());
            rolloutHandler.handleAll();
        }

        Assertions.assertEquals(20, deploymentManagement.countActionsByTarget(target.getControllerId()));
        List<Action> firstSample = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
        List<Long> shouldBePurgedActionsList = firstSample.stream().map(Identifiable::getId).limit(5).toList();

        DistributionSet distributionSet = testdataFactory.createDistributionSet();
        Rollout rollout = testdataFactory.createRolloutByVariables("rollout-quota", "Description", 1, "controllerId==" + target
                .getControllerId(),
                distributionSet, "50", "50");
        rolloutManagement.start(rollout.getId());
        // don't assert quota exception here because rollout executor does not throw such in order to not interrupt other executions
        rolloutHandler.handleAll();
        //check that the old number of actions remain instead
        Assertions.assertEquals(20, deploymentManagement.countActionsByTarget(target.getControllerId()));

        // set purge config to 25 %
        tenantConfigurationManagement.addOrUpdateConfiguration("percentage.of.actions.cleaned.on.quota.hit", 25);
        rolloutHandler.handleAll();
        Assertions.assertEquals(16, deploymentManagement.countActionsByTarget(target.getControllerId()));

        List<Action> actionsList = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
        // first 5 should have been purged so the first actionId should be the last purged action id + 1
        Assertions.assertEquals(shouldBePurgedActionsList.get(shouldBePurgedActionsList.size() - 1) + 1, actionsList.get(0).getId());
        Assertions.assertEquals(firstSample.get(firstSample.size() - 1).getId() + 1, actionsList.get(15).getId());

    }
}
