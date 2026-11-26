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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.ConcurrentModificationException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionProperties;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.EventVerifier;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.ConflictInjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Rollout execution under concurrent device writes. The device write is injected deterministically between the read of
 * the rollout batch and its writes ({@link ConflictInjection}).
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Rollout Execution - concurrency
 */
class RolloutExecutionConcurrencyTest extends AbstractJpaIntegrationTest {

    // the read of the first group's scheduled actions on group start - the batch reads the obsolete actions after it
    private static final String FIRST_GROUP_SCHEDULED_ACTIONS_READ = "findWithTargetByIdIn";

    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private ApplicationEventMulticaster eventMulticaster;
    @Autowired
    private EntityManager entityManager;

    private final List<CancelTargetAssignmentEvent> cancelEvents = new CopyOnWriteArrayList<>();
    private final ApplicationListener<CancelTargetAssignmentEvent> cancelEventListener = cancelEvents::add;
    private final List<TargetAssignDistributionSetEvent> assignEvents = new CopyOnWriteArrayList<>();
    private final ApplicationListener<TargetAssignDistributionSetEvent> assignEventListener = assignEvents::add;

    @BeforeEach
    void registerCancelEventListener() {
        eventMulticaster.addApplicationListener(cancelEventListener);
        eventMulticaster.addApplicationListener(assignEventListener);
    }

    @AfterEach
    void removeCancelEventListener() {
        eventMulticaster.removeApplicationListener(cancelEventListener);
        eventMulticaster.removeApplicationListener(assignEventListener);
    }

    /**
     * Verifies that a manual action the device finishes after the rollout read its batch stays finished, gets no cancel
     * status, and doesn't block the start of the rollout action.
     */
    @Test
    void obsoleteActionFinishedByDeviceDuringGroupStart() {
        final String prefix = "obsolete-finished-";
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, 3);
        final Action manualAction = assignManually(targets.get(0), "obsolete-finished-old");
        final Rollout rollout = createRollout(prefix, "obsolete-finished-new");

        startFirstGroup(rollout, () -> finishAction(manualAction));

