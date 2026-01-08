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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.callAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withUser;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.SneakyThrows;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.TestPropertySource;

/**
 * Junit tests for RolloutManagement.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Rollout Management (Flow)
 */
@TestPropertySource(properties = { "hawkbit.server.repository.dynamicRolloutsMinInvolvePeriodMS=-1" })
class RolloutManagementFlowTest extends AbstractJpaIntegrationTest {

    @BeforeEach
    void reset() {
        this.approvalStrategy.setApprovalNeeded(false);
    }

    /**
     * Verifies a simple rollout flow
     */
    @SneakyThrows
    @Test
    void rolloutDefaultFlow() {
        rolloutFlow(RolloutGroupSuccessAction.NEXTGROUP);
    }

    /**
     * Verifies a simple rollout flow
     */
    @SneakyThrows
    @Test
    void rolloutPauseFlow() {
        rolloutFlow(RolloutGroupSuccessAction.PAUSE);
    }


    void rolloutFlow(final RolloutGroupSuccessAction successAction) throws Exception {
        final String rolloutName = "rollout-std";
        final int amountGroups = 5; // static only
        final String targetPrefix = "controller-rollout-std-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSetLocked("dsFor" + rolloutName);

        testdataFactory.createTargets(targetPrefix, 0, amountGroups * 3);
        final Rollout rollout = callAs(
                withUser("rolloutFlowUser", "READ_DISTRIBUTION_SET", "READ_TARGET", "READ_ROLLOUT", "CREATE_ROLLOUT"),
                () -> testdataFactory.createRolloutByVariables(rolloutName, rolloutName, amountGroups,
                        "controllerid==" + targetPrefix + "*", distributionSet, "60", successAction,"30", false, false));
        final List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id"))).getContent();

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

        executeStaticWithoutOneTargetFromTheLastGroupAndHandleAll(groups, rollout, amountGroups, successAction);

        assertRollout(rollout, false, RolloutStatus.RUNNING, amountGroups, amountGroups * 3);

        rolloutManagement.pauseRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, false, RolloutStatus.PAUSED, amountGroups, amountGroups * 3);
        assertAndGetRunning(rollout, 1); // keep running

