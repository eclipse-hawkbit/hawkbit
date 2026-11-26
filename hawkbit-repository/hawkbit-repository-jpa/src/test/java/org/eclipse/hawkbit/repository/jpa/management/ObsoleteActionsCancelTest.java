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
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.EntityManager;

import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionProperties;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.ConflictInjection;
import org.eclipse.hawkbit.repository.test.util.QueryCount;
import org.eclipse.hawkbit.repository.test.util.QueryCount.Kind;
import org.eclipse.hawkbit.repository.test.util.QueryCountConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The obsolete actions of the targets of a new assignment (manual or rollout group start) are cancelled in pages, with a bulk
 * update per page: a number of statements that doesn't depend on the number of obsolete actions, the same statuses and events
 * as an update per action, and no rollback of the batch on a concurrent device update of an obsolete action.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Obsolete action cancellation
 */
// groups of 50 targets exceed the test default of the targets per rollout group quota
@TestPropertySource(properties = "hawkbit.server.security.dos.maxTargetsPerRolloutGroup=100")
@Import(QueryCountConfiguration.class)
class ObsoleteActionsCancelTest extends AbstractJpaIntegrationTest {

    private static final String TENANT_CONFIGURATION = "sp_tenant_configuration";
    // the read of a page of obsolete actions
    private static final String OBSOLETE_ACTIONS_READ = "findAllWithoutCount";

    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private QueryCount queryCount;
    @Autowired
    private ApplicationEventMulticaster eventMulticaster;

    private final List<CancelTargetAssignmentEvent> cancelEvents = new CopyOnWriteArrayList<>();
    private final ApplicationListener<CancelTargetAssignmentEvent> cancelEventListener = cancelEvents::add;

    @BeforeEach
    void registerEventListeners() {
        eventMulticaster.addApplicationListener(cancelEventListener);
    }

    @AfterEach
    void removeEventListeners() {
        eventMulticaster.removeApplicationListener(cancelEventListener);
    }

    /**
     * Verifies that cancelling the obsolete actions of 10 and of 50 targets on a group start issues one action UPDATE and the
     * same number of SELECTs.
     */
    @Test
    void cancelStatementsIndependentOfObsoleteActionCount() {
        final QueryCount.Snapshot small = cancelStatements("cancel-statements-10-", 10);
        final QueryCount.Snapshot large = cancelStatements("cancel-statements-50-", 50);

        assertThat(small.count(Kind.UPDATE, "sp_action")).as("action UPDATEs 10: %s", small).isEqualTo(1);
        assertThat(large.count(Kind.UPDATE, "sp_action")).as("action UPDATEs 50: %s", large).isEqualTo(1);
        assertThat(large.countExcluding(Kind.SELECT, TENANT_CONFIGURATION))
                .as("SELECTs 10: %s, 50: %s", small, large)
                .isEqualTo(small.countExcluding(Kind.SELECT, TENANT_CONFIGURATION));
    }

    /**
     * Verifies that a device feedback on an obsolete action, committed after the group start read the obsolete actions, doesn't
     * roll back the cancellation: the obsolete actions are read once (no retry or split) and all of them are cancelled.
     */
    @Test
    void deviceFeedbackOnObsoleteActionDoesNotRollBackCancellation() {
        final String prefix = "cancel-feedback-";
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, 3);
        final List<Action> obsolete = targets.stream().map(target -> assignManually(target, prefix + target.getControllerId())).toList();
        final Rollout rollout = createRollout(prefix);

        final int pageReads = startFirstGroup(rollout, () -> controllerManagement.addUpdateActionStatus(
                ActionStatusCreate.builder().actionId(obsolete.get(0).getId()).status(Status.RUNNING).build()));

