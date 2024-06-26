/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.builder.DynamicRolloutGroupTemplate;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Junit tests for RolloutManagement.
 */
@Feature("Component Tests - Repository")
@Story("Rollout Management (Flow)")
class RolloutManagementFlowTest extends AbstractJpaIntegrationTest {

    @BeforeEach
    void reset() {
        this.approvalStrategy.setApprovalNeeded(false);
    }

    @Test
    @Description("Verifies a simple rollout flow")
    void rolloutFlow() {
        final String rolloutName = "rollout-std";
        final int amountGroups = 5; // static only
        final String targetPrefix = "controller-rollout-std-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);

        testdataFactory.createTargets(targetPrefix, 0, amountGroups * 3);
        final Rollout rollout = testdataFactory.createRolloutByVariables(rolloutName, rolloutName, amountGroups,
                "controllerid==" + targetPrefix + "*", distributionSet, "60", "30", false, false);
        final List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(
                new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id")),
                rollout.getId()).getContent();

        // add 2 targets not to be included
        testdataFactory.createTargets(targetPrefix, amountGroups * 3, 2);
        // start rollout
        rolloutManagement.start(rollout.getId());

        // handleStartingRollout (no handleRunning called yet)
        rolloutHandler.handleAll();
        assertRollout(rollout, false, RolloutStatus.RUNNING, amountGroups, amountGroups * 3);
        for (int i = 0; i < amountGroups; i++) {
            assertGroup(groups.get(i), false, i == 0 ? RolloutGroupStatus.RUNNING : RolloutGroupStatus.SCHEDULED, 3);
        }

        executeStaticWithoutOneTargetFromTheLastGroupAndHandleAll(groups, rollout, amountGroups);

        rolloutManagement.pauseRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, false, RolloutStatus.PAUSED, amountGroups, amountGroups * 3);
        assertAndGetRunning(rollout, 1); // keep running

