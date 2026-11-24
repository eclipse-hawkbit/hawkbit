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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.repository.test.util.ConflictInjection;
import org.eclipse.hawkbit.repository.test.util.QueryCount;
import org.eclipse.hawkbit.repository.test.util.QueryCount.Kind;
import org.eclipse.hawkbit.repository.test.util.QueryCountConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Rollout execution issues a number of statements that doesn't depend on the number of targets / actions processed. Each
 * scenario runs for 10 and for 50 targets and compares the counts. The tenant configuration reads are excluded, as the
 * tenant configuration cache is disabled in tests.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Rollout Execution - bounded statements
 */
// groups of 50 targets exceed the test default of the targets per rollout group quota
@TestPropertySource(properties = "hawkbit.server.security.dos.maxTargetsPerRolloutGroup=100")
@Import(QueryCountConfiguration.class)
class RolloutExecutionStatementsTest extends AbstractJpaIntegrationTest {

    private static final String TENANT_CONFIGURATION = "sp_tenant_configuration";
    // a SELECT with COUNT on sp_action (not on sp_action_status etc.), also grouped
    private static final Pattern ACTION_COUNT = Pattern.compile("(?is)^\\s*select\\b.*\\bcount\\(.*\\bfrom\\s+sp_action\\b");

    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private QueryCount queryCount;

    /**
     * Verifies that starting the first group issues the same number of SELECTs for 10 and for 50 targets, every target
     * with an active manual action.
     */
    @Test
    void groupStartSelectCountIndependentOfGroupSize() {
        final QueryCount.Snapshot small = groupStartStatements("group-start-10-", 10);
        final QueryCount.Snapshot large = groupStartStatements("group-start-50-", 50);

        assertThat(large.countExcluding(Kind.SELECT, TENANT_CONFIGURATION))
                .as("SELECTs 10: %s, 50: %s", small, large)
                .isEqualTo(small.countExcluding(Kind.SELECT, TENANT_CONFIGURATION));
    }

    /**
     * Verifies that an action of a running child group that is still {@code SCHEDULED} (e.g. left by a concurrency failure
     * on the group start) is started by a later scheduler run.
     */
    @Test
    void actionLeftScheduledByConflictIsStartedLater() {
        final Rollout rollout = runningRolloutWithSecondGroupStarted("left-scheduled-");
        final RolloutGroup secondGroup = rolloutGroupManagement.findByRollout(rollout.getId(), PAGE).getContent().get(1);
        assertThat(refresh(secondGroup).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        final Action left = findActionsByRolloutAndStatus(rollout, Status.RUNNING).stream()
                .filter(action -> action.getRolloutGroup().getId().equals(secondGroup.getId()))
                .findFirst().orElseThrow();
        // the state a failed start leaves: the action is still scheduled and the target has no assignment
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            final JpaAction action = actionRepository.findById(left.getId()).orElseThrow();
            action.setStatus(Status.SCHEDULED);
            action.setActive(false);
            actionRepository.save(action);
            final JpaTarget target = targetRepository.findById(left.getTarget().getId()).orElseThrow();
            target.setAssignedDistributionSet(null);
            targetRepository.save(target);
        });

        rolloutHandler.handleAll();