        assertThat(pageReads).isEqualTo(1);
        for (final Action action : obsolete) {
            assertThat(actionRepository.findById(action.getId()).orElseThrow().getStatus()).isEqualTo(Status.CANCELING);
            assertThat(statusesOf(action)).filteredOn(status -> status == Status.CANCELING).hasSize(1);
        }
        assertThat(statusesOf(obsolete.get(0))).contains(Status.RUNNING);
        assertRolloutActionsRunning(rollout, 3);
    }

    /**
     * Verifies that an obsolete action the device finishes after the group start read the obsolete actions is skipped by the
     * bulk update: it stays finished, gets no cancel status and no cancel event, the others are cancelled without a retry.
     */
    @Test
    void obsoleteActionFinishedConcurrentlyIsSkipped() {
        final String prefix = "cancel-finished-";
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, 3);
        final List<Action> obsolete = targets.stream().map(target -> assignManually(target, prefix + target.getControllerId())).toList();
        final Rollout rollout = createRollout(prefix);

        final int pageReads = startFirstGroup(rollout, () -> finishAction(obsolete.get(0)));

        assertThat(pageReads).isEqualTo(1);
        final JpaAction finished = actionRepository.findById(obsolete.get(0).getId()).orElseThrow();
        assertThat(finished.getStatus()).isEqualTo(Status.FINISHED);
        assertThat(finished.isActive()).isFalse();
        assertThat(statusesOf(finished)).noneMatch(status -> status == Status.CANCELING);
        assertThat(cancelledActionIdsFor(targets.get(0))).isEmpty();
        for (int i = 1; i < obsolete.size(); i++) {
            final Action action = obsolete.get(i);
            assertThat(actionRepository.findById(action.getId()).orElseThrow().getStatus()).isEqualTo(Status.CANCELING);
            assertThat(statusesOf(action)).filteredOn(status -> status == Status.CANCELING).hasSize(1);
            assertThat(cancelledActionIdsFor(targets.get(i))).containsExactly(action.getId());
        }
        assertRolloutActionsRunning(rollout, 3);
    }

    /**
     * Verifies that an obsolete action set to canceling by a concurrent transaction after the group start read the obsolete
     * actions isn't treated as cancelled by the group start: no second cancel status and no cancel event for it.
     */
    @Test
    void obsoleteActionCancelledConcurrentlyGetsNoSecondCancel() {
        final String prefix = "cancel-concurrent-";
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, 3);
        final List<Action> obsolete = targets.stream().map(target -> assignManually(target, prefix + target.getControllerId())).toList();
        final Rollout rollout = createRollout(prefix);

        final int pageReads = startFirstGroup(rollout, () -> entityManager
                .createNativeQuery("UPDATE sp_action SET status = 5, optlock_revision = optlock_revision + 1 WHERE id = ?1")
                .setParameter(1, obsolete.get(0).getId())
                .executeUpdate());

        assertThat(pageReads).isEqualTo(1);
        assertThat(actionRepository.findById(obsolete.get(0).getId()).orElseThrow().getStatus()).isEqualTo(Status.CANCELING);
        assertThat(statusesOf(obsolete.get(0))).noneMatch(status -> status == Status.CANCELING);
        assertThat(cancelledActionIdsFor(targets.get(0))).isEmpty();
        for (int i = 1; i < obsolete.size(); i++) {
            assertThat(statusesOf(obsolete.get(i))).filteredOn(status -> status == Status.CANCELING).hasSize(1);
            assertThat(cancelledActionIdsFor(targets.get(i))).containsExactly(obsolete.get(i).getId());
        }
        assertRolloutActionsRunning(rollout, 3);
    }

    // statements of the cancellation of the obsolete actions on the first group start (from right after the first read of the
    // obsolete actions to right after the save of their statuses)
    private QueryCount.Snapshot cancelStatements(final String prefix, final int targetCount) {
        final List<Target> targets = testdataFactory.createTargets(prefix, 0, targetCount);
        final DistributionSet manualSet = testdataFactory.createDistributionSetLocked(prefix + "manual");
        // in chunks, below the assignments per request quota
        ListUtils.partition(targets.stream().map(Target::getControllerId).toList(), 10)
                .forEach(controllerIds -> assignDistributionSet(manualSet.getId(), controllerIds, ActionType.FORCED));
        final Rollout rollout = createRollout(prefix);
        rolloutManagement.start(rollout.getId());
        waitForEvents();

        final AtomicReference<QueryCount.Snapshot> measured = new AtomicReference<>();
        final AtomicReference<ConflictInjection> end = new AtomicReference<>();
        try (ConflictInjection _ = ConflictInjection.afterRead(txManager, actionRepository, OBSOLETE_ACTIONS_READ, () -> {
            queryCount.resetQueries();
            end.set(ConflictInjection.afterRead(txManager, actionStatusRepository, "saveAll",
                    () -> measured.set(queryCount.snapshot())));
        })) {
            rolloutHandler.handleAll();
        } finally {
            if (end.get() != null) {
                end.get().close();
            }
        }
        waitForEvents();

        assertThat(measured.get()).as("measured section reached").isNotNull();
        assertRolloutActionsRunning(rollout, targetCount);
        assertThat(actionRepository.findAll(byDistributionSetId(manualSet.getId())))
                .hasSize(targetCount).allMatch(action -> action.getStatus() == Status.CANCELING);
        return measured.get();
    }

    // starts the rollout and its first group; the device write runs right after the first read of the obsolete actions. Returns
    // the number of reads of obsolete actions
    private int startFirstGroup(final Rollout rollout, final Runnable deviceWrite) {
        rolloutManagement.start(rollout.getId());
        final AtomicInteger pageReads = new AtomicInteger();
        try (ConflictInjection _ = ConflictInjection.afterEachRead(txManager, actionRepository, OBSOLETE_ACTIONS_READ, () -> {
            if (pageReads.getAndIncrement() == 0) {
                deviceWrite.run();
            }
        })) {
            rolloutHandler.handleAll();
        }
        waitForEvents();
        return pageReads.get();
    }

    private Action assignManually(final Target target, final String dsName) {
        final DistributionSet ds = testdataFactory.createDistributionSetLocked(dsName);
        return assignDistributionSet(ds.getId(), target.getControllerId()).getAssignedEntity().get(0);
    }

    private Rollout createRollout(final String prefix) {
        final DistributionSet ds = testdataFactory.createDistributionSetLocked(prefix + "rollout");
        return testdataFactory.createRolloutByVariables(prefix, prefix, 1, "controllerid==" + prefix + "*", ds, "100", "100");
    }

    private void assertRolloutActionsRunning(final Rollout rollout, final int count) {
        assertThat(actionRepository.findByRolloutIdAndStatus(PageRequest.of(0, 100), rollout.getId(), Status.RUNNING).getContent())
                .hasSize(count).allMatch(Action::isActive);
    }

    private List<Status> statusesOf(final Action action) {
        return actionStatusRepository.findByActionId(PageRequest.of(0, 100), action.getId()).map(ActionStatus::getStatus).getContent();
    }

    private List<Long> cancelledActionIdsFor(final Target target) {
        return cancelEvents.stream()
                .map(event -> event.getActionPropertiesForController(target.getControllerId()))
                .flatMap(Optional::stream)
                .map(ActionProperties::getId)
                .toList();
    }
}
