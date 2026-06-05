/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.callAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withUser;

import java.util.List;

import lombok.SneakyThrows;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
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
 * Integration tests for rollout group condition evaluation — group cascade behavior.
 *
 * Every test uses a 2-group rollout so both groups' final state is verified:
 *   - group 1: the group under test
 *   - group 2: verifies cascade (SCHEDULED = did not start, RUNNING = was triggered)
 *
 * Success-action tests use PAUSE: PauseRolloutGroupSuccessAction only fires when a
 * scheduled child group exists, making success-action execution observable as
 * RolloutStatus.PAUSED without advancing to group 2.
 *
 * Decision-log rules under test:
 *   - Success Action fires on SuccessCondition OR GroupFINISHED
 *   - CANCEL not mapped to Error (ignored in condition evaluation)
 *   - Target's Action count used as Group Target count
 */
@TestPropertySource(properties = { "hawkbit.server.repository.dynamicRolloutsMinInvolvePeriodMS=-1" })
class RolloutGroupConditionTest extends AbstractJpaIntegrationTest {

    @BeforeEach
    void reset() {
        this.approvalStrategy.setApprovalNeeded(false);
    }

    // -----------------------------------------------------------------------
    // CANCELED ignored; group completion triggers success action
    // -----------------------------------------------------------------------

    @SneakyThrows
    @Test
    void canceledActionIgnoredGroupCompletionTriggersSuccessAction() {
        final String targetPrefix = "cancelTarget";
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("cancelTargetDs");
        testdataFactory.createTargets(targetPrefix, 10);

        final Rollout rollout = rolloutWith2Groups(
                "cancelTargetRollout", targetPrefix, ds, "100", RolloutGroupSuccessAction.PAUSE, "10");
        final List<RolloutGroup> groups = getGroups(rollout);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<JpaAction> running = assertAndGetRunning(rollout, 5).getContent();
        running.subList(0, 4).forEach(this::finishAction);
        setCanceled(running.get(4));

        rolloutHandler.handleAll();

        assertGroup(groups.get(0), false, RolloutGroupStatus.FINISHED, 5);
        assertGroup(groups.get(1), false, RolloutGroupStatus.SCHEDULED, 5);
        assertRollout(rollout, false, RolloutStatus.PAUSED, 2, 10);
    }

    @SneakyThrows
    @Test
    void groupCompleteNeitherConditionFulfilledTriggersSuccessAction() {
        final String targetPrefix = "subthreshold";
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("subthresholdDs");
        testdataFactory.createTargets(targetPrefix, 10);

        final Rollout rollout = rolloutWith2Groups(
                "subthreshRollout", targetPrefix, ds, "100", RolloutGroupSuccessAction.PAUSE, "50");
        final List<RolloutGroup> groups = getGroups(rollout);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<JpaAction> running = assertAndGetRunning(rollout, 5).getContent();
        // 3/5 SUCCESS, 2/5 ERRORS, neither condition fulfilled (S:100, E:50)
        // but Group Finishes 5/5 -> execute SuccessAction#PAUSE
        running.subList(0, 3).forEach(this::finishAction);
        running.subList(3, 5).forEach(this::reportError);

        rolloutHandler.handleAll();

        assertGroup(groups.get(0), false, RolloutGroupStatus.FINISHED, 5);
        assertGroup(groups.get(1), false, RolloutGroupStatus.SCHEDULED, 5);
        assertRollout(rollout, false, RolloutStatus.PAUSED, 2, 10);
    }