        rolloutManagement.resumeRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, false, RolloutStatus.RUNNING, amountGroups, amountGroups * 3);
        assertAndGetRunning(rollout, 1); // keep running
    }

    @Test
    @Description("Verifies a simple dynamic rollout flow")
    void dynamicRolloutFlow() {
        final String rolloutName = "dynamic-rollout-std";
        final int amountGroups = 3; // static only
        final String targetPrefix = "controller-dynamic-rollout-std-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);

        testdataFactory.createTargets(targetPrefix, 0, amountGroups * 3);
        final Rollout rollout = testdataFactory.createRolloutByVariables(rolloutName, rolloutName, amountGroups,
                "controllerid==" + targetPrefix + "*", distributionSet, "60", "30", false, true);

        // rollout is READY
        assertRollout(rollout, true, RolloutStatus.READY, amountGroups + 1, amountGroups * 3);
        List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(
                new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id")),
                rollout.getId()).getContent();
        final RolloutGroup dynamic1 = groups.get(amountGroups);
        assertRollout(rollout, true, RolloutStatus.READY, amountGroups + 1, amountGroups * 3); // + dynamic
        for (int i = 0; i < amountGroups; i++) {
            assertGroup(groups.get(i), false, RolloutGroupStatus.READY, 3);
        }
        assertGroup(dynamic1, true, RolloutGroupStatus.READY, 0);

        // add 2 targets for the first dynamic group
        testdataFactory.createTargets(targetPrefix, amountGroups * 3, 2);
        // start rollout
        rolloutManagement.start(rollout.getId());

        // handleStartingRollout (no handleRunning called yet)
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 1, amountGroups * 3);
        for (int i = 0; i < amountGroups; i++) {
            assertGroup(groups.get(i), false, i == 0 ? RolloutGroupStatus.RUNNING : RolloutGroupStatus.SCHEDULED, 3);
        }
        assertGroup(dynamic1, true, RolloutGroupStatus.SCHEDULED, 0);

        executeStaticWithoutOneTargetFromTheLastGroupAndHandleAll(groups, rollout, amountGroups);

        // partially fill the first dynamic (it is running and now create actions for 2 targets)
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 1, amountGroups * 3 + 2);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 2);

        // fill first and create second
        testdataFactory.createTargets(targetPrefix, amountGroups * 3 + 2, 2);
        rolloutHandler.handleAll(); // fill first dynamic group and create a new dynamic2
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 3);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 3);
        groups = rolloutGroupManagement.findByRollout(
                new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id")),
                rollout.getId()).getContent();
        final RolloutGroup dynamic2 = groups.get(amountGroups + 1);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 0);

        // create scheduled actions for the dynamic2
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 3);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 3);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 0);
        assertAndGetRunning(rollout, 4); // one from the last static group and 3 from the first dynamic
        assertScheduled(rollout, 0);

        // executes last from static and dynamic1 without 1 target
        assertAndGetRunning(rollout, 4)// one from the last static and 6 for the first dynamic
                .stream()
                // filters for action of the last static group
                .filter(action -> Integer.parseInt(action.getTarget().getControllerId().substring(targetPrefix.length())) < amountGroups * 3)
                .forEach(this::finishAction);
        executeWithoutOneTargetFromAGroup(rollout, dynamic1, 3);
        assertAndGetRunning(rollout, 1); // remains on in the first dynamic

        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 3);
        assertGroup(groups.get(amountGroups - 1), false, RolloutGroupStatus.FINISHED, 3);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 3);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 0);

        rolloutHandler.handleAll(); // add 1 action to now running second dynamic
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 4);
        assertAndGetRunning(rollout, 2);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 1);

        testdataFactory.createTargets(targetPrefix, amountGroups * 3 + 4, 1);
        rolloutManagement.pauseRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.PAUSED, amountGroups + 2, amountGroups * 3 + 4);
        assertAndGetRunning(rollout, 2);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 1); // no new assignment

        rolloutManagement.resumeRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 5);
        assertAndGetRunning(rollout, 3);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 2); // assign the target created when paused
    }

    @Test
    @Description("Verifies a simple dynamic rollout flow with a dynamic group template")
    void dynamicRolloutTemplateFlow() {
        final String rolloutName = "dynamic-template-rollout-std";
        final int amountGroups = 3; // static only
        final String targetPrefix = "controller-template-dynamic-rollout-std-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);

        // create rollout with amountGroups static groups * 3 targets and dynamic group template with 6 targets
        testdataFactory.createTargets(targetPrefix, 0, amountGroups * 3);
        final Rollout rollout = testdataFactory.createRolloutByVariables(rolloutName, rolloutName, amountGroups,
                "controllerid==" + targetPrefix + "*", distributionSet, "60", "30",
                Action.ActionType.FORCED, 1000, false, true,
                DynamicRolloutGroupTemplate.builder().nameSuffix("-dyn").targetCount(6).build());

        // rollout is READY, amountGroups + 1 (dynamic) rollout groups and amountGroups * 3 targets in static groups
        assertRollout(rollout, true, RolloutStatus.READY, amountGroups + 1, amountGroups * 3);
        List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(
                new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id")),
                rollout.getId()).getContent();
        final RolloutGroup dynamic1 = groups.get(amountGroups);
        assertRollout(rollout, true, RolloutStatus.READY, amountGroups + 1, amountGroups * 3); // + dynamic
        for (int i = 0; i < amountGroups; i++) {
            assertGroup(groups.get(i), false, RolloutGroupStatus.READY, 3);
        }
        assertGroup(dynamic1, true, RolloutGroupStatus.READY, 0);

        // add 4 targets for the first dynamic group, fill partially
        testdataFactory.createTargets(targetPrefix, amountGroups * 3, 4);
        // start rollout
        rolloutManagement.start(rollout.getId());

        // handleStartingRollout (no handleRunning called yet)
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 1, amountGroups * 3);
        for (int i = 0; i < amountGroups; i++) {
            assertGroup(groups.get(i), false, i == 0 ? RolloutGroupStatus.RUNNING : RolloutGroupStatus.SCHEDULED, 3);
        }
        assertGroup(dynamic1, true, RolloutGroupStatus.SCHEDULED, 0);

        executeStaticWithoutOneTargetFromTheLastGroupAndHandleAll(groups, rollout, amountGroups);

        // partially fill the first dynamic (it is running and now create actions for 4 targets)
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 1, amountGroups * 3 + 4);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 4);

        // fill first (2) and create fill partially the second (+2 new)
        testdataFactory.createTargets(targetPrefix, amountGroups * 3 + 4, 4);
        rolloutHandler.handleAll(); // fill first dynamic group and create a new dynamic2
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 6);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        groups = rolloutGroupManagement.findByRollout(
                new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id")),
                rollout.getId()).getContent();
        final RolloutGroup dynamic2 = groups.get(amountGroups + 1);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 0);

        // create scheduled actions for the dynamic2
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 6);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 0);
        assertAndGetRunning(rollout, 7); // one from the last static group and 6 from the first dynamic
        assertScheduled(rollout, 0);

        // executes last from static and dynamic1 without 1 target
        assertAndGetRunning(rollout, 7)// one from the last static and 6 for the first dynamic
                .stream()
                // filters for action of the last static group
                .filter(action -> Integer.parseInt(action.getTarget().getControllerId().substring(targetPrefix.length())) < amountGroups * 3)
                .forEach(this::finishAction);
        executeWithoutOneTargetFromAGroup(rollout, dynamic1, 6);
        assertAndGetRunning(rollout, 1); // remains on in the first dynamic

        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 6);
        assertGroup(groups.get(amountGroups - 1), false, RolloutGroupStatus.FINISHED, 3);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 0);

        rolloutHandler.handleAll(); // add 2 action to now running second dynamic
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 8);
        assertAndGetRunning(rollout, 3);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 2);

        testdataFactory.createTargets(targetPrefix, amountGroups * 3 + 8, 2);
        rolloutManagement.pauseRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.PAUSED, amountGroups + 2, amountGroups * 3 + 8);
        assertAndGetRunning(rollout, 3);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 2); // no new assignment

        rolloutManagement.resumeRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 10);
        assertAndGetRunning(rollout, 5);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 4); // assign the target created when paused
    }


    @Test
    @Description("Verifies a simple pure (no static groups) dynamic rollout flow with a dynamic group template")
    void dynamicRolloutPureFlow() {
        final String rolloutName = "pure-dynamic-rollout-std";
        final String targetPrefix = "controller-pure-dynamic-rollout-std-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);

        final Rollout rollout = testdataFactory.createRolloutByVariables(rolloutName, rolloutName, 0,
                "controllerid==" + targetPrefix + "*", distributionSet, "60", "30",
                Action.ActionType.FORCED, 1000, false, true,
                DynamicRolloutGroupTemplate.builder().nameSuffix("-dyn").targetCount(6).build());

        // rollout is READY, amountGroups + 1 (dynamic) rollout groups and amountGroups * 3 targets in static groups
        assertRollout(rollout, true, RolloutStatus.READY, 1, 0);
        List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(
                new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id")),
                rollout.getId()).getContent();
        final RolloutGroup dynamic1 = groups.get(0);
        assertRollout(rollout, true, RolloutStatus.READY, 1, 0); // + dynamic
        assertGroup(dynamic1, true, RolloutGroupStatus.READY, 0);

        // add 4 targets for the first dynamic group, fill partially
        testdataFactory.createTargets(targetPrefix, 0, 4);
        // start rollout
        rolloutManagement.start(rollout.getId());

        // handleStartingRollout (no handleRunning called yet)
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, 1, 0);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 0);

        // partially fill the first dynamic (it is running and now create actions for 4 targets)
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, 1, 4);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 4);

        // fill first (2) and create fill partially the second (+2 new)
        testdataFactory.createTargets(targetPrefix, 4, 4);
        rolloutHandler.handleAll(); // fill first dynamic group and create a new dynamic2
        assertRollout(rollout, true, RolloutStatus.RUNNING, 2, 6);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        groups = rolloutGroupManagement.findByRollout(
                new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id")),
                rollout.getId()).getContent();
        final RolloutGroup dynamic2 = groups.get(1);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 0);

        // create scheduled actions for the dynamic2
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, 2, 6);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 0);
        assertAndGetRunning(rollout, 6); // 6 from the first dynamic
        assertScheduled(rollout, 0);

        // executes dynamic1 without 1 target
        executeWithoutOneTargetFromAGroup(rollout, dynamic1, 6);
        assertAndGetRunning(rollout, 1); // remains on in the first dynamic

        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING,  2, 6);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 0);

        rolloutHandler.handleAll(); // add 2 action to now running second dynamic
        assertRollout(rollout, true, RolloutStatus.RUNNING, 2, 8);
        assertAndGetRunning(rollout, 3);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 2);

        testdataFactory.createTargets(targetPrefix, 8, 2);
        rolloutManagement.pauseRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.PAUSED, 2, 8);
        assertAndGetRunning(rollout, 3);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 2); // no new assignment

        rolloutManagement.resumeRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, 2, 10);
        assertAndGetRunning(rollout, 5);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 4); // assign the target created when paused
    }

    private void executeStaticWithoutOneTargetFromTheLastGroupAndHandleAll(
            final List<RolloutGroup> groups,
            final Rollout rollout, final int amountGroups) {
        // execute groups (without on of the last)
        assertThat(refresh(groups.get(0)).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        for (int i = 0; i < amountGroups; i++) {
            if (i + 1 < groups.size()) {
                assertThat(refresh(groups.get(i + 1)).getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED);
            }
            // skip on from the last group only
            final AtomicBoolean skipOne = new AtomicBoolean(i + 1 == amountGroups);
            assertAndGetRunning(rollout, 3)
                    .stream()
                    .filter(action -> {
                        if (skipOne.get()) {
                            skipOne.set(false);
                            // in the last group, skip first
                            return false;
                        } else {
                            return true;
                        }
                    })
                    .forEach(this::finishAction);
            assertAndGetRunning(rollout, i + 1 == amountGroups ? 1 : 0);
            rolloutHandler.handleAll();
            final RolloutGroupStatus expectedStatus =
                    i + 1 == amountGroups ? RolloutGroupStatus.RUNNING : RolloutGroupStatus.FINISHED;
            assertThat(refresh(groups.get(i)).getStatus())
                    .as("Check that group %s is in status %s", i, expectedStatus)
                    .isEqualTo(expectedStatus);
            if (i + 1 != amountGroups) {
                assertThat(refresh(groups.get(i + 1)).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
            }
        }
    }

    private void executeWithoutOneTargetFromAGroup(
            final Rollout rollout, final RolloutGroup group, final int count) {
        // execute groups (without on of the last)
        assertThat(refresh(group).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        // skip on from the last group only
        final AtomicBoolean skipOne = new AtomicBoolean(true);
        final Page<JpaAction> running = actionRepository.findByRolloutIdAndStatus(PAGE, rollout.getId(), Action.Status.RUNNING);
        assertThat(running.getTotalElements()).as("Action count").isEqualTo(count);
        running.stream()
                // check if the action belongs to the needed group. group equals may not working because of the optLockRevision
                .filter(action -> action.getRolloutGroup().getId().equals(group.getId()))
                .filter(action -> {
                    if (skipOne.get()) {
                        skipOne.set(false);
                        // in the last group, skip first
                        return false;
                    } else {
                        return true;
                    }
                })
                .forEach(this::finishAction);
        assertAndGetRunning(rollout, 1);
    }
}