        final JpaAction manualAfter = actionRepository.findById(manualAction.getId()).orElseThrow();
        assertThat(manualAfter.getStatus()).isEqualTo(Status.FINISHED);
        assertThat(manualAfter.isActive()).isFalse();
        assertThat(statusesOf(manualAction)).noneMatch(s -> s == Status.CANCELING || s == Status.CANCELED);
        assertRolloutActionsActive(rollout, 3);
    }

    /**
     * Verifies that only the still active obsolete actions are cancelled (one status and one cancellation event each),
     * and that an action the device finished before the cancellation is left unchanged.
     */
    @Test
    void onlyStillActiveActionsAreCancelled() {
        final String prefix = "only-active-";
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, 3);
        final List<Action> manualActions = targets.stream().map(t -> assignManually(t, prefix + t.getControllerId())).toList();
        final Action finishedByDevice = manualActions.get(0);
        final Rollout rollout = createRollout(prefix, "only-active-new");

        startFirstGroup(rollout, () -> finishAction(finishedByDevice));

        final JpaAction finishedAfter = actionRepository.findById(finishedByDevice.getId()).orElseThrow();
        assertThat(finishedAfter.getStatus()).isEqualTo(Status.FINISHED);
        assertThat(statusesOf(finishedByDevice)).noneMatch(s -> s == Status.CANCELING || s == Status.CANCELED);
        assertThat(cancelledActionIdsFor(targets.get(0))).isEmpty();

        for (int i = 1; i < 3; i++) {
            final Action cancelled = manualActions.get(i);
            assertThat(actionRepository.findById(cancelled.getId()).orElseThrow().getStatus()).isEqualTo(Status.CANCELING);
            assertThat(statusesOf(cancelled)).filteredOn(s -> s == Status.CANCELING).hasSize(1);
            assertThat(cancelledActionIdsFor(targets.get(i))).containsExactly(cancelled.getId());
        }
        assertRolloutActionsActive(rollout, 3);
    }

    /**
     * Verifies that a device updating its target after the rollout read its batch doesn't block the group start: all
     * actions get active, all targets get the distribution set assigned, the device change is kept, and the target
     * version changes.
     */
    @Test
    void targetUpdatedByDeviceDuringGroupStart() {
        final String prefix = "target-updated-";
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, 10);
        final Target updatedByDevice = targets.get(0);
        final Rollout rollout = createRollout(prefix, "target-updated-new");
        final int untouchedRevisionBefore = targetRepository.findById(targets.get(1).getId()).orElseThrow().getOptLockRevision();

        startFirstGroup(rollout, () -> updateAddress(updatedByDevice.getId(), "http://device-updated"));

        assertRolloutActionsActive(rollout, 10);
        for (final Target target : targets) {
            final JpaTarget after = targetRepository.findById(target.getId()).orElseThrow();
            assertThat(after.getAssignedDistributionSet().getId()).isEqualTo(rollout.getDistributionSet().getId());
            assertThat(after.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        }
        assertThat(targetRepository.findById(updatedByDevice.getId()).orElseThrow().getAddress()).isEqualTo("http://device-updated");
        assertThat(targetRepository.findById(targets.get(1).getId()).orElseThrow().getOptLockRevision())
                .isNotEqualTo(untouchedRevisionBefore);
    }

    /**
     * Verifies that a device-side save based on a target read before the group start fails with a concurrency failure
     * instead of overwriting the assignment, and that the retry keeps the rollout distribution set assigned.
     */
    @Test
    void staleDeviceUpdateAfterGroupStartFails() {
        final String prefix = "stale-device-";
        final Target target = testdataFactory.createTargets(prefix, 0, 1).get(0);
        final Rollout rollout = createRollout(prefix, "stale-device-new");
        // the device-side transaction reads its target before the group start
        final JpaTarget staleTarget = requiresNew().execute(status -> targetRepository.findById(target.getId()).orElseThrow());

        startFirstGroup(rollout, () -> {});

        staleTarget.setAddress("http://device-stale");
        assertThatExceptionOfType(ConcurrencyFailureException.class)
                .isThrownBy(() -> requiresNew().executeWithoutResult(status -> targetRepository.save(staleTarget)));

        // retry: the device-side transaction reads the target again
        updateAddress(target.getId(), "http://device-retried");
        final JpaTarget after = targetRepository.findById(target.getId()).orElseThrow();
        assertThat(after.getAddress()).isEqualTo("http://device-retried");
        assertThat(after.getAssignedDistributionSet().getId()).isEqualTo(rollout.getDistributionSet().getId());
        assertThat(after.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
    }

    /**
     * Verifies that a device-side save based on a target read before a manual assignment fails with a concurrency failure
     * instead of overwriting the assignment (the assignment bulk update increments the target version on every JPA vendor).
     */
    @Test
    void staleDeviceUpdateAfterManualAssignmentFails() {
        final Target target = testdataFactory.createTargets("stale-manual-", 0, 1).get(0);
        // the device-side transaction reads its target before the assignment
        final JpaTarget staleTarget = requiresNew().execute(_ -> targetRepository.findById(target.getId()).orElseThrow());

        final Action action = assignManually(target, "stale-manual-ds");

        staleTarget.setAddress("http://device-stale");
        assertThatExceptionOfType(ConcurrencyFailureException.class)
                .isThrownBy(() -> requiresNew().executeWithoutResult(_ -> targetRepository.save(staleTarget)));
        final JpaTarget after = targetRepository.findById(target.getId()).orElseThrow();
        assertThat(after.getAssignedDistributionSet().getId()).isEqualTo(action.getDistributionSet().getId());
        assertThat(after.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
    }

    /**
     * Verifies that a device-side save based on a target read before an offline assignment fails with a concurrency failure
     * instead of overwriting the assignment.
     */
    @Test
    void staleDeviceUpdateAfterOfflineAssignmentFails() {
        final Target target = testdataFactory.createTargets("stale-offline-", 0, 1).get(0);
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("stale-offline-ds");
        // the device-side transaction reads its target before the assignment
        final JpaTarget staleTarget = requiresNew().execute(_ -> targetRepository.findById(target.getId()).orElseThrow());

        deploymentManagement.offlineAssignedDistributionSets(List.of(Map.entry(target.getControllerId(), ds.getId())));

        staleTarget.setAddress("http://device-stale");
        assertThatExceptionOfType(ConcurrencyFailureException.class)
                .isThrownBy(() -> requiresNew().executeWithoutResult(_ -> targetRepository.save(staleTarget)));
        final JpaTarget after = targetRepository.findById(target.getId()).orElseThrow();
        assertThat(after.getInstalledDistributionSet().getId()).isEqualTo(ds.getId());
        assertThat(after.getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);
    }

    /**
     * Verifies the events of a group start whose targets have active manual actions: one cancellation event for the
     * manual actions, one target updated event per target, one action updated event per cancelled and per started action and one
     * assignment event. Targets and actions get last modified at set.
     */
    @Test
    @ExpectEvents({
            // the scheduled actions are created on the start too
            @Expect(type = ActionCreatedEvent.class, count = 5),
            // one event for all cancelled actions
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 5),
            @Expect(type = ActionUpdatedEvent.class, count = 10),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = RolloutUpdatedEvent.class, count = 1),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 2) })
    void eventsOnGroupStart() {
        final String prefix = "events-";
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, 5);
        final List<Action> manualActions = targets.stream().map(t -> assignManually(t, prefix + t.getControllerId())).toList();
        final Rollout rollout = createRollout(prefix, "events-new");
        rolloutManagement.start(rollout.getId());
        waitForEvents();
        // count only the events of the group start
        EventVerifier.publishResetMarkerEvent(eventPublisher);
        final long startTime = System.currentTimeMillis();

        rolloutHandler.handleAll();
        waitForEvents();

        final List<JpaAction> rolloutActions = assertRolloutActionsActive(rollout, 5);
        for (final Target target : targets) {
            assertThat(targetRepository.findById(target.getId()).orElseThrow().getLastModifiedAt()).isGreaterThanOrEqualTo(startTime);
        }
        for (final Action action : manualActions) {
            assertThat(actionRepository.findById(action.getId()).orElseThrow().getLastModifiedAt()).isGreaterThanOrEqualTo(startTime);
        }
        assertThat(rolloutActions).allMatch(action -> action.getLastModifiedAt() >= startTime);
    }

    /**
     * Verifies that an action whose start fails with a concurrency failure doesn't stall the rest of the first group:
     * the other actions are started, the conflicting one stays scheduled and the rollout stays starting. The next run
     * starts the remaining action and the rollout gets running.
     */
    @Test
    void oneConflictingActionDoesNotStallBatch() {
        final String prefix = "one-conflict-";
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, 10);
        final Rollout rollout = createRollout(prefix, "one-conflict-new");
        rolloutManagement.start(rollout.getId());

        // every start attempt of the action of this target conflicts in the first run
        try (ConflictInjection ignored = ConflictInjection.afterEachRead(
                txManager, actionRepository, FIRST_GROUP_SCHEDULED_ACTIONS_READ,
                () -> bumpActionRevision(rollout, targets.get(4)))) {
            rolloutHandler.handleAll();
        }
        waitForEvents();

        assertRolloutActionsActive(rollout, 9);
        assertThat(actionRepository.findByRolloutIdAndStatus(PageRequest.of(0, 100), rollout.getId(), Status.SCHEDULED).getContent())
                .singleElement().satisfies(action -> assertThat(action.getTarget().getId()).isEqualTo(targets.get(4).getId()));
        assertThat(rolloutManagement.get(rollout.getId()).getStatus()).isEqualTo(RolloutStatus.STARTING);
        // assignment events only for the committed starts
        assertThat(assignedControllerIds()).hasSize(9).doesNotContain(targets.get(4).getControllerId());

        assignEvents.clear();
        rolloutHandler.handleAll();
        waitForEvents();

        assertRolloutActionsActive(rollout, 10);
        assertThat(assignedControllerIds()).containsExactly(targets.get(4).getControllerId());
        assertThat(rolloutManagement.get(rollout.getId()).getStatus()).isEqualTo(RolloutStatus.RUNNING);
    }

    /**
     * Verifies that when every action conflicts, the split stops after the failure budget of the page (default 10 failed transactions)
     * instead of a transaction per action, all actions stay scheduled and the next run starts them.
     */
    @Test
    void allConflictingActionsStopSplitAfterFailureBudget() {
        final String prefix = "all-conflict-";
        testdataFactory.createTargets(prefix, 0, 10);
        final Rollout rollout = createRollout(prefix, "all-conflict-new");
        rolloutManagement.start(rollout.getId());

        final AtomicInteger startAttempts = new AtomicInteger();
        try (ConflictInjection _ = ConflictInjection.afterEachRead(
                txManager, actionRepository, FIRST_GROUP_SCHEDULED_ACTIONS_READ, () -> bumpActionRevisions(rollout));
                ConflictInjection _ = ConflictInjection.afterEachRead(
                        txManager, actionRepository, FIRST_GROUP_SCHEDULED_ACTIONS_READ, startAttempts::incrementAndGet)) {
            rolloutHandler.handleAll();
        }
        waitForEvents();

        // page (10) + halves (5, 5) + quarters (2, 3, 2, 3) + 4 of the eighths (1, 1, 1, 2, ...) - then the budget of 10 failures
        // is exhausted (without it: 1 + 2 * 10 - 2 = 19 attempts)
        assertThat(startAttempts.get()).isEqualTo(11);
        assertThat(actionRepository.findByRolloutIdAndStatus(PageRequest.of(0, 100), rollout.getId(), Status.SCHEDULED).getContent())
                .hasSize(10);
        assertThat(rolloutManagement.get(rollout.getId()).getStatus()).isEqualTo(RolloutStatus.STARTING);

        rolloutHandler.handleAll();
        waitForEvents();

        assertRolloutActionsActive(rollout, 10);
        assertThat(rolloutManagement.get(rollout.getId()).getStatus()).isEqualTo(RolloutStatus.RUNNING);
    }

    /**
     * Verifies that an action of a child group whose start fails with a concurrency failure keeps the child group
     * scheduled (the parent's success action is rolled back), and that the next run starts the action and the child group.
     */
    @Test
    void oneConflictingActionOfChildGroupKeepsChildScheduled() {
        final String prefix = "one-conflict-child-";
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, 10);
        final DistributionSet ds = testdataFactory.createDistributionSetLocked(prefix + "new");
        final Rollout rollout = testdataFactory.createRolloutByVariables(
                prefix, prefix, 2, "controllerid==" + prefix + "*", ds, "50", "80");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final List<JpaAction> firstGroupActions = assertRolloutActionsActive(rollout, 5);
        firstGroupActions.subList(0, 3).forEach(this::finishAction);
        final RolloutGroup childGroup = rolloutGroupManagement.findByRollout(rollout.getId(), PageRequest.of(0, 10)).getContent().get(1);
        final Target conflicting = actionRepository.findByRolloutIdAndStatus(PageRequest.of(0, 100), rollout.getId(), Status.SCHEDULED)
                .getContent().get(2).getTarget();

        try (ConflictInjection ignored = ConflictInjection.afterEachRead(
                txManager, actionRepository, FIRST_GROUP_SCHEDULED_ACTIONS_READ, () -> bumpActionRevision(rollout, conflicting))) {
            rolloutHandler.handleAll();
        }
        waitForEvents();

        // 2 of the first group still running, 4 of the child group started
        assertRolloutActionsActive(rollout, 6);
        assertThat(actionRepository.findByRolloutIdAndStatus(PageRequest.of(0, 100), rollout.getId(), Status.SCHEDULED).getContent())
                .singleElement().satisfies(action -> assertThat(action.getTarget().getId()).isEqualTo(conflicting.getId()));
        assertThat(rolloutGroupManagement.get(childGroup.getId()).getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED);

        rolloutHandler.handleAll();
        waitForEvents();

        assertRolloutActionsActive(rollout, 7);
        assertThat(rolloutGroupManagement.get(childGroup.getId()).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
    }

    /**
     * Verifies that a manual trigger of the next group whose start can't start all actions fails with a concurrent modification
     * (REST 409) after one attempt - not retried - and that the startable actions are started while the group stays scheduled.
     */
    @Test
    void triggerNextGroupWithConflictFailsWithConcurrentModificationWithoutRetry() {
        final String prefix = "trigger-conflict-";
        testdataFactory.createTargets(prefix, 0, 10);
        final DistributionSet ds = testdataFactory.createDistributionSetLocked(prefix + "new");
        final Rollout rollout = testdataFactory.createRolloutByVariables(
                prefix, prefix, 2, "controllerid==" + prefix + "*", ds, "100", "100");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        assertRolloutActionsActive(rollout, 5);
        final RolloutGroup childGroup = rolloutGroupManagement.findByRollout(rollout.getId(), PageRequest.of(0, 10)).getContent().get(1);
        final Target conflicting = actionRepository.findByRolloutIdAndStatus(PageRequest.of(0, 100), rollout.getId(), Status.SCHEDULED)
                .getContent().get(2).getTarget();

        final AtomicInteger pageReads = new AtomicInteger();
        try (ConflictInjection _ = ConflictInjection.afterEachRead(
                txManager, actionRepository, FIRST_GROUP_SCHEDULED_ACTIONS_READ, () -> bumpActionRevision(rollout, conflicting));
                ConflictInjection _ = ConflictInjection.afterEachRead(
                        txManager, actionRepository, "findIdsByRolloutGroupIdInAndStatusAndIdGreaterThan", pageReads::incrementAndGet)) {
            final long rolloutId = rollout.getId();
            assertThatExceptionOfType(ConcurrentModificationException.class)
                    .isThrownBy(() -> rolloutManagement.triggerNextGroup(rolloutId));
        }
        waitForEvents();

        // one attempt: the page and the empty page after it
        assertThat(pageReads.get()).isEqualTo(2);
        assertRolloutActionsActive(rollout, 9);
        assertThat(rolloutGroupManagement.get(childGroup.getId()).getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED);
    }

    // a concurrent change of the rollout action of the target: increments its version, so a start based on the state read
    // before fails with a concurrency failure
    private void bumpActionRevision(final Rollout rollout, final Target target) {
        entityManager.createNativeQuery("UPDATE sp_action SET optlock_revision = optlock_revision + 1 WHERE rollout = ?1 AND target = ?2")
                .setParameter(1, rollout.getId())
                .setParameter(2, target.getId())
                .executeUpdate();
    }

    // a concurrent change of all rollout actions - every start attempt based on the state read before fails
    private void bumpActionRevisions(final Rollout rollout) {
        entityManager.createNativeQuery("UPDATE sp_action SET optlock_revision = optlock_revision + 1 WHERE rollout = ?1")
                .setParameter(1, rollout.getId())
                .executeUpdate();
    }

    private void updateAddress(final long targetId, final String address) {
        requiresNew().executeWithoutResult(status -> {
            final JpaTarget target = targetRepository.findById(targetId).orElseThrow();
            target.setAddress(address);
            targetRepository.save(target);
        });
    }

    private TransactionTemplate requiresNew() {
        final TransactionTemplate template = new TransactionTemplate(txManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private Action assignManually(final Target target, final String dsName) {
        final DistributionSet ds = testdataFactory.createDistributionSetLocked(dsName);
        return assignDistributionSet(ds.getId(), target.getControllerId()).getAssignedEntity().get(0);
    }

    private Rollout createRollout(final String prefix, final String dsName) {
        final DistributionSet ds = testdataFactory.createDistributionSetLocked(dsName);
        return testdataFactory.createRolloutByVariables(prefix, prefix, 1, "controllerid==" + prefix + "*", ds, "100", "100");
    }

    // starts the rollout and its first group; the device write runs right after the group start read its batch
    private void startFirstGroup(final Rollout rollout, final Runnable deviceWrite) {
        rolloutManagement.start(rollout.getId());
        try (ConflictInjection ignored = ConflictInjection.afterRead(
                txManager, actionRepository, FIRST_GROUP_SCHEDULED_ACTIONS_READ, deviceWrite)) {
            rolloutHandler.handleAll();
        }
        waitForEvents();
    }

    private List<JpaAction> assertRolloutActionsActive(final Rollout rollout, final int count) {
        final List<JpaAction> actions = actionRepository.findByRolloutIdAndStatus(PageRequest.of(0, 100), rollout.getId(), Status.RUNNING)
                .getContent();
        assertThat(actions).hasSize(count).allMatch(Action::isActive);
        return actions;
    }

    private List<Status> statusesOf(final Action action) {
        return actionStatusRepository.findByActionId(PageRequest.of(0, 100), action.getId()).map(ActionStatus::getStatus).getContent();
    }

    private List<String> assignedControllerIds() {
        return assignEvents.stream().flatMap(event -> event.getActions().keySet().stream()).toList();
    }

    private List<Long> cancelledActionIdsFor(final Target target) {
        return cancelEvents.stream()
                .map(event -> event.getActionPropertiesForController(target.getControllerId()))
                .flatMap(Optional::stream)
                .map(ActionProperties::getId)
                .toList();
    }
}