        rolloutManagement.resumeRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, false, RolloutStatus.RUNNING, amountGroups, amountGroups * 3);
        assertAndGetRunning(rollout, 1); // keep running
    }

    /**
     * Verifies a simple dynamic rollout flow with default {@link RolloutGroupSuccessAction#NEXTGROUP} success action
     */
    @SneakyThrows
    @Test
    void dynamicRolloutDefaultFlow() {
        dynamicRolloutFlow(RolloutGroupSuccessAction.NEXTGROUP);
    }

    /**
     * Verifies a simple dynamic rollout flow with {@link RolloutGroupSuccessAction#PAUSE} success action
     */
    @SneakyThrows
    @Test
    void dynamicRolloutPauseFlow() {
        dynamicRolloutFlow(RolloutGroupSuccessAction.PAUSE);
    }

    void dynamicRolloutFlow(final RolloutGroup.RolloutGroupSuccessAction successAction) throws Exception {
        final String rolloutName = "dynamic-rollout-std";
        final int amountGroups = 2; // static only
        final String targetPrefix = "controller-dynamic-rollout-std-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSetLocked("dsFor" + rolloutName);

        testdataFactory.createTargets(targetPrefix, 0, amountGroups * 3);
        final Rollout rollout = callAs(
                withUser("dynamicRolloutFlow", "READ_DISTRIBUTION_SET", "READ_TARGET", "READ_ROLLOUT", "CREATE_ROLLOUT"),
                () -> testdataFactory.createRolloutByVariables(rolloutName, rolloutName, amountGroups,
                        "controllerid==" + targetPrefix + "*", distributionSet, "60", successAction,"30", false, true));

        // rollout is READY
        assertRollout(rollout, true, RolloutStatus.READY, amountGroups + 1, amountGroups * 3);
        List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id"))).getContent();
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

        executeStaticWithoutOneTargetFromTheLastGroupAndHandleAll(groups, rollout, amountGroups, successAction);

        // partially fill the first dynamic (it is running and now create actions for 2 targets)
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 1, amountGroups * 3 + 2);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 2);

        // fill first and create second
        testdataFactory.createTargets(targetPrefix, amountGroups * 3 + 2, 2);
        rolloutHandler.handleAll(); // fill first dynamic group
        rolloutHandler.handleAll(); // and create a new dynamic2
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 3);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 3);
        groups = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id"))).getContent();
        final RolloutGroup dynamic2 = groups.get(amountGroups + 1);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 0);

        // create scheduled actions for the dynamic2
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 4);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 3);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 1);
        assertAndGetRunning(rollout, 4); // one from the last static group and 3 from the first dynamic
        assertScheduled(rollout, 1);

        // executes last from static and dynamic1 without 1 target
        assertAndGetRunning(rollout, 4)// one from the last static and 6 for the first dynamic
                .stream()
                // filters for action of the last static group
                .filter(action -> Integer.parseInt(action.getTarget().getControllerId().substring(targetPrefix.length())) < amountGroups * 3)
                .forEach(this::finishAction);
        executeWithoutOneTargetFromAGroup(dynamic1, rollout, 3);
        assertAndGetRunning(rollout, 1); // remains on in the first dynamic
        if (successAction == RolloutGroupSuccessAction.PAUSE) {
            // let success pause action run
            rolloutHandler.handleAll();
            // external resume rollout
            rolloutManagement.resumeRollout(rollout.getId());
        }
        // start next group
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 4);
        assertGroup(groups.get(amountGroups - 1), false, RolloutGroupStatus.FINISHED, 3);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 3);
        // first dynamic threshold is reached, second is started
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 1);
        assertAndGetRunning(rollout, 2);

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

        // finish the second dynamic group
        testdataFactory.createTargets(targetPrefix, amountGroups * 3 + 5, 1);
        rolloutHandler.handleAll();
        rolloutHandler.handleAll(); // create next
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 3, amountGroups * 3 + 6);
        assertAndGetRunning(rollout, 4); // one from the dynamic1 and 3 from the dynamic2
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 3); // assign the target created when paused
        // create third dynamic group
        rolloutHandler.handleAll();
        assertThat(rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id"))
        ).getContent()).hasSize(amountGroups + 3);
        executeAllFromGroup(rollout, dynamic1, 1);
        executeAllFromGroup(rollout, dynamic2, 3);
        assertAndGetRunning(rollout, 0);
        // create third dynamic group
        rolloutHandler.handleAll();
        assertThat(refresh(dynamic2).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
    }


    /**
     * Verifies a simple dynamic rollout flow with a dynamic group template with default {@link RolloutGroupSuccessAction#NEXTGROUP} success action
     */
    @SneakyThrows
    @Test
    void dynamicDefaultRolloutTemplateFlow() {
        dynamicRolloutTemplateFlow(RolloutGroup.RolloutGroupSuccessAction.NEXTGROUP);
    }

    /**
     * Verifies a simple dynamic rollout flow with a dynamic group template with {@link RolloutGroupSuccessAction#PAUSE} success action
     */
    @SneakyThrows
    @Test
    void dynamicPauseRolloutTemplateFlow() {
        dynamicRolloutTemplateFlow(RolloutGroup.RolloutGroupSuccessAction.PAUSE);
    }

    void dynamicRolloutTemplateFlow(final RolloutGroup.RolloutGroupSuccessAction successAction) throws Exception {
        final String rolloutName = "dynamic-template-rollout-std";
        final int amountGroups = 3; // static only
        final String targetPrefix = "controller-template-dynamic-rollout-std-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSetLocked("dsFor" + rolloutName);

        // create rollout with amountGroups static groups * 3 targets and dynamic group template with 6 targets
        testdataFactory.createTargets(targetPrefix, 0, amountGroups * 3);
        final Rollout rollout = callAs(
                withUser("dynamicRolloutTemplateFlow", "READ_DISTRIBUTION_SET", "READ_TARGET", "READ_ROLLOUT", "CREATE_ROLLOUT"),
                () -> testdataFactory.createRolloutByVariables(rolloutName, rolloutName, amountGroups,
                        "controllerid==" + targetPrefix + "*", distributionSet, "60", successAction,"30",
                        Action.ActionType.FORCED, 1000, false, true,
                        RolloutManagement.DynamicRolloutGroupTemplate.builder().nameSuffix("-dyn").targetCount(6).build()));

        // rollout is READY, amountGroups + 1 (dynamic) rollout groups and amountGroups * 3 targets in static groups
        assertRollout(rollout, true, RolloutStatus.READY, amountGroups + 1, amountGroups * 3);
        List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id"))).getContent();
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

        executeStaticWithoutOneTargetFromTheLastGroupAndHandleAll(groups, rollout, amountGroups, successAction);

        // partially fill the first dynamic (it is running and now create actions for 4 targets)
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 1, amountGroups * 3 + 4);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 4);

        // fill first (2) and create fill partially the second (+2 new)
        testdataFactory.createTargets(targetPrefix, amountGroups * 3 + 4, 4);
        rolloutHandler.handleAll(); // fill first dynamic group
        rolloutHandler.handleAll(); // and create a new dynamic2
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 6);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        groups = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id"))).getContent();
        final RolloutGroup dynamic2 = groups.get(amountGroups + 1);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 0);

        // create scheduled actions for the dynamic2
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 8);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 2);
        assertAndGetRunning(rollout, 7); // one from the last static group and 6 from the first dynamic
        assertScheduled(rollout, 2);

        // executes last from static and dynamic1 without 1 target
        assertAndGetRunning(rollout, 7)// one from the last static and 6 for the first dynamic
                .stream()
                // filters for action of the last static group
                .filter(action -> Integer.parseInt(action.getTarget().getControllerId().substring(targetPrefix.length())) < amountGroups * 3)
                .forEach(this::finishAction);
        executeWithoutOneTargetFromAGroup(dynamic1, rollout, 6);
        assertAndGetRunning(rollout, 1); // remains on in the first dynamic

        if (successAction == RolloutGroupSuccessAction.PAUSE) {
            // let success pause action run
            rolloutHandler.handleAll();
            // external resume rollout
            rolloutManagement.resumeRollout(rollout.getId());
        }
        // start next group
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 8);
        assertGroup(groups.get(amountGroups - 1), false, RolloutGroupStatus.FINISHED, 3);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        // first dynamic threshold is reached, second is started
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 2);
        assertAndGetRunning(rollout, 3);

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

    /**
     * Verifies a simple pure (no static groups) dynamic rollout flow with a dynamic group template with default {@link RolloutGroupSuccessAction#NEXTGROUP} success action
     */
    @SneakyThrows
    @Test
    void dynamicDefaultRolloutPureFlow() {
        dynamicRolloutPureFlow(RolloutGroup.RolloutGroupSuccessAction.NEXTGROUP);
    }

    /**
     * Verifies a simple pure (no static groups) dynamic rollout flow with a dynamic group template with {@link RolloutGroupSuccessAction#PAUSE} success action
     */
    @SneakyThrows
    @Test
    void dynamicPauseRolloutPureFlow() {
        dynamicRolloutPureFlow(RolloutGroup.RolloutGroupSuccessAction.PAUSE);
    }

    void dynamicRolloutPureFlow(final RolloutGroup.RolloutGroupSuccessAction successAction) throws Exception {
        final String rolloutName = "pure-dynamic-rollout-std";
        final String targetPrefix = "controller-pure-dynamic-rollout-std-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSetLocked("dsFor" + rolloutName);

        final Rollout rollout = callAs(
                withUser("dynamicRolloutPureFlow", "READ_DISTRIBUTION_SET", "READ_TARGET", "READ_ROLLOUT", "CREATE_ROLLOUT"),
                () -> testdataFactory.createRolloutByVariables(rolloutName, rolloutName, 0,
                        "controllerid==" + targetPrefix + "*", distributionSet, "60", successAction,"30",
                        Action.ActionType.FORCED, 1000, false, true,
                        RolloutManagement.DynamicRolloutGroupTemplate.builder().nameSuffix("-dyn").targetCount(6).build()));

        // rollout is READY, amountGroups + 1 (dynamic) rollout groups and amountGroups * 3 targets in static groups
        assertRollout(rollout, true, RolloutStatus.READY, 1, 0);
        List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id"))
        ).getContent();
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
        rolloutHandler.handleAll(); // fill first dynamic group
        rolloutHandler.handleAll(); // and create a new dynamic2
        assertRollout(rollout, true, RolloutStatus.RUNNING, 2, 6);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        groups = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id"))).getContent();
        final RolloutGroup dynamic2 = groups.get(1);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 0);

        // create scheduled actions for the dynamic2
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, 2, 8);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 2);
        assertAndGetRunning(rollout, 6); // 6 from the first dynamic
        assertScheduled(rollout, 2);

        // executes dynamic1 without 1 target
        executeWithoutOneTargetFromAGroup(dynamic1, rollout, 6);
        assertAndGetRunning(rollout, 1); // remains on in the first dynamic

        if (successAction == RolloutGroupSuccessAction.PAUSE) {
            // let success pause action run
            rolloutHandler.handleAll();
            // external resume rollout
            rolloutManagement.resumeRollout(rollout.getId());
        }
        // start next group
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, 2, 8);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 6);
        // first dynamic threshold is reached, second is started
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 2);
        assertAndGetRunning(rollout, 3);

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

    /**
     * Verifies a simple rollout flow with {@link RolloutGroupSuccessAction#NEXTGROUP} success action
     */
    @SneakyThrows
    @Test
    void rolloutDefault0ThresholdFlow() {
        rollout0ThresholdFlow(RolloutGroupSuccessAction.NEXTGROUP);
    }

    /**
     * Verifies a simple rollout flow with {@link RolloutGroupSuccessAction#PAUSE} success action
     */
    @SneakyThrows
    @Test
    void rolloutPause0ThresholdFlow() {
        rollout0ThresholdFlow(RolloutGroupSuccessAction.PAUSE);
    }

    void rollout0ThresholdFlow(final RolloutGroup.RolloutGroupSuccessAction successAction) throws Exception {
        final String rolloutName = "rollout-std-0threshold";
        final int amountGroups = 5; // static only
        final String targetPrefix = "controller-rollout-std-0threshold-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSetLocked("dsFor" + rolloutName);

        testdataFactory.createTargets(targetPrefix, 0, amountGroups * 3);
        final Rollout rollout = callAs(
                withUser("rollout0ThresholdFlow", "READ_DISTRIBUTION_SET", "READ_TARGET", "READ_ROLLOUT", "CREATE_ROLLOUT"),
                () -> testdataFactory.createRolloutByVariables(rolloutName, rolloutName, amountGroups,
                        "controllerid==" + targetPrefix + "*", distributionSet, "0", successAction, "25", false, false));
        final List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id"))).getContent();

        // start rollout
        rolloutManagement.start(rollout.getId());

        // handleStartingRollout (no handleRunning called yet)
        rolloutHandler.handleAll();
        assertRollout(rollout, false, RolloutStatus.RUNNING, amountGroups, amountGroups * 3);
        for (int step = 1; step <= amountGroups; step++) {
            for (int i = 0; i < amountGroups; i++) {
                assertGroup(groups.get(i), false, i < step ? RolloutGroupStatus.RUNNING : RolloutGroupStatus.SCHEDULED, 3);
            }

            // expect all groups without last to trigger PAUSE action
            if (step < amountGroups && successAction == RolloutGroupSuccessAction.PAUSE) {
                rolloutHandler.handleAll();
                rolloutManagement.resumeRollout(rollout.getId());
            }
            // starting the next group
            rolloutHandler.handleAll();
        }
    }

    private void executeStaticWithoutOneTargetFromTheLastGroupAndHandleAll(
            final List<RolloutGroup> groups,
            final Rollout rollout, final int amountGroups, final RolloutGroupSuccessAction successAction) {
        // create dynamic group if needed
        rolloutHandler.handleAll();
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
            if (i + 1 < groups.size() && successAction == RolloutGroupSuccessAction.PAUSE) {
                rolloutManagement.resumeRollout(rollout.getId());
                rolloutHandler.handleAll();
            }
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

    // count of GROUP running actions
    private void executeAllFromGroup(final Rollout rollout, final RolloutGroup group, final int count) {
        final List<JpaAction> running =
                actionRepository.findByRolloutIdAndStatus(PAGE, rollout.getId(), Action.Status.RUNNING).getContent()
                        .stream().filter(action -> action.getRolloutGroup().getId().equals(group.getId())).toList();
        if (count >= 0) {
            assertThat(running).as("Action count").hasSize(count);
        }
        running.forEach(this::finishAction);
    }

    // count of GROUP running actions
    private void executeWithoutOneTargetFromAGroup(final RolloutGroup group, final Rollout rollout, final int count) {
        // execute groups (without on of the last)
        assertThat(refresh(group).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        final List<JpaAction> running =
                actionRepository.findByRolloutIdAndStatus(PAGE, rollout.getId(), Action.Status.RUNNING).getContent()
                        .stream().filter(action -> action.getRolloutGroup().getId().equals(group.getId())).toList();
        if (count >= 0) {
            assertThat(running).as("Action count").hasSize(count);
        }

        // skip on from the last group only
        final AtomicBoolean skipOne = new AtomicBoolean(true);
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

        assertThat(skipOne.get()).as("One action should be skipped").isFalse();
    }
}