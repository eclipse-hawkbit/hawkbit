/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutExecutor;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutHelper;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.event.remote.RolloutGroupDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutStoppedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.management.JpaRolloutManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.EvaluatorNotConfiguredException;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupEvaluationManager;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.security.SpringSecurityAuditorAware;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;

import static org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutGroupCreate.addSuccessAndErrorConditionsAndActions;

/**
 * A Jpa implementation of {@link RolloutExecutor}
 */
@Slf4j
public class JpaRolloutExecutor implements RolloutExecutor {

    /**
     * Max amount of targets that are handled in one transaction.
     */
    private static final int TRANSACTION_TARGETS = 5_000;

    /**
     * Maximum amount of actions that are deleted in one transaction.
     */
    private static final int TRANSACTION_ACTIONS = 5_000;

    /**
     * Action statuses that result in a terminated action
     */
    private static final List<Status> DEFAULT_ACTION_TERMINATION_STATUSES = Arrays.asList(Status.ERROR, Status.FINISHED,
            Status.CANCELED);
    /**
     * In case of DOWNLOAD_ONLY, actions can be finished with DOWNLOADED status.
     */
    private static final List<Status> DOWNLOAD_ONLY_ACTION_TERMINATION_STATUSES = Arrays.asList(Status.ERROR,
            Status.FINISHED, Status.CANCELED, Status.DOWNLOADED);

    private final RolloutTargetGroupRepository rolloutTargetGroupRepository;
    private final EntityManager entityManager;
    private final RolloutRepository rolloutRepository;
    private final ActionRepository actionRepository;
    private final RolloutGroupRepository rolloutGroupRepository;
    private final AfterTransactionCommitExecutor afterCommit;
    private final TenantAware tenantAware;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final QuotaManagement quotaManagement;
    private final DeploymentManagement deploymentManagement;
    private final TargetManagement targetManagement;
    private final EventPublisherHolder eventPublisherHolder;
    private final PlatformTransactionManager txManager;
    private final RolloutApprovalStrategy rolloutApprovalStrategy;
    private final RolloutGroupEvaluationManager evaluationManager;
    private final RolloutManagement rolloutManagement;
    
    /**
     * Constructor
     */
    public JpaRolloutExecutor(final RolloutTargetGroupRepository rolloutTargetGroupRepository,
            final EntityManager entityManager, final RolloutRepository rolloutRepository,
            final ActionRepository actionRepository, final RolloutGroupRepository rolloutGroupRepository,
            final AfterTransactionCommitExecutor afterCommit, final TenantAware tenantAware,
            final RolloutGroupManagement rolloutGroupManagement, final QuotaManagement quotaManagement,
            final DeploymentManagement deploymentManagement, final TargetManagement targetManagement,
            final EventPublisherHolder eventPublisherHolder, final PlatformTransactionManager txManager,
            final RolloutApprovalStrategy rolloutApprovalStrategy,
            final RolloutGroupEvaluationManager evaluationManager, final RolloutManagement rolloutManagement) {
        this.rolloutTargetGroupRepository = rolloutTargetGroupRepository;
        this.entityManager = entityManager;
        this.rolloutRepository = rolloutRepository;
        this.actionRepository = actionRepository;
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.afterCommit = afterCommit;
        this.tenantAware = tenantAware;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.quotaManagement = quotaManagement;
        this.deploymentManagement = deploymentManagement;
        this.targetManagement = targetManagement;
        this.eventPublisherHolder = eventPublisherHolder;
        this.txManager = txManager;
        this.rolloutApprovalStrategy = rolloutApprovalStrategy;
        this.evaluationManager = evaluationManager;
        this.rolloutManagement = rolloutManagement;
    }

    @Override
    public void execute(final Rollout rollout) {
        log.debug("handle rollout {}", rollout.getId());

        switch (rollout.getStatus()) {
        case CREATING:
            handleCreateRollout((JpaRollout) rollout);
            break;
        case READY:
            handleReadyRollout(rollout);
            break;
        case STARTING:
            // the lastModifiedBy user is probably the user that has actually called the rollout start (unless overridden) - not the creator
            SpringSecurityAuditorAware.setAuditorOverride(rollout.getLastModifiedBy());
            try {
                handleStartingRollout(rollout);
            } finally {
                // clear, ALWAYS, the set auditor override
                SpringSecurityAuditorAware.clearAuditorOverride();
            }
            break;
        case RUNNING:
            handleRunningRollout((JpaRollout) rollout);
            break;
        case STOPPING:
            // the lastModifiedBy user is probably the user that has actually called the rollout stop (unless overridden) - not the creator
            SpringSecurityAuditorAware.setAuditorOverride(rollout.getLastModifiedBy());
            try {
                handleStopRollout((JpaRollout) rollout);
            } finally {
                // clear, ALWAYS, the set auditor override
                SpringSecurityAuditorAware.clearAuditorOverride();
            }
            break;
        case DELETING:
            // the lastModifiedBy user is probably the user that has actually called the rollout delete (unless overridden) - not the creator
            SpringSecurityAuditorAware.setAuditorOverride(rollout.getLastModifiedBy());
            try {
                handleDeleteRollout((JpaRollout) rollout);
            } finally {
                // clear, ALWAYS, the set auditor override
                SpringSecurityAuditorAware.clearAuditorOverride();
            }
            break;
        default:
            log.error("Rollout in status {} not supposed to be handled!", rollout.getStatus());
            break;
        }
    }

