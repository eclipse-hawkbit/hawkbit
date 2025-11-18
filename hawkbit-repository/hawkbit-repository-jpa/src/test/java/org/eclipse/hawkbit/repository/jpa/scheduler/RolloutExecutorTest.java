/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;

public class RolloutExecutorTest extends AbstractJpaIntegrationTest {

    @Autowired
    private JpaRolloutExecutor jpaRolloutExecutor;

    /**
     * Tests action based aspects of the dynamic group assignment filters.
     * Target matches filter no active action with ge weight.
     */
    @Test
    void findByTargetFilterQueryAndNoOverridingActionsAndNotInRolloutAndCompatibleAndUpdatable() {
        final String targetPrefix = "dyn_action_filter_";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(targetPrefix, 10);
        final Rollout rolloutOlder = testdataFactory.createRollout();
        final Rollout rollout = testdataFactory.createRollout();
        final Rollout rolloutNewer = testdataFactory.createRollout();

        int target = 0;
        final List<Integer> expected = new ArrayList<>();
        // old ro with less weight - match
        expected.add(target);
        createAction(targets.get(target++), rolloutOlder, 0, Action.Status.RUNNING, distributionSet);
        // old ro with equal weight - match
        expected.add(target);
        createAction(targets.get(target++), rolloutOlder, 10, Action.Status.RUNNING, distributionSet);
        // old ro with bigger weight, scheduled - match
        expected.add(target);
        createAction(targets.get(target++), rolloutOlder, 11, Action.Status.SCHEDULED, distributionSet);
        // old ro with bigger weight, running - match
        expected.add(target);
        createAction(targets.get(target++), rolloutOlder, 11, Action.Status.RUNNING, distributionSet);
        // old ro with bigger weight, running - match
        expected.add(target);
        createAction(targets.get(target++), rolloutOlder, 11, Action.Status.FINISHED, distributionSet);
        // same ro - doesn't match
        createAction(targets.get(target++), rollout, 10, Action.Status.RUNNING, distributionSet);
        // new ro with less weight - doesn't match
        createAction(targets.get(target++), rolloutNewer, 0, Action.Status.RUNNING, distributionSet);
        // new ro with less weight - doesn't match
        createAction(targets.get(target++), rolloutNewer, 5, Action.Status.WARNING, distributionSet);
        // NEW ro with EQUAL weight - doesn't match
        createAction(targets.get(target++), rolloutNewer, 10, Action.Status.RUNNING, distributionSet);
        // new ro with BIGGER weight - doesn't match
        createAction(targets.get(target), rolloutNewer, 20, Action.Status.DOWNLOADED, distributionSet);

        final Slice<Target> matching =
                jpaRolloutExecutor.findByRsqlAndNoOverridingActionsAndNotInRolloutAndCompatibleAndUpdatable(
                        rollout.getId(), "controllerid==dyn_action_filter_*", distributionSet.getType(), PAGE);

        assertThat(matching.getNumberOfElements()).isEqualTo(expected.size());
        assertThat(matching.stream()
                .map(Target::getControllerId)
                .map(s -> s.substring(targetPrefix.length()))
                .map(Integer::parseInt)
                .sorted()
                .toList()).isEqualTo(expected);
    }

    private void createAction(
            final Target target, final Rollout rollout, final Integer weight, final Action.Status status, final DistributionSet distributionSet) {
        final JpaAction action = new JpaAction();
        action.setActionType(Action.ActionType.FORCED);
        action.setTarget(target);
        action.setInitiatedBy("test");
        if (rollout != null) {
            action.setRollout(rollout);
        }
        if (weight != null) {
            action.setWeight(weight);
        }
        action.setStatus(status);
        action.setActive(status != Action.Status.FINISHED && status != Action.Status.ERROR && status != Action.Status.CANCELED);
        action.setDistributionSet(distributionSet);
        actionRepository.save(action);
    }
}