        final JpaAction started = actionRepository.findById(left.getId()).orElseThrow();
        assertThat(started.getStatus()).isEqualTo(Status.RUNNING);
        assertThat(started.isActive()).isTrue();
        assertThat(targetRepository.findById(left.getTarget().getId()).orElseThrow().getAssignedDistributionSet().getId())
                .isEqualTo(rollout.getDistributionSet().getId());
    }

    /**
     * Verifies that creating the actions of a group of 10 and of 50 targets issues the same number of action count
     * statements (actions per target quota).
     */
    @Test
    void quotaCheckCountIndependentOfGroupSize() {
        final long small = actionCreationStatements("quota-count-10-", 10).countMatching(ACTION_COUNT);
        final long large = actionCreationStatements("quota-count-50-", 50).countMatching(ACTION_COUNT);

        assertThat(large).as("action count statements 10: %s, 50: %s", small, large).isEqualTo(small);
    }

    /**
     * Verifies that a target that reached the maximum actions per target quota still gets the quota handling on action
     * creation (here: the oldest actions are purged), and the other targets of the group are not affected.
     */
    @Test
    void quotaStillEnforcedPerTarget() {
        final String prefix = "quota-enforced-";
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, 5);
        final Target atQuota = targets.get(0);
        final int maxActions = quotaManagement.getMaxActionsPerTarget();
        final List<DistributionSet> sets = List.of(
                testdataFactory.createDistributionSetLocked(prefix + "a"), testdataFactory.createDistributionSetLocked(prefix + "b"));
        for (int i = 0; i < maxActions; i++) {
            assignDistributionSet(sets.get(i % 2).getId(), atQuota.getControllerId());
        }
        assertThat(deploymentManagement.countActionsByTarget(atQuota.getControllerId())).isEqualTo(maxActions);
        // purge 25 % of the quota when hit
        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.ACTION_CLEANUP_ON_QUOTA_HIT_PERCENTAGE, 25);
        final Rollout rollout = createRollout(prefix);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        assertThat(findActionsByRolloutAndStatus(rollout, Status.RUNNING)).hasSize(5);
        // the oldest 25 % purged, then the rollout action added
        assertThat(deploymentManagement.countActionsByTarget(atQuota.getControllerId())).isEqualTo(maxActions - maxActions / 4 + 1);
        for (final Target other : targets.subList(1, targets.size())) {
            assertThat(deploymentManagement.countActionsByTarget(other.getControllerId())).isEqualTo(1);
        }
    }

    // statements of the group scheduling, i.e. the action creation (from the read of the groups to schedule to the group start)
    private Measured actionCreationStatements(final String prefix, final int targetCount) {
        testdataFactory.createTargets(prefix, 0, targetCount);
        final Rollout rollout = createRollout(prefix);
        rolloutManagement.start(rollout.getId());
        waitForEvents();

        final Measured measured = measure(
                rolloutGroupRepository, "findByRolloutAndStatus",
                rolloutGroupRepository, "findByRolloutOrderByIdAsc",
                rolloutHandler::handleAll);

        assertThat(findActionsByRolloutAndStatus(rollout, Status.RUNNING)).hasSize(targetCount);
        return measured;
    }

    /**
     * Verifies that filling a group of 10 and a group of 50 targets issues the same number of SELECTs.
     */
    @Test
    void groupFillWithoutPerTargetReads() {
        final QueryCount.Snapshot small = groupFillStatements("group-fill-10-", 10);
        final QueryCount.Snapshot large = groupFillStatements("group-fill-50-", 50);

        assertThat(large.countExcluding(Kind.SELECT, TENANT_CONFIGURATION))
                .as("SELECTs 10: %s, 50: %s", small, large)
                .isEqualTo(small.countExcluding(Kind.SELECT, TENANT_CONFIGURATION));
        assertThat(small.count(Kind.INSERT, "sp_rollout_target_group")).isPositive();
    }

    // statements of the group fill on the rollout creation (from the count of the group's targets to the save of the READY rollout).
    // Not up to the save of the filled group: that is in the transaction of the last page, the inserts are flushed on its commit
    private QueryCount.Snapshot groupFillStatements(final String prefix, final int targetCount) {
        testdataFactory.createTargets(prefix, 0, targetCount);
        final AtomicReference<Rollout> rollout = new AtomicReference<>();

        final QueryCount.Snapshot snapshot = measure(
                rolloutTargetGroupRepository, "countByRolloutGroup",
                rolloutRepository, "save",
                () -> rollout.set(createRollout(prefix))).snapshot();

        assertThat(rollout.get().getTotalTargets()).isEqualTo(targetCount);
        return snapshot;
    }

    /**
     * Verifies that deleting the scheduled actions of a stopped rollout issues the same number of SELECTs for 10 and for 50
     * scheduled actions.
     */
    @Test
    void deleteScheduledActionsWithoutPerActionReads() {
        final QueryCount.Snapshot small = deleteScheduledStatements("delete-scheduled-10-", 10);
        final QueryCount.Snapshot large = deleteScheduledStatements("delete-scheduled-50-", 50);

        assertThat(large.countExcluding(Kind.SELECT, TENANT_CONFIGURATION))
                .as("SELECTs 10: %s, 50: %s", small, large)
                .isEqualTo(small.countExcluding(Kind.SELECT, TENANT_CONFIGURATION));
    }

    // statements of the stop handling up to the deletion of the scheduled actions, for a rollout with a running first group and a
    // second group of scheduled actions, each of the given size; the rollout is STOPPED afterward, so it isn't handled again
    private QueryCount.Snapshot deleteScheduledStatements(final String prefix, final int groupSize) {
        testdataFactory.createTargets(prefix, 0, 2 * groupSize);
        final DistributionSet ds = testdataFactory.createDistributionSetLocked(prefix + "rollout");
        final Rollout rollout = testdataFactory.createRolloutByVariables(
                prefix, prefix, 2, "controllerid==" + prefix + "*", ds, "100", "100");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        assertThat(findActionsByRolloutAndStatus(rollout, Status.SCHEDULED)).hasSize(groupSize);
        rolloutManagement.stop(rollout.getId());
        waitForEvents();

        final QueryCount.Snapshot snapshot = measure(
                rolloutManagement, "getAccessControlContext",
                actionRepository, "countByRolloutIdAndStatus",
                rolloutHandler::handleAll).snapshot();

        assertThat(findActionsByRolloutAndStatus(rollout, Status.SCHEDULED)).isEmpty();
        // the device finishes the cancelled actions, so that the rollout gets STOPPED
        findActionsByRolloutAndStatus(rollout, Status.CANCELING).forEach(this::finishAction);
        rolloutHandler.handleAll();
        assertThat(rolloutManagement.get(rollout.getId()).getStatus()).isEqualTo(RolloutStatus.STOPPED);
        return snapshot;
    }

    // a rollout with two groups of 5 targets: the first group met its success condition (3 of 5 finished, not complete) and
    // the second group is running
    private Rollout runningRolloutWithSecondGroupStarted(final String prefix) {
        testdataFactory.createTargets(prefix, 0, 10);
        final DistributionSet ds = testdataFactory.createDistributionSetLocked(prefix + "rollout");
        final Rollout rollout = testdataFactory.createRolloutByVariables(
                prefix, prefix, 2, "controllerid==" + prefix + "*", ds, "50", "80");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final List<Action> firstGroupActions = findActionsByRolloutAndStatus(rollout, Status.RUNNING);
        assertThat(firstGroupActions).hasSize(5);
        firstGroupActions.subList(0, 3).forEach(this::finishAction);
        rolloutHandler.handleAll();
        assertThat(findActionsByRolloutAndStatus(rollout, Status.RUNNING)).hasSize(7);
        waitForEvents();
        return rolloutManagement.get(rollout.getId());
    }

    // statements of the first group start (from the read of the groups to the end of the start of the scheduled actions)
    private QueryCount.Snapshot groupStartStatements(final String prefix, final int targetCount) {
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, targetCount);
        final DistributionSet manualSet = testdataFactory.createDistributionSetLocked(prefix + "manual");
        // in chunks, below the assignments per request quota
        ListUtils.partition(targets.stream().map(Target::getControllerId).toList(), 10)
                .forEach(controllerIds -> assignDistributionSet(manualSet.getId(), controllerIds, ActionType.FORCED));
        final Rollout rollout = createRollout(prefix);
        rolloutManagement.start(rollout.getId());
        waitForEvents();

        final QueryCount.Snapshot snapshot = measure(
                rolloutGroupRepository, "findByRolloutOrderByIdAsc",
                deploymentManagement, "startScheduledActionsByRolloutGroupParent",
                rolloutHandler::handleAll).snapshot();

        assertThat(findActionsByRolloutAndStatus(rollout, Status.RUNNING)).hasSize(targetCount);
        return snapshot;
    }

    /**
     * Verifies that the soft cancel of the active actions of a rollout (rollout stop) issues the same number of SELECTs for
     * 10 and for 50 actions, i.e. the targets are not read per action and there is no COUNT per page.
     */
    @Test
    void softCancelSelectCountIndependentOfActionCount() {
        final QueryCount.Snapshot small = cancelStatements("soft-cancel-10-", 10, ActionCancellationType.SOFT, Status.CANCELING);
        final QueryCount.Snapshot large = cancelStatements("soft-cancel-50-", 50, ActionCancellationType.SOFT, Status.CANCELING);

        assertThat(large.countExcluding(Kind.SELECT, TENANT_CONFIGURATION))
                .as("SELECTs 10: %s, 50: %s", small, large)
                .isEqualTo(small.countExcluding(Kind.SELECT, TENANT_CONFIGURATION));
    }

    /**
     * Verifies that the force quit of the active actions of a rollout (rollout delete) issues the same number of SELECTs for
     * 10 and for 50 actions.
     */
    @Test
    void forceQuitSelectCountIndependentOfActionCount() {
        final QueryCount.Snapshot small = cancelStatements("force-quit-10-", 10, ActionCancellationType.FORCE, Status.CANCELED);
        final QueryCount.Snapshot large = cancelStatements("force-quit-50-", 50, ActionCancellationType.FORCE, Status.CANCELED);

        assertThat(large.countExcluding(Kind.SELECT, TENANT_CONFIGURATION))
                .as("SELECTs 10: %s, 50: %s", small, large)
                .isEqualTo(small.countExcluding(Kind.SELECT, TENANT_CONFIGURATION));
    }

    // statements of the cancel of the active actions of a started rollout over the given number of new targets
    private QueryCount.Snapshot cancelStatements(
            final String prefix, final int targetCount, final ActionCancellationType type, final Status expectedStatus) {
        testdataFactory.createTargets(prefix, 0, targetCount);
        final Rollout rollout = startRollout(prefix, targetCount);

        queryCount.resetQueries();
        rolloutManagement.cancelActiveActionsForRollouts(rollout, type);
        final QueryCount.Snapshot snapshot = queryCount.snapshot();
        waitForEvents();

        assertThat(findActionsByRolloutAndStatus(rollout, expectedStatus)).hasSize(targetCount);
        return snapshot;
    }

    // a started rollout (one group) over the targets with the prefix, all its actions running
    private Rollout startRollout(final String prefix, final int targetCount) {
        final Rollout rollout = createRollout(prefix);
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        assertThat(findActionsByRolloutAndStatus(rollout, Status.RUNNING)).hasSize(targetCount);
        waitForEvents();
        return rolloutManagement.get(rollout.getId());
    }

    private Rollout createRollout(final String prefix) {
        final DistributionSet ds = testdataFactory.createDistributionSetLocked(prefix + "rollout");
        return testdataFactory.createRolloutByVariables(prefix, prefix, 1, "controllerid==" + prefix + "*", ds, "100", "100");
    }

    // records the statements from right after the first call of the start method to right after the first call of the end method
    // that follows it (the end interception is added only when the start method has been called)
    private Measured measure(
            final Object startBean, final String startMethod, final Object endBean, final String endMethod, final Runnable run) {
        final AtomicReference<Measured> measured = new AtomicReference<>();
        final AtomicReference<ConflictInjection> end = new AtomicReference<>();
        try (ConflictInjection ignoredStart = ConflictInjection.afterRead(txManager, startBean, startMethod, () -> {
            queryCount.resetQueries();
            end.set(ConflictInjection.afterRead(txManager, endBean, endMethod,
                    () -> measured.set(new Measured(queryCount.snapshot(), queryCount.getAllStatements()))));
        })) {
            run.run();
        } finally {
            if (end.get() != null) {
                end.get().close();
            }
        }
        waitForEvents();
        assertThat(measured.get()).as("measured section reached").isNotNull();
        return measured.get();
    }

    private record Measured(QueryCount.Snapshot snapshot, List<String> statements) {

        long countMatching(final Pattern pattern) {
            return statements.stream().filter(sql -> pattern.matcher(sql).find()).count();
        }
    }
}