    @SneakyThrows
    @Test
    void successConditionMetBeforeGroupFinishedTriggersSuccessAction() {
        final String targetPrefix = "scBefore";
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("scBeforeDs");
        testdataFactory.createTargets(targetPrefix, 10);

        final Rollout rollout = rolloutWith2Groups(
                "scBeforeRollout", targetPrefix, ds, "60", RolloutGroupSuccessAction.PAUSE, "90");
        final List<RolloutGroup> groups = getGroups(rollout);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<JpaAction> group1Actions = assertAndGetRunning(rollout, 5).getContent();
        // 3/5 SUCCESS, 2/5 RUNNING -> fulfill 60% SuccessCondition => Trigger SuccessAction#PAUSE
        group1Actions.subList(0, 3).forEach(this::finishAction);

        rolloutHandler.handleAll();

        assertGroup(groups.get(0), false, RolloutGroupStatus.RUNNING, 5);
        assertGroup(groups.get(1), false, RolloutGroupStatus.SCHEDULED, 5);
        assertRollout(rollout, false, RolloutStatus.PAUSED, 2, 10);
    }

    @SneakyThrows
    @Test
    void deletedActionUpdatesGroupCountAndSuccessFires() {
        final String targetPrefix = "denomSuccess";
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("denomSuccessDs");
        testdataFactory.createTargets(targetPrefix, 10);

        final Rollout rollout = rolloutWith2Groups(
                "denomSuccessRollout", targetPrefix, ds, "100", RolloutGroupSuccessAction.PAUSE, "10");
        final List<RolloutGroup> groups = getGroups(rollout);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<JpaAction> running = assertAndGetRunning(rollout, 5).getContent();
        // 4/5 SUCCESS, 1 DELETE -> 4/4 SUCCESS fulfill SuccessCondition 100% => Trigger SuccessAction#PAUSE
        actionRepository.deleteById(running.get(4).getId());
        running.subList(0, 4).forEach(this::finishAction);

        rolloutHandler.handleAll();

        assertGroup(groups.get(0), false, RolloutGroupStatus.FINISHED, 4);
        assertGroup(groups.get(1), false, RolloutGroupStatus.SCHEDULED, 5);
        assertRollout(rollout, false, RolloutStatus.PAUSED, 2, 9);
    }

    @SneakyThrows
    @Test
    void deletedActionUpdatesErrorDenominator() {
        final String targetPrefix = "denomError";
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("denomErrorDs");
        testdataFactory.createTargets(targetPrefix, 10);

        final Rollout rollout = rolloutWith2Groups(
                "denomErrorRollout", targetPrefix, ds, "100", RolloutGroupSuccessAction.PAUSE, "23");
        final List<RolloutGroup> groups = getGroups(rollout);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<JpaAction> running = assertAndGetRunning(rollout, 5).getContent();
        // 3/5 SUCCESS, 1 DELETE -> 3/4 SUCCESS, 1/4 ERROR fulfill ErrorCondition >23% => Trigger ErrorAction#PAUSE
        actionRepository.deleteById(running.get(4).getId());
        running.subList(0, 3).forEach(this::finishAction);
        reportError(running.get(3));

        rolloutHandler.handleAll();

        assertGroup(groups.get(0), false, RolloutGroupStatus.ERROR, 4);
        assertGroup(groups.get(1), false, RolloutGroupStatus.SCHEDULED, 5);
        assertRollout(rollout, false, RolloutStatus.PAUSED, 2, 9);
    }

    private Rollout rolloutWith2Groups(
            final String name, final String targetPrefix, final DistributionSet ds,
            final String successCondition, final RolloutGroupSuccessAction successAction,
            final String errorCondition) throws Exception {
        return callAs(
                withUser(name + "User", "READ_DISTRIBUTION_SET", "READ_TARGET", "READ_ROLLOUT", "CREATE_ROLLOUT"),
                () -> testdataFactory.createRolloutByVariables(name, name, 2,
                        "controllerid==" + targetPrefix + "*", ds,
                        successCondition, successAction, errorCondition, null, null, false, false));
    }

    private List<RolloutGroup> getGroups(final Rollout rollout) {
        return rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id"))).getContent();
    }

    private void setCanceled(final JpaAction action) {
        action.setStatus(Action.Status.CANCELED);
        action.setActive(false);
        actionRepository.save(action);
    }

    private void reportError(final Action action) {
        controllerManagement.addUpdateActionStatus(
                ActionStatusCreate.builder().actionId(action.getId()).status(Action.Status.ERROR).build());
    }
}