    private void handleCreateRollout(final JpaRollout rollout) {
        log.debug("handleCreateRollout called for rollout {}", rollout.getId());

        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(
                PageRequest.of(0, quotaManagement.getMaxRolloutGroupsPerRollout(), Sort.by(Direction.ASC, "id")),
                rollout.getId()).getContent();

        int readyGroups = 0;
        int totalTargets = 0;
        for (final RolloutGroup group : rolloutGroups) {
            if (RolloutGroupStatus.READY == group.getStatus()) {
                readyGroups++;
                totalTargets += group.getTotalTargets();
                continue;
            }

            final RolloutGroup filledGroup = fillRolloutGroupWithTargets(rollout, (JpaRolloutGroup) group,
                    rolloutGroups);
            if (RolloutGroupStatus.READY == filledGroup.getStatus()) {
                readyGroups++;
                totalTargets += filledGroup.getTotalTargets();
            }
        }

        // When all groups are ready the rollout status can be changed to be
        // ready, too.
        if (readyGroups == rolloutGroups.size()) {
            if (rollout.isDynamic()) {
                // add first dynamic group one by using the last as a parent and as a pattern
                createDynamicGroup(rollout, rolloutGroups.get(rolloutGroups.size() - 1), rolloutGroups.size(), RolloutGroupStatus.READY);
            }

            if (!rolloutApprovalStrategy.isApprovalNeeded(rollout)) {
                rollout.setStatus(RolloutStatus.READY);
                log.debug("rollout {} creation done. Switch to READY.", rollout.getId());
            } else {
                log.debug("rollout {} creation done. Switch to WAITING_FOR_APPROVAL.", rollout.getId());
                rollout.setStatus(RolloutStatus.WAITING_FOR_APPROVAL);
                rolloutApprovalStrategy.onApprovalRequired(rollout);
            }
            rollout.setLastCheck(0);
            rollout.setTotalTargets(totalTargets);
            rolloutRepository.save(rollout);
        }
    }

    private void handleDeleteRollout(final JpaRollout rollout) {
        log.debug("handleDeleteRollout called for {}", rollout.getId());

        // check if there are actions beyond schedule
        boolean hardDeleteRolloutGroups = !actionRepository.existsByRolloutIdAndStatusNotIn(rollout.getId(),
                Status.SCHEDULED);
        if (hardDeleteRolloutGroups) {
            log.debug("Rollout {} has no actions other than scheduled -> hard delete", rollout.getId());
            hardDeleteRollout(rollout);
            return;
        }
        // clean up all scheduled actions
        final Slice<JpaAction> scheduledActions = findScheduledActionsByRollout(rollout);
        deleteScheduledActions(rollout, scheduledActions);

        // avoid another scheduler round and re-check if all scheduled actions
        // has been cleaned up. we flush first to ensure that the we include the
        // deletion above
        entityManager.flush();
        final boolean hasScheduledActionsLeft = actionRepository.countByRolloutIdAndStatus(rollout.getId(),
                Status.SCHEDULED) > 0;

        if (hasScheduledActionsLeft) {
            return;
        }

        // only hard delete the rollout if no actions are left for the rollout.
        // In case actions are left, they are probably are running or were
        // running before, so only soft delete.
        hardDeleteRolloutGroups = !actionRepository.existsByRolloutId(rollout.getId());
        if (hardDeleteRolloutGroups) {
            hardDeleteRollout(rollout);
            return;
        }

        // set soft delete
        rollout.setStatus(RolloutStatus.DELETED);
        rollout.setDeleted(true);
        rolloutRepository.save(rollout);

        sendRolloutGroupDeletedEvents(rollout);
    }

