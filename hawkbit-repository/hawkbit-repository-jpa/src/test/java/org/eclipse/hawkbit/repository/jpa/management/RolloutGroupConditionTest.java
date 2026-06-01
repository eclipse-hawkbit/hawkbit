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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "hawkbit.server.repository.dynamicRolloutsMinInvolvePeriodMS=-1" })
class RolloutGroupConditionTest extends AbstractJpaIntegrationTest {

    @BeforeEach
    void reset() {
        this.approvalStrategy.setApprovalNeeded(false);
    }

    @SneakyThrows
    @Test
    void canceledActionCountedAsError() {
        final String targetPrefix = "bug3cancel";
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("bug3cancelDs");
        testdataFactory.createTargets(targetPrefix, 5);

        final Rollout rollout = callAs(
                withUser("bug3User", "READ_DISTRIBUTION_SET", "READ_TARGET", "READ_ROLLOUT", "CREATE_ROLLOUT"),
                () -> testdataFactory.createRolloutByVariables("bug3cancelRollout", "bug3", 1,
                        "controllerid==" + targetPrefix + "*", ds, "100", "10", null, null, false));

        final RolloutGroup group = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id"))).getContent().get(0);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<JpaAction> running = assertAndGetRunning(rollout, 5).getContent();

        running.subList(0, 4).forEach(this::finishAction);
        setCanceled(running.get(4));

        rolloutHandler.handleAll();

        assertRollout(rollout, false, RolloutStatus.PAUSED, 1, 5);
        assertGroup(group, false, RolloutGroupStatus.ERROR, 5);
    }

    @SneakyThrows
    @Test
    void groupCompleteWithSubthresholdErrorsFinishes() {
        final String targetPrefix = "bug1complete";
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("bug1completeDs");
        testdataFactory.createTargets(targetPrefix, 5);

        final Rollout rollout = callAs(
                withUser("bug1User", "READ_DISTRIBUTION_SET", "READ_TARGET", "READ_ROLLOUT", "CREATE_ROLLOUT"),
                () -> testdataFactory.createRolloutByVariables("bug1completeRollout", "bug1", 1,
                        "controllerid==" + targetPrefix + "*", ds, "100", "50", null, null, false));

        final RolloutGroup group = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id"))).getContent().get(0);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<JpaAction> running = assertAndGetRunning(rollout, 5).getContent();

        // 3 FINISHED (60%) + 2 ERROR (40%) — error below 50% threshold, no error action fires
        running.subList(0, 3).forEach(this::finishAction);
        running.subList(3, 5).forEach(this::reportError);

        rolloutHandler.handleAll();

        // group complete, error condition not breached → success action (NEXTGROUP) → FINISHED
        assertGroup(group, false, RolloutGroupStatus.FINISHED, 5);
        assertRollout(rollout, false, RolloutStatus.FINISHED, 1, 5);
    }

    @SneakyThrows
    @Test
    void deletedActionUpdatesCountDenominatorAndSuccessFires() {
        final String targetPrefix = "bug2denom";
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("bug2denomDs");
        testdataFactory.createTargets(targetPrefix, 5);

        final Rollout rollout = callAs(
                withUser("bug2User", "READ_DISTRIBUTION_SET", "READ_TARGET", "READ_ROLLOUT", "CREATE_ROLLOUT"),
                () -> testdataFactory.createRolloutByVariables("bug2denomRollout", "bug2", 1,
                        "controllerid==" + targetPrefix + "*", ds, "100", "10", null, null, false));

        final RolloutGroup group = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id"))).getContent().get(0);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<JpaAction> running = assertAndGetRunning(rollout, 5).getContent();

        // delete one action directly — simulates external action removal
        actionRepository.deleteById(running.get(4).getId());
        // finish the remaining 4
        running.subList(0, 4).forEach(this::finishAction);

        rolloutHandler.handleAll();

        // totalTargets must be recalculated to 4; success=4/4=100% → FINISHED
        assertGroup(group, false, RolloutGroupStatus.FINISHED, 4);
        assertRollout(rollout, false, RolloutStatus.FINISHED, 1, 4);
    }

    @SneakyThrows
    @Test
    void deletedTargetUpdatesErrorThresholdDenominator() {
        final String targetPrefix = "delTargetErr";
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("delTargetErrDs");
        testdataFactory.createTargets(targetPrefix, 5);

        final Rollout rollout = callAs(
                withUser("delTargetUser", "READ_DISTRIBUTION_SET", "READ_TARGET", "READ_ROLLOUT", "CREATE_ROLLOUT"),
                () -> testdataFactory.createRolloutByVariables("delTargetErrRollout", "delTarget", 1,
                        "controllerid==" + targetPrefix + "*", ds, "100", "20", null, null, false));

        final RolloutGroup group = rolloutGroupManagement.findByRollout(
                rollout.getId(), new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id"))).getContent().get(0);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<JpaAction> running = assertAndGetRunning(rollout, 5).getContent();

        // delete target at index 4 — cascades to action deletion, denominator drops from 5 to 4
        targetManagement.delete(List.of(running.get(4).getTarget().getId()));
        // finish 3, report error on 1 → error=1/4=25% > 20%
        running.subList(0, 3).forEach(this::finishAction);
        reportError(running.get(3));

        rolloutHandler.handleAll();

        // correct denominator=4: 1/4=25% > 20% → error fires → PAUSED
        assertRollout(rollout, false, RolloutStatus.PAUSED, 1, 4);
        assertGroup(group, false, RolloutGroupStatus.ERROR, 4);
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