    private void handleStopRollout(final JpaRollout rollout) {
        log.debug("handleStopRollout called for {}", rollout.getId());
        // clean up all scheduled actions
        final Slice<JpaAction> scheduledActions = findScheduledActionsByRollout(rollout);
        deleteScheduledActions(rollout, scheduledActions);

        // avoid another scheduler round and re-check if all scheduled actions
        // has been cleaned up. we flush first to ensure that the we include the
        // deletion above
        entityManager.flush();
        final boolean hasScheduledActionsLeft = actionRepository.countByRolloutIdAndStatus(rollout.getId(),
                Status.SCHEDULED) > 0;

        if (hasScheduledActionsLeft) {
            return;
        }

        rolloutGroupRepository.findByRolloutAndStatusNotIn(rollout,
                Arrays.asList(RolloutGroupStatus.FINISHED, RolloutGroupStatus.ERROR)).forEach(rolloutGroup -> {
                    rolloutGroup.setStatus(RolloutGroupStatus.FINISHED);
                    rolloutGroupRepository.save(rolloutGroup);
                });

        rollout.setStatus(RolloutStatus.FINISHED);
        rolloutRepository.save(rollout);

        final List<Long> groupIds = rollout.getRolloutGroups().stream().map(RolloutGroup::getId)
                .collect(Collectors.toList());

        afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher().publishEvent(new RolloutStoppedEvent(
                tenantAware.getCurrentTenant(), eventPublisherHolder.getApplicationId(), rollout.getId(), groupIds)));
    }

    private void handleReadyRollout(final Rollout rollout) {
        if (rollout.getStartAt() != null && rollout.getStartAt() <= System.currentTimeMillis()) {
            log.debug(
                    "handleReadyRollout called for rollout {} with autostart beyond define time. Switch to STARTING",
                    rollout.getId());
            rolloutManagement.start(rollout.getId());
        }
    }

    private void handleStartingRollout(final Rollout rollout) {
        log.debug("handleStartingRollout called for rollout {}", rollout.getId());

        if (ensureAllGroupsAreScheduled(rollout)) {
            startFirstRolloutGroup(rollout);
        }
    }

    private void handleRunningRollout(final JpaRollout rollout) {
        log.debug("handleRunningRollout called for rollout {}", rollout.getId());

        if (rollout.isDynamic()) {
            if (fillDynamicRolloutGroupsWithTargets(rollout)) {
                log.debug("Dynamic group created for rollout {}", rollout.getId());
                return;
            }
        }

        final List<JpaRolloutGroup> rolloutGroupsRunning =
                rollout.getRolloutGroups().stream()
                        .filter(group -> group.getStatus() == RolloutGroupStatus.RUNNING)
                        .map(JpaRolloutGroup.class::cast)
                        .toList();

        if (rolloutGroupsRunning.isEmpty()) {
            // no running rollouts, probably there was an error
            // somewhere at the latest group. And the latest group has
            // been switched from running into error state. So we need
            // to find the latest group which
            executeLatestRolloutGroup(rollout);
        } else {
            log.debug("Rollout {} has {} running groups", rollout.getId(), rolloutGroupsRunning.size());
            executeRolloutGroups(rollout, rolloutGroupsRunning, rollout.getRolloutGroups().get(rollout.getRolloutGroups().size() - 1));
        }

        if (isRolloutComplete(rollout)) {
            log.info("Rollout {} is finished, setting FINISHED status", rollout);
            rollout.setStatus(RolloutStatus.FINISHED);
            rolloutRepository.save(rollout);
        }
    }

    private void hardDeleteRollout(final JpaRollout rollout) {
        sendRolloutGroupDeletedEvents(rollout);
        rolloutRepository.delete(rollout);
    }

    private void deleteScheduledActions(final JpaRollout rollout, final Slice<JpaAction> scheduledActions) {
        final boolean hasScheduledActions = scheduledActions.getNumberOfElements() > 0;

        if (hasScheduledActions) {
            try {
                final Iterable<JpaAction> iterable = scheduledActions::iterator;
                final List<Long> actionIds = StreamSupport.stream(iterable.spliterator(), false).map(Action::getId)
                        .collect(Collectors.toList());
                actionRepository.deleteByIdIn(actionIds);
                afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher()
                        .publishEvent(new RolloutUpdatedEvent(rollout, eventPublisherHolder.getApplicationId())));
            } catch (final RuntimeException e) {
                log.error("Exception during deletion of actions of rollout {}", rollout, e);
            }
        }
    }

    private Slice<JpaAction> findScheduledActionsByRollout(final JpaRollout rollout) {
        return actionRepository.findByRolloutIdAndStatus(PageRequest.of(0, TRANSACTION_ACTIONS), rollout.getId(),
                Status.SCHEDULED);
    }

    private void sendRolloutGroupDeletedEvents(final JpaRollout rollout) {
        final List<Long> groupIds = rollout.getRolloutGroups().stream().map(RolloutGroup::getId)
                .collect(Collectors.toList());

        afterCommit.afterCommit(() -> groupIds.forEach(rolloutGroupId -> eventPublisherHolder.getEventPublisher()
                .publishEvent(new RolloutGroupDeletedEvent(tenantAware.getCurrentTenant(), rolloutGroupId,
                        JpaRolloutGroup.class, eventPublisherHolder.getApplicationId()))));
    }

    private boolean isRolloutComplete(final JpaRollout rollout) {
        // ensure that changes in the same transaction count
        entityManager.flush();
        final Long groupsActiveLeft = rolloutGroupRepository.countByRolloutIdAndStatusOrStatus(rollout.getId(),
                RolloutGroupStatus.RUNNING, RolloutGroupStatus.SCHEDULED);
        return groupsActiveLeft == 0;
    }

    private static final Comparator<RolloutGroup> DESC_COMP = Comparator.comparingLong(RolloutGroup::getId).reversed();
    private void executeLatestRolloutGroup(final JpaRollout rollout) {
        // was - rolloutGroupRepository.findByRolloutAndStatusNotOrderByIdDesc(rollout, RolloutGroupStatus.SCHEDULED);
        final List<JpaRolloutGroup> latestRolloutGroup = rollout.getRolloutGroups().stream()
                .filter(group -> group.getStatus() != RolloutGroupStatus.SCHEDULED)
                .sorted(DESC_COMP)
                .map(JpaRolloutGroup.class::cast)
                .toList();
        if (latestRolloutGroup.isEmpty()) {
            return;
        }
        executeRolloutGroupSuccessAction(rollout, latestRolloutGroup.get(0));
    }

    private static final int DEFAULT_DYNAMIC_GROUP_EXPECTED = 100;
    private static int expectedDynamicGroupSize(final List<? extends RolloutGroup> rolloutGroups) {
        int expected = 0;
        // gets the size of last static group (it is a pattern for dynamic groups)
        for (final RolloutGroup rolloutGroup : rolloutGroups) {
            if (rolloutGroup.isDynamic()) {
                break;
            }
            expected = rolloutGroup.getTotalTargets();
        }
        return expected <= 0 ? DEFAULT_DYNAMIC_GROUP_EXPECTED /* default if the last static group has been empty */ : expected;
    }

    // fakes getTotalTargets count to match expected for the last dynamic group
    // so the evaluation to use total targets to properly
    private RolloutGroup evalProxy(final RolloutGroup group, final List<JpaRolloutGroup> rolloutGroups) {
        if (group.isDynamic()) {
            final int expected = expectedDynamicGroupSize(rolloutGroups);
            return (RolloutGroup) Proxy.newProxyInstance(
                    RolloutGroup.class.getClassLoader(),
                    new Class<?>[] {RolloutGroup.class},
                    (proxy, method, args) -> {
                        if ("getTotalTargets".equals(method.getName())) {
                            return expected;
                        } else {
                            try {
                                return method.invoke(group, args);
                            } catch (final InvocationTargetException e) {
                                throw e.getCause() == null ? e : e.getCause();
                            }
                        }
                    });
        } else {
            return group;
        }
    }

    private void executeRolloutGroups(final JpaRollout rollout, final List<JpaRolloutGroup> rolloutGroups, final RolloutGroup lastRolloutGroup) {
        for (final JpaRolloutGroup rolloutGroup : rolloutGroups) {
            final long targetCount = countTargetsFrom(rolloutGroup);
            if (rolloutGroup.getTotalTargets() != targetCount) {
                updateTotalTargetCount(rolloutGroup, targetCount);
            }

            final RolloutGroup evalProxy = rolloutGroup == rolloutGroups.get(rolloutGroups.size() - 1) ?
                    evalProxy(rolloutGroup, rolloutGroups) : rolloutGroup;
            // error state check, do we need to stop the whole
            // rollout because of error?
            final boolean isError = checkErrorState(rollout, evalProxy);
            if (isError) {
                log.info("Rollout {} {} has error, calling error action", rollout.getName(), rollout.getId());
                callErrorAction(rollout, rolloutGroup);
            } else {
                // not in error so check finished state, do we need to
                // start the next group?
                checkSuccessCondition(rollout, rolloutGroup, evalProxy, rolloutGroup.getSuccessCondition());
                if (!(rolloutGroup == lastRolloutGroup && rolloutGroup.isDynamic()) && isRolloutGroupComplete(rollout, rolloutGroup)) {
                    rolloutGroup.setStatus(RolloutGroupStatus.FINISHED);
                    rolloutGroupRepository.save(rolloutGroup);
                }
            }
        }
    }

    private void updateTotalTargetCount(final JpaRolloutGroup rolloutGroup, final long countTargetsOfRolloutGroup) {
        final JpaRollout jpaRollout = (JpaRollout) rolloutGroup.getRollout();
        final long updatedTargetCount = jpaRollout.getTotalTargets()
                - (rolloutGroup.getTotalTargets() - countTargetsOfRolloutGroup);
        jpaRollout.setTotalTargets(updatedTargetCount);
        rolloutGroup.setTotalTargets((int) countTargetsOfRolloutGroup);
        rolloutRepository.save(jpaRollout);
        rolloutGroupRepository.save(rolloutGroup);
    }

    private long countTargetsFrom(final JpaRolloutGroup rolloutGroup) {
        if (rolloutGroup.isDynamic()) {
            return targetManagement.countByActionsInRolloutGroup(rolloutGroup.getId());
        } else {
            return rolloutGroupManagement.countTargetsOfRolloutsGroup(rolloutGroup.getId());
        }
    }

    private void callErrorAction(final Rollout rollout, final RolloutGroup rolloutGroup) {
        try {
            evaluationManager.getErrorActionEvaluator(rolloutGroup.getErrorAction()).exec(rollout, rolloutGroup);
        } catch (final EvaluatorNotConfiguredException e) {
            log.error("Something bad happened when accessing the error action bean {}",
                    rolloutGroup.getErrorAction().name(), e);
        }
    }

    private boolean isRolloutGroupComplete(final JpaRollout rollout, final JpaRolloutGroup rolloutGroup) {
        final Long actionsLeftForRollout =
                actionRepository.countByRolloutAndRolloutGroupAndStatusNotIn(
                        rollout, rolloutGroup,
                        ActionType.DOWNLOAD_ONLY == rollout.getActionType() ?
                                DOWNLOAD_ONLY_ACTION_TERMINATION_STATUSES : DEFAULT_ACTION_TERMINATION_STATUSES);
        return actionsLeftForRollout == 0;
    }

    private boolean checkErrorState(final Rollout rollout, final RolloutGroup rolloutGroup) {

        final RolloutGroupErrorCondition errorCondition = rolloutGroup.getErrorCondition();

        if (errorCondition == null) {
            // there is no error condition, so return false, don't have error.
            return false;
        }
        try {
            return evaluationManager.getErrorConditionEvaluator(errorCondition).eval(rollout, rolloutGroup,
                    rolloutGroup.getErrorConditionExp());
        } catch (final EvaluatorNotConfiguredException e) {
            log.error("Something bad happened when accessing the error condition bean {}", errorCondition.name(), e);
            return false;
        }
    }

    private boolean checkSuccessCondition(final Rollout rollout, final RolloutGroup rolloutGroup, final RolloutGroup evalProxy,
            final RolloutGroupSuccessCondition successCondition) {
        log.trace("Checking finish condition {} on rolloutgroup {}", successCondition, rolloutGroup);
        try {
            final boolean isFinished = evaluationManager.getSuccessConditionEvaluator(successCondition).eval(rollout,
                    evalProxy, rolloutGroup.getSuccessConditionExp());
            if (isFinished) {
                log.debug("Rolloutgroup {} is finished, starting next group", rolloutGroup);
                executeRolloutGroupSuccessAction(rollout, rolloutGroup);
            } else {
                log.debug("Rolloutgroup {} is still running", rolloutGroup);
            }
            return isFinished;
        } catch (final EvaluatorNotConfiguredException e) {
            log.error("Something bad happened when accessing the finish condition or success action bean {}",
                    successCondition.name(), e);
            return false;
        }
    }

    private void executeRolloutGroupSuccessAction(final Rollout rollout, final RolloutGroup rolloutGroup) {
        evaluationManager.getSuccessActionEvaluator(rolloutGroup.getSuccessAction()).exec(rollout, rolloutGroup);
    }

    private void startFirstRolloutGroup(final Rollout rollout) {
        log.debug("startFirstRolloutGroup called for rollout {}", rollout.getId());
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.STARTING);
        final JpaRollout jpaRollout = (JpaRollout) rollout;

        final List<JpaRolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutOrderByIdAsc(jpaRollout);
        final JpaRolloutGroup rolloutGroup = rolloutGroups.get(0);
        if (rolloutGroup.getParent() != null) {
            throw new RolloutIllegalStateException("First Group is not the first group.");
        }

        deploymentManagement.startScheduledActionsByRolloutGroupParent(rollout.getId(),
                rollout.getDistributionSet().getId(), null);

        rolloutGroup.setStatus(RolloutGroupStatus.RUNNING);
        rolloutGroupRepository.save(rolloutGroup);

        jpaRollout.setStatus(RolloutStatus.RUNNING);
        jpaRollout.setLastCheck(0);
        rolloutRepository.save(jpaRollout);
    }

    private boolean ensureAllGroupsAreScheduled(final Rollout rollout) {
        final JpaRollout jpaRollout = (JpaRollout) rollout;

        final List<JpaRolloutGroup> groupsToBeScheduled = rolloutGroupRepository.findByRolloutAndStatus(rollout,
                RolloutGroupStatus.READY);
        final long scheduledGroups = groupsToBeScheduled.stream()
                .filter(group -> scheduleRolloutGroup(jpaRollout, group)).count();

        return scheduledGroups == groupsToBeScheduled.size();
    }

    private RolloutGroup fillRolloutGroupWithTargets(final JpaRollout rollout, final JpaRolloutGroup group,
            final List<RolloutGroup> rolloutGroups) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.CREATING);

        final String groupTargetFilter = RolloutHelper.getGroupTargetFilter(
                RolloutHelper.getTargetFilterQuery(rollout), group);

        final List<Long> readyGroups = RolloutHelper.getGroupsByStatusIncludingGroup(rollout.getRolloutGroups(),
                RolloutGroupStatus.READY, group);

        long targetsInGroupFilter;
        if (!RolloutHelper.isRolloutRetried(rollout.getTargetFilterQuery())) {
            targetsInGroupFilter = DeploymentHelper.runInNewTransaction(txManager,
                "countAllTargetsByTargetFilterQueryAndNotInRolloutGroups",
                count -> targetManagement.countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(readyGroups,
                        groupTargetFilter, rollout.getDistributionSet().getType()));
        } else {
            targetsInGroupFilter = DeploymentHelper.runInNewTransaction(txManager,
                "countByFailedRolloutAndNotInRolloutGroupsAndCompatible",
                count -> targetManagement.countByFailedRolloutAndNotInRolloutGroups(readyGroups,
                    RolloutHelper.getIdFromRetriedTargetFilter(rollout.getTargetFilterQuery())));
        }

        final double percentFromTheRest;
        if (rollout.isNewStyleTargetPercent()) { // new style percent - total percent
            percentFromTheRest = RolloutHelper.toPercentFromTheRest(group, rolloutGroups);
        } else { // old style percent - percent from rest
            percentFromTheRest = group.getTargetPercentage();
        }

        final long expectedInGroup = Math.round(percentFromTheRest * targetsInGroupFilter / 100);
        final long currentlyInGroup = DeploymentHelper.runInNewTransaction(txManager,
                "countRolloutTargetGroupByRolloutGroup",
                count -> rolloutTargetGroupRepository.countByRolloutGroup(group));

        // Switch the Group status to READY, when there are enough Targets in the Group
        if (currentlyInGroup >= expectedInGroup) {
            group.setStatus(RolloutGroupStatus.READY);
            return rolloutGroupRepository.save(group);
        }

        try {
            long targetsLeftToAdd = expectedInGroup - currentlyInGroup;

            do {
                // Add up to TRANSACTION_TARGETS of the left targets
                // In case a TransactionException is thrown this loop aborts
                final long assigned = assignTargetsToGroupInNewTransaction(rollout, group, groupTargetFilter,
                        Math.min(TRANSACTION_TARGETS, targetsLeftToAdd));
                if (assigned == 0) {
                    break; // percent > 100 or some could have disappeared
                } else {
                    targetsLeftToAdd -= assigned;
                }
            } while (targetsLeftToAdd > 0);

            group.setStatus(RolloutGroupStatus.READY);
            group.setTotalTargets(
                    DeploymentHelper.runInNewTransaction(txManager, "countRolloutTargetGroupByRolloutGroup",
                            count -> rolloutTargetGroupRepository.countByRolloutGroup(group)).intValue());
            return rolloutGroupRepository.save(group);

        } catch (final TransactionException e) {
            log.warn("Transaction assigning Targets to RolloutGroup failed", e);
            return group;
        }
    }

    private Long assignTargetsToGroupInNewTransaction(final JpaRollout rollout, final RolloutGroup group,
            final String targetFilter, final long limit) {
        return DeploymentHelper.runInNewTransaction(txManager, "assignTargetsToRolloutGroup", status -> {
            final PageRequest pageRequest = PageRequest.of(0, Math.toIntExact(limit));
            final List<Long> readyGroups = RolloutHelper.getGroupsByStatusIncludingGroup(rollout.getRolloutGroups(),
                    RolloutGroupStatus.READY, group);
            Slice<Target> targets;
            if (!RolloutHelper.isRolloutRetried(rollout.getTargetFilterQuery())) {
                targets = targetManagement.findByTargetFilterQueryAndNotInRolloutGroupsAndCompatibleAndUpdatable(
                        pageRequest, readyGroups, targetFilter, rollout.getDistributionSet().getType());
            } else {
                targets = targetManagement.findByFailedRolloutAndNotInRolloutGroups(
                        pageRequest, readyGroups, RolloutHelper.getIdFromRetriedTargetFilter(rollout.getTargetFilterQuery()));
            }

            createAssignmentOfTargetsToGroup(targets, group);

            return Long.valueOf(targets.getNumberOfElements());
        });
    }

    // return if group change is made
    private boolean fillDynamicRolloutGroupsWithTargets(final JpaRollout rollout) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.RUNNING);
        final List<RolloutGroup> rolloutGroups = rollout.getRolloutGroups();

        final JpaRolloutGroup group = (JpaRolloutGroup)rolloutGroups.get(rolloutGroups.size() - 1);

        if (group.getStatus() == RolloutGroupStatus.FINISHED) {
            createDynamicGroup(rollout, group, rolloutGroups.size(), RolloutGroupStatus.RUNNING);
            return true;
        } else if (group.getStatus() != RolloutGroupStatus.RUNNING) {
            return false;
        }

        // expected as last full group - last static or previously filled in dynamic group
        final long expectedInGroup = expectedDynamicGroupSize(rolloutGroups);

        final long currentlyInGroup = group.getTotalTargets();
        if (currentlyInGroup >= expectedInGroup) {
            // the last one is filled. create new and start filling it
            createDynamicGroup(rollout, group, rolloutGroups.size(), RolloutGroupStatus.SCHEDULED);
            return true;
        }

        // there are more to be filled for that group
        // do this until there are more matching
        try {
            long targetsLeftToAdd = expectedInGroup - currentlyInGroup;
            final String groupTargetFilter = RolloutHelper.getGroupTargetFilter(
                    // don't use RolloutHelper.getTargetFilterQuery(rollout)
                    // since it contains condition for device to be created
                    // before the rollout
                    rollout.getTargetFilterQuery(), group);
            long newActions = 0;
            do {
                // Add up to TRANSACTION_TARGETS actions of the left targets
                // In case a TransactionException is thrown this loop aborts
                final long createdActions = createActionsForDynamicGroupInNewTransaction(rollout, group, groupTargetFilter,
                        Math.min(TRANSACTION_TARGETS, targetsLeftToAdd));
                if (createdActions == 0) {
                    break; // no more to assign
                } else {
                    newActions += createdActions;
                    targetsLeftToAdd -= createdActions;
                }
            } while (targetsLeftToAdd > 0);

            if (newActions > 0) {
                updateTotalTargetCount(group, group.getTotalTargets() + newActions);

                if (targetsLeftToAdd == 0) {
                    // this is filled create a new one in sheduled state
                    createDynamicGroup(rollout, group, rolloutGroups.size(), RolloutGroupStatus.SCHEDULED);
                    return true;
                }

                // TODO - try to return false and proceed with handleRunningRollout
                // the problem is that OptimisticLockException is thrown in that case
                return true;
            }
        } catch (final TransactionException e) {
            log.warn("Transaction assigning Targets to RolloutGroup failed", e);
        }
        return false;
    }

    private void createDynamicGroup(final JpaRollout rollout, final RolloutGroup lastGroup, final int groupCount, final RolloutGroupStatus status) {
        final JpaRolloutGroup group = new JpaRolloutGroup();
        final String nameAndDesc = "group-" + (groupCount + 1) + "-dynamic";
        group.setDynamic(true);
        group.setName(nameAndDesc);
        group.setDescription(nameAndDesc);
        group.setRollout(rollout);
        group.setParent(lastGroup);
        // no need to be filled with targets, directly in ready (if first on create - it will be scheduled on start)
        // or scheduled state (for next dynamic groups)
        group.setStatus(status);
        group.setConfirmationRequired(lastGroup.isConfirmationRequired());

        group.setTargetPercentage(lastGroup.getTargetPercentage());
        group.setTargetFilterQuery(lastGroup.getTargetFilterQuery());

        addSuccessAndErrorConditionsAndActions(group, lastGroup.getSuccessCondition(),
                lastGroup.getSuccessConditionExp(), lastGroup.getSuccessAction(),
                lastGroup.getSuccessActionExp(), lastGroup.getErrorCondition(),
                lastGroup.getErrorConditionExp(), lastGroup.getErrorAction(),
                lastGroup.getErrorActionExp());

        final JpaRolloutGroup savedGroup = rolloutGroupRepository.save(group);
        rollout.setRolloutGroupsCreated(rollout.getRolloutGroupsCreated() + 1);
        rolloutRepository.save(rollout);
        ((JpaRolloutManagement) rolloutManagement).publishRolloutGroupCreatedEventAfterCommit(savedGroup, rollout);
    }

    private Long createActionsForDynamicGroupInNewTransaction(final JpaRollout rollout, final RolloutGroup group,
            final String targetFilter, final long limit) {
        return DeploymentHelper.runInNewTransaction(txManager, "createActionsForRolloutDynamicGroup", status -> {
            final PageRequest pageRequest = PageRequest.of(0, Math.toIntExact(limit));
            final Slice<Target> targets = targetManagement.findByNotInGEGroupAndNotInActiveActionGEWeightOrInRolloutAndTargetFilterQueryAndCompatibleAndUpdatable(
                    pageRequest,
                    rollout.getId(), rollout.getWeight().orElse(1000), // Dynamic rollouts shall always have weight!
                    rolloutGroupRepository.findByRolloutOrderByIdAsc(rollout).get(0).getId(),
                    targetFilter, rollout.getDistributionSet().getType());

            if (targets.getNumberOfElements() > 0) {
                final DistributionSet distributionSet = rollout.getDistributionSet();
                final ActionType actionType = rollout.getActionType();
                final long forceTime = rollout.getForcedTime();
                createActions(targets.getContent(), distributionSet, actionType, forceTime, rollout, group);
            }

            return Long.valueOf(targets.getNumberOfElements());
        });
    }

    /**
     * Schedules a group of the rollout. Scheduled Actions are created to
     * achieve this. The creation of those Actions is allowed to fail.
     */
    private boolean scheduleRolloutGroup(final JpaRollout rollout, final JpaRolloutGroup group) {
        final long targetsInGroup = rolloutTargetGroupRepository.countByRolloutGroup(group);
        final long countOfActions = actionRepository.countByRolloutAndRolloutGroup(rollout, group);

        long actionsLeft = targetsInGroup - countOfActions;
        if (actionsLeft > 0) {
            actionsLeft -= createActionsForRolloutGroup(rollout, group);
        }

        if (actionsLeft <= 0) {
            if (group.getStatus() != RolloutGroupStatus.SCHEDULED && group.getStatus() != RolloutGroupStatus.RUNNING) { // dynamic groups could already be running
                group.setStatus(RolloutGroupStatus.SCHEDULED);
                rolloutGroupRepository.save(group);
            }
            return true;
        }
        return false;
    }

    private long createActionsForRolloutGroup(final Rollout rollout, final RolloutGroup group) {
        long totalActionsCreated = 0;
        try {
            long actionsCreated;
            do {
                actionsCreated = createActionsForTargetsInNewTransaction(rollout, group, TRANSACTION_TARGETS);
                totalActionsCreated += actionsCreated;
            } while (actionsCreated > 0);

        } catch (final TransactionException e) {
            log.warn("Transaction assigning Targets to RolloutGroup failed", e);
            return 0;
        }
        return totalActionsCreated;
    }

    private Long createActionsForTargetsInNewTransaction(
            final Rollout rollout, final RolloutGroup group, final int limit) {
        return DeploymentHelper.runInNewTransaction(txManager, "createActionsForTargets", status -> {
            final Slice<Target> targets =
                    targetManagement.findByInRolloutGroupWithoutAction(PageRequest.of(0, limit), group.getId());

            if (targets.getNumberOfElements() > 0) {
                final DistributionSet distributionSet = rollout.getDistributionSet();
                final ActionType actionType = rollout.getActionType();
                final long forceTime = rollout.getForcedTime();
                createActions(targets.getContent(), distributionSet, actionType, forceTime, rollout, group);
            }

            return Long.valueOf(targets.getNumberOfElements());
        });
    }

    private void createAssignmentOfTargetsToGroup(final Slice<Target> targets, final RolloutGroup group) {
        targets.forEach(target -> rolloutTargetGroupRepository.save(new RolloutTargetGroup(group, target)));
    }

    /**
     * Creates an action entry into the action repository. In case of existing
     * scheduled actions the scheduled actions gets canceled. A scheduled action
     * is created in-active for static and running for dynamic groups.
     */
    private void createActions(final Collection<Target> targets, final DistributionSet distributionSet,
            final ActionType actionType, final Long forcedTime, final Rollout rollout,
            final RolloutGroup rolloutGroup) {
        // cancel all current scheduled actions for this target. E.g. an action
        // is already scheduled and a next action is created then cancel the
        // current scheduled action to cancel. E.g. a new scheduled action is
        // created.
        final List<Long> targetIds = targets.stream().map(Target::getId).collect(Collectors.toList());
        deploymentManagement.cancelInactiveScheduledActionsForTargets(targetIds);
        targets.forEach(target -> {
            assertActionsPerTargetQuota(target, 1);

            final JpaAction action = new JpaAction();
            action.setTarget(target);
            action.setActive(rolloutGroup.isDynamic());
            action.setDistributionSet(distributionSet);
            action.setActionType(actionType);
            action.setForcedTime(forcedTime);
            action.setStatus(rolloutGroup.isDynamic() ? Status.RUNNING : Status.SCHEDULED);
            action.setRollout(rollout);
            action.setRolloutGroup(rolloutGroup);
            action.setInitiatedBy(rollout.getCreatedBy());
            rollout.getWeight().ifPresent(action::setWeight);
            actionRepository.save(action);
        });
    }

    /**
     * Enforces the quota defining the maximum number of {@link Action}s per
     * {@link Target}.
     *
     * @param target
     *            The target
     * @param requested
     *            number of actions to check
     */
    private void assertActionsPerTargetQuota(final Target target, final int requested) {
        final int quota = quotaManagement.getMaxActionsPerTarget();
        QuotaHelper.assertAssignmentQuota(target.getId(), requested, quota, Action.class, Target.class,
                actionRepository::countByTargetId);
    }
}
