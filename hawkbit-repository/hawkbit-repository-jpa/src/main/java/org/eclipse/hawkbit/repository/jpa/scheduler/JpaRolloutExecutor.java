/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.scheduler;

import static org.eclipse.hawkbit.context.AccessContext.asActor;
import static org.eclipse.hawkbit.context.AccessContext.asSystem;
import static org.eclipse.hawkbit.context.AccessContext.withSecurityContext;
import static org.eclipse.hawkbit.repository.jpa.JpaManagementHelper.combineWithAnd;
import static org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor.afterCommit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutExecutor;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutHelper;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.RolloutStoppedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.management.JpaRolloutManagement;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.EvaluatorNotConfiguredException;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupEvaluationManager;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.util.ObjectUtils;

/**
 * A Jpa implementation of {@link RolloutExecutor}
 */
@Slf4j
@Service
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
    private static final List<Status> DEFAULT_ACTION_TERMINATION_STATUSES = List.of(Status.ERROR, Status.FINISHED, Status.CANCELED);
    /**
     * In case of DOWNLOAD_ONLY, actions can be finished with DOWNLOADED status.
     */
    private static final List<Status> DOWNLOAD_ONLY_ACTION_TERMINATION_STATUSES =
            List.of(Status.ERROR, Status.FINISHED, Status.CANCELED, Status.DOWNLOADED);
    private static final String TRANSACTION_ASSIGNING_TARGETS_TO_ROLLOUT_GROUP_FAILED = "Transaction assigning Targets to RolloutGroup failed";

    private final ActionRepository actionRepository;
    private final RolloutGroupRepository rolloutGroupRepository;
    private final RolloutTargetGroupRepository rolloutTargetGroupRepository;
    private final RolloutRepository rolloutRepository;
    private final TargetRepository targetRepository;
    private final DeploymentManagement deploymentManagement;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final RolloutManagement rolloutManagement;
    private final QuotaManagement quotaManagement;
    private final RolloutGroupEvaluationManager evaluationManager;
    private final RolloutApprovalStrategy rolloutApprovalStrategy;
    private final EntityManager entityManager;
    private final PlatformTransactionManager txManager;
    private final RepositoryProperties repositoryProperties;
    private final Map<Long, AtomicLong> lastDynamicGroupFill = new ConcurrentHashMap<>();

    @SuppressWarnings("java:S107")
    public JpaRolloutExecutor(
            final ActionRepository actionRepository, final RolloutGroupRepository rolloutGroupRepository,
            final RolloutTargetGroupRepository rolloutTargetGroupRepository,
            final RolloutRepository rolloutRepository, final TargetRepository targetRepository,
            final DeploymentManagement deploymentManagement, final RolloutGroupManagement rolloutGroupManagement,
            final RolloutManagement rolloutManagement, final QuotaManagement quotaManagement,
            final RolloutGroupEvaluationManager evaluationManager, final RolloutApprovalStrategy rolloutApprovalStrategy,
            final EntityManager entityManager, final PlatformTransactionManager txManager,
            final RepositoryProperties repositoryProperties) {
        this.actionRepository = actionRepository;
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.rolloutTargetGroupRepository = rolloutTargetGroupRepository;
        this.rolloutRepository = rolloutRepository;
        this.targetRepository = targetRepository;
        this.deploymentManagement = deploymentManagement;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.rolloutManagement = rolloutManagement;
        this.quotaManagement = quotaManagement;
        this.evaluationManager = evaluationManager;
        this.rolloutApprovalStrategy = rolloutApprovalStrategy;
        this.entityManager = entityManager;
        this.txManager = txManager;
        this.repositoryProperties = repositoryProperties;
    }

    @Override
    public boolean execute(final Rollout rollout) {
        final AtomicBoolean processAgain = new AtomicBoolean();
        // the access control context is a large, lazily mapped @Lob that is not exposed on the entity; resolve it
        // explicitly through the management layer (rollout processing is already one-per-transaction, so no N+1)
        rolloutManagement.getAccessControlContext(rollout.getId()).ifPresentOrElse(
                // has stored context - executes it with it
                context -> withSecurityContext(context, () -> processAgain.set(execute0(rollout))),
                // has no stored context - executes it in the tenant & user scope
                () -> asActor(rollout.getCreatedBy(), () -> processAgain.set(execute0(rollout))));
        return processAgain.get();
    }

    // returns true if the rollout shall be processed again right away (see RolloutExecutor#execute)
    private boolean execute0(final Rollout rollout) {
        log.debug("Processing rollout {}", rollout.getId());

        boolean processAgain = false;
        switch (rollout.getStatus()) {
            case CREATING:
                handleCreateRollout((JpaRollout) rollout);
                break;
            case READY:
                handleReadyRollout(rollout);
                break;
            case STARTING:
                // the lastModifiedBy user is probably the user that has actually called the rollout start (unless overridden) - not the creator
                asActor(rollout.getLastModifiedBy(), () -> handleStartingRollout((JpaRollout) rollout));
                break;
            case RUNNING:
                processAgain = handleRunningRollout((JpaRollout) rollout);
                break;
            case STOPPING:
                // the lastModifiedBy user is probably the user that has actually called the rollout stop (unless overridden) - not the creator
                asActor(rollout.getLastModifiedBy(), () -> handleStopRollout((JpaRollout) rollout));
                break;
            case DELETING:
                // the lastModifiedBy user is probably the user that has actually called the rollout delete (unless overridden) - not the creator
                asActor(rollout.getLastModifiedBy(), () -> handleDeleteRollout((JpaRollout) rollout));
                break;
            default:
                log.error("Rollout in status {} not supposed to be handled!", rollout.getStatus());
                break;
        }

        log.debug("Rollout {} processed", rollout.getId());
        return processAgain;
    }

    private void handleCreateRollout(final JpaRollout rollout) {
        log.debug("handleCreateRollout called for rollout {}", rollout.getId());

        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(
                        rollout.getId(),
                        PageRequest.of(0, quotaManagement.getMaxRolloutGroupsPerRollout(), Sort.by(Direction.ASC, "id")))
                .getContent();

        int readyGroups = 0;
        int totalTargets = 0;
        // count of the targets of the rollout's target set that are in no READY group, carried from group to group within
        // this run, so that it is counted once per run and not once per group (null - unknown, has to be counted)
        Long rolloutCandidates = null;
        for (final RolloutGroup group : rolloutGroups) {
            if (RolloutGroupStatus.READY == group.getStatus()) {
                readyGroups++;
                totalTargets += group.getTotalTargets();
                continue;
            }

            final GroupFill fill = fillRolloutGroupWithTargets(rollout, (JpaRolloutGroup) group, rolloutGroups, rolloutCandidates);
            if (!fill.ready()) {
                // the group is filled in order: a later group must not take targets this group would get. The committed pages
                // are kept and the next run continues with this group
                break;
            }
            readyGroups++;
            totalTargets += fill.totalTargets();
            rolloutCandidates = fill.rolloutCandidatesLeft();
        }

        // When all groups are ready the rollout status can be changed to be ready, too.
        if (readyGroups == rolloutGroups.size()) {
            if (rollout.isDynamic() && !rolloutGroups.get(rolloutGroups.size() - 1).isDynamic()) {
                // add first dynamic group one by using the last as a parent and as a pattern
                createDynamicGroup(
                        rollout, (JpaRolloutGroup) rolloutGroups.get(rolloutGroups.size() - 1), rolloutGroups.size(), RolloutGroupStatus.READY);
            }

            if (!rolloutApprovalStrategy.isApprovalNeeded(rollout)) {
                log.debug("rollout {} creation done. Switch to READY.", rollout.getId());
                rollout.setStatus(RolloutStatus.READY);
            } else {
                log.debug("rollout {} creation done. Switch to WAITING_FOR_APPROVAL.", rollout.getId());
                rollout.setStatus(RolloutStatus.WAITING_FOR_APPROVAL);
                rolloutApprovalStrategy.onApprovalRequired(rollout);
            }
            rollout.setTotalTargets(totalTargets);
            rollout.setLastCheck(0);
            rolloutRepository.save(rollout);
        }
    }

    private void handleDeleteRollout(final JpaRollout rollout) {
        log.debug("handleDeleteRollout called for {}", rollout.getId());

        // check if there are actions beyond schedule
        boolean hardDeleteRolloutGroups = !actionRepository.existsByRolloutIdAndStatusNot(rollout.getId(),
                Status.SCHEDULED);
        if (hardDeleteRolloutGroups) {
            log.debug("Rollout {} has no actions other than scheduled -> hard delete", rollout.getId());
            hardDeleteRollout(rollout);
            return;
        }
        // clean up all scheduled actions
        deleteScheduledActions(rollout, findScheduledActionIdsByRollout(rollout));

        // avoid another scheduler round and re-check if all scheduled actions has been cleaned up.
        // we flush first to ensure that will include the deletion above
        entityManager.flush();
        final boolean hasScheduledActionsLeft = actionRepository.countByRolloutIdAndStatus(rollout.getId(), Status.SCHEDULED) > 0;

        if (hasScheduledActionsLeft) {
            return;
        }

        // only hard delete the rollout if no actions are left for the rollout.
        // In case actions are left, they are probably are running or were running before, so only soft delete.
        hardDeleteRolloutGroups = !actionRepository.existsByRolloutId(rollout.getId());
        if (hardDeleteRolloutGroups) {
            hardDeleteRollout(rollout);
            return;
        }

        finishRolloutGroups(rollout);

        rolloutManagement.cancelActiveActionsForRollouts(rollout, ActionCancellationType.FORCE);
        entityManager.flush();

        boolean hasActiveActionsLeft = actionRepository.countByRolloutIdAndActive(rollout.getId(), true) > 0;
        log.trace("rollout {} has active actions left : {}  ", rollout.getId(), hasActiveActionsLeft);
        if (!hasActiveActionsLeft) {
            // set soft delete
            rollout.setStatus(RolloutStatus.DELETED);
            rollout.setDeleted(true);
            rolloutRepository.save(rollout);
        }
    }

    private void handleStopRollout(final JpaRollout rollout) {
        log.debug("handleStopRollout called for {}", rollout.getId());
        // clean up all scheduled actions
        deleteScheduledActions(rollout, findScheduledActionIdsByRollout(rollout));

        // avoid another scheduler round and re-check if all scheduled actions has been cleaned up. we flush first to ensure that
        // we include the deletion above
        entityManager.flush();
        final boolean hasScheduledActionsLeft = actionRepository.countByRolloutIdAndStatus(rollout.getId(), Status.SCHEDULED) > 0;

        if (hasScheduledActionsLeft) {
            return;
        }

        finishRolloutGroups(rollout);

        // Soft cancel all active rollouts actions
        rolloutManagement.cancelActiveActionsForRollouts(rollout, ActionCancellationType.SOFT);
        // check if all actions are non-active and then finish or finish once all are processed.
        boolean hasActiveActions = actionRepository.countByRolloutIdAndActiveAndStatusNot(rollout.getId(), true, Status.CANCELING) > 0;
        if (!hasActiveActions) {
            rollout.setStatus(RolloutStatus.STOPPED);
            rolloutRepository.save(rollout);

            final List<Long> groupIds = rollout.getRolloutGroups().stream().map(RolloutGroup::getId).toList();
            afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                    new RolloutStoppedEvent(AccessContext.tenant(), rollout.getId(), groupIds)));
        }
    }

    private void handleReadyRollout(final Rollout rollout) {
        if (rollout.getStartAt() != null && rollout.getStartAt() <= System.currentTimeMillis()) {
            log.debug("handleReadyRollout called for rollout {} with autostart beyond define time. Switch to STARTING", rollout.getId());
            rolloutManagement.start(rollout.getId());
        }
    }

    private void handleStartingRollout(final JpaRollout rollout) {
        log.debug("handleStartingRollout called for rollout {}", rollout.getId());

        if (ensureAllGroupsAreScheduled(rollout)) {
            startFirstRolloutGroup(rollout);

            rollout.setStatus(RolloutStatus.RUNNING);
            rollout.setLastCheck(0);
            rolloutRepository.save(rollout);
        }
    }

    // returns true if a group has been started, so that the rollout is processed again right away (the success condition of
    // the started group may be reached already, e.g. 0%) - each time in a new transaction, so that it sees the started actions
    private boolean handleRunningRollout(final JpaRollout rollout) {
        log.debug("handleRunningRollout called for rollout {}", rollout.getId());

        if (rollout.isDynamic() && fillDynamicRolloutGroupsWithTargets(rollout)) {
            log.debug("Dynamic group created for rollout {}", rollout.getId());
            return false;
        }

        final List<JpaRolloutGroup> runningGroups = rollout.getRolloutGroups().stream()
                .filter(group -> group.getStatus() == RolloutGroupStatus.RUNNING)
                .map(JpaRolloutGroup.class::cast)
                .toList();
        if (runningGroups.isEmpty()) {
            // no running rollouts, probably there was an error somewhere at the latest group. And the latest group has
            // been switched from running into error state. So we need to find the latest group which
            asSystem(() -> rolloutManagement.triggerNextGroup(rollout.getId()));
        } else {
            log.debug("Rollout {} has {} running groups", rollout.getId(), runningGroups.size());
            final RolloutGroup lastGroup = rollout.getRolloutGroups().get(rollout.getRolloutGroups().size() - 1);
            executeRunningGroups(rollout, runningGroups, lastGroup);
        }

        if (isRolloutComplete(rollout)) {
            log.info("Rollout {} is finished, setting FINISHED status", rollout);
            rollout.setStatus(RolloutStatus.FINISHED);
            rolloutRepository.save(rollout);
            return false;
        }
        // the group status is set by a bulk update - read from the DB (the own changes are visible to the own transaction)
        final Set<Long> runningBefore = runningGroups.stream().map(JpaRolloutGroup::getId).collect(Collectors.toSet());
        return rolloutGroupRepository.findIdsByRolloutIdAndStatus(rollout.getId(), RolloutGroupStatus.RUNNING).stream()
                .anyMatch(groupId -> !runningBefore.contains(groupId));
    }

    private void hardDeleteRollout(final JpaRollout rollout) {
        rolloutRepository.delete(rollout);
    }

    private void deleteScheduledActions(final JpaRollout rollout, final List<Long> scheduledActionIds) {
        if (!scheduledActionIds.isEmpty()) {
            // has scheduled actions - delete them
            try {
                final List<List<Long>> chunks = ListUtils.partition(scheduledActionIds, Constants.MAX_ENTRIES_IN_STATEMENT);
                // the delete access check of the repository's deleteAllById
                actionRepository.getAccessController().ifPresent(accessController -> chunks.forEach(chunk -> {
                    if (actionRepository.count(AccessController.Operation.DELETE, actionRepository.byIdsSpec(chunk)) != chunk.size()) {
                        throw new InsufficientPermissionException("Has at least one id that is not allowed for deletion!");
                    }
                }));
                // bulk DELETE instead of an entity remove per action, which reads the action statuses of each action first (cascade).
                // The action statuses are deleted by the DB (ON DELETE CASCADE) - a new child table of sp_action needs it too.
                // Pending changes are flushed before. No action of the rollout is loaded here (ids only), so no managed entity
                // gets stale - the persistence context isn't cleared, as the rollout handling goes on with the managed rollout
                entityManager.flush();
                chunks.forEach(chunk -> actionRepository.deleteByIdsAndStatus(chunk, Status.SCHEDULED));
                afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new RolloutUpdatedEvent(rollout)));
            } catch (final RuntimeException e) {
                log.error("Exception during deletion of actions of rollout {}", rollout, e);
            }
        }
    }

    private void finishRolloutGroups(final JpaRollout rollout) {
        rolloutGroupRepository.findByRolloutAndStatusNotIn(rollout, List.of(RolloutGroupStatus.FINISHED, RolloutGroupStatus.ERROR))
                .forEach(rolloutGroup -> {
                    rolloutGroup.setStatus(RolloutGroupStatus.FINISHED);
                    rolloutGroupRepository.save(rolloutGroup);
                });
    }

    private List<Long> findScheduledActionIdsByRollout(final JpaRollout rollout) {
        return actionRepository.findIdsByRolloutIdAndStatus(rollout.getId(), Status.SCHEDULED, Limit.of(TRANSACTION_ACTIONS));
    }

    private boolean isRolloutComplete(final JpaRollout rollout) {
        // ensure that changes in the same transaction count
        entityManager.flush();
        final Long groupsActiveLeft = rolloutGroupRepository.countByRolloutIdAndStatusOrStatus(rollout.getId(),
                RolloutGroupStatus.RUNNING, RolloutGroupStatus.SCHEDULED);
        return groupsActiveLeft == 0;
    }

    // fakes getTotalTargets count to match expected for the last dynamic group
    // so the evaluation to use total targets to properly
    private RolloutGroup evalProxy(final RolloutGroup group) {
        if (group.isDynamic()) {
            final int expected = Math.max((int) group.getTargetPercentage(), 1);
            return (RolloutGroup) Proxy.newProxyInstance(
                    RolloutGroup.class.getClassLoader(),
                    new Class<?>[] { RolloutGroup.class },
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

    private void executeRunningGroups(final JpaRollout rollout, final List<JpaRolloutGroup> runningGroups, final RolloutGroup lastGroup) {
        for (final JpaRolloutGroup rolloutGroup : runningGroups) {
            // handle eventual deletion of devices, which might reflect the success condition
            final long targetCount = countTargetsFrom(rolloutGroup);
            if (rolloutGroup.getTotalTargets() != targetCount) {
                updateTotalTargetCount(rolloutGroup, targetCount);
            }

            final RolloutGroup evalProxy = rolloutGroup == runningGroups.get(runningGroups.size() - 1) ? evalProxy(rolloutGroup) : rolloutGroup;
            // error state check, do we need to stop the whole rollout because of error?
            final boolean isError = checkErrorState(rollout, evalProxy);
            if (isError) {
                log.info("Rollout {} {} has error, calling error action", rollout.getName(), rollout.getId());
                callErrorAction(rollout, rolloutGroup);
            } else {// not in error so check success condition and group completed
                // 'success' is either group completed or success condition reached - execute 'success' Action
                final boolean groupCompleted = !(rolloutGroup == lastGroup && rolloutGroup.isDynamic())
                        && isRolloutGroupComplete(rollout, rolloutGroup);
                checkSuccessCondition(rollout, rolloutGroup, evalProxy, rolloutGroup.getSuccessCondition(), groupCompleted);
                if (groupCompleted) {
                    rolloutGroup.setStatus(RolloutGroupStatus.FINISHED);
                    rolloutGroupRepository.save(rolloutGroup);
                }
            }
        }
    }

    private void updateTotalTargetCount(final JpaRolloutGroup rolloutGroup, final long countTargetsOfRolloutGroup) {
        final JpaRollout jpaRollout = rolloutGroup.getRollout();
        final long updatedTargetCount = jpaRollout.getTotalTargets() - (rolloutGroup.getTotalTargets() - countTargetsOfRolloutGroup);
        jpaRollout.setTotalTargets(updatedTargetCount);
        rolloutGroup.setTotalTargets((int) countTargetsOfRolloutGroup);
        rolloutRepository.save(jpaRollout);
        rolloutGroupRepository.save(rolloutGroup);
    }

    private long countTargetsFrom(final JpaRolloutGroup rolloutGroup) {
        // Use action-based count for all groups: deleting an action removes the target from the count,
        // keeping totalTargets consistent with the actual denominator used by condition evaluators.
        return countByActionsInRolloutGroup(rolloutGroup.getId());
    }

    private void callErrorAction(final Rollout rollout, final RolloutGroup rolloutGroup) {
        try {
            evaluationManager.getErrorActionEvaluator(rolloutGroup.getErrorAction()).exec(rollout, rolloutGroup);
        } catch (final EvaluatorNotConfiguredException e) {
            log.error("Something bad happened when accessing the error action bean {}", rolloutGroup.getErrorAction().name(), e);
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
            return evaluationManager
                    .getErrorConditionEvaluator(errorCondition)
                    .eval(rollout, rolloutGroup, rolloutGroup.getErrorConditionExp());
        } catch (final EvaluatorNotConfiguredException e) {
            log.error("Something bad happened when accessing the error condition bean {}", errorCondition.name(), e);
            return false;
        }
    }

    private void checkSuccessCondition(final Rollout rollout, final RolloutGroup rolloutGroup, final RolloutGroup evalProxy,
            final RolloutGroupSuccessCondition successCondition, final boolean groupCompleted) {
        log.trace("Checking finish condition {} on rolloutgroup {}", successCondition, rolloutGroup);
        try {
            if (groupCompleted || evaluationManager
                    .getSuccessConditionEvaluator(successCondition)
                    .eval(rollout, evalProxy, rolloutGroup.getSuccessConditionExp())) {
                log.debug("Rollout group {} fulfills SuccessCondition or is Finished, executing Success Action", rolloutGroup);
                evaluationManager.getSuccessActionEvaluator(rolloutGroup.getSuccessAction()).exec(rollout, rolloutGroup);
            } else {
                log.debug("Rollout group {} is still running", rolloutGroup);
            }
        } catch (final EvaluatorNotConfiguredException e) {
            log.error("Something bad happened when accessing the finish condition or success action bean {}", successCondition.name(), e);
        }
    }

    private void startFirstRolloutGroup(final JpaRollout rollout) {
        log.debug("startFirstRolloutGroup called for rollout {}", rollout.getId());

        final List<JpaRolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutOrderByIdAsc(rollout);
        final JpaRolloutGroup rolloutGroup = rolloutGroups.get(0);
        if (rolloutGroup.getParent() != null) {
            throw new RolloutIllegalStateException("First found group is not the first group (has a parent).");
        }

        deploymentManagement.startScheduledActionsByRolloutGroupParent(rollout.getId(), rollout.getDistributionSet().getId(), null);

        rolloutGroup.setStatus(RolloutGroupStatus.RUNNING);
        rolloutGroupRepository.save(rolloutGroup);
    }

    private boolean ensureAllGroupsAreScheduled(final Rollout rollout) {
        final List<JpaRolloutGroup> groupsToBeScheduled = rolloutGroupRepository.findByRolloutAndStatus(rollout, RolloutGroupStatus.READY);
        if (groupsToBeScheduled.isEmpty()) {
            return true;
        }

        final long scheduledGroups = groupsToBeScheduled.stream().filter(group -> scheduleRolloutGroup((JpaRollout) rollout, group)).count();
        entityManager.flush(); // flush groups so scheduled group to start to have scheduled event
        return scheduledGroups == groupsToBeScheduled.size();
    }

    /**
     * Fills the group with its targets in pages (one transaction per page), reading the candidates ordered by id from a
     * cursor (keyset paging): a page continues where the previous one ended, instead of scanning again past the targets
     * already assigned. A page only adds rollout target group rows; the group is set READY with its total targets by the
     * transaction of the run (as its group instance is the one the next groups read). The cursor is not stored - it is
     * derived from the assigned targets, so after a failure the committed pages are kept and the next run continues the
     * group (the key of the rollout target group prevents duplicates). If the candidates after the cursor run out before
     * the group is complete, the candidates are read once more from the start, for targets with a lower id matching by now.
     * <p/>
     * The candidates are taken in id order, so all candidates of a group with an id up to the max id assigned to the group
     * are in the group (or in a group before). For a resumed fill the cursor is the max assigned id of the group. A group
     * that selects from the rollout's target set as is (no own target filter, or a retried rollout, which ignores the group
     * filters) continues after the max assigned id of the nearest such group before it: all targets of the rollout's
     * target set up to that id are already in that group or in a group before. So these groups share one pass over the
     * targets, and the cost of a fill doesn't grow with the number of groups before it. The NOT IN guard of the READY
     * groups stays, for the targets taken by groups with an own target filter, which may take any targets of the set.
     *
     * @param rolloutCandidates the count of the targets of the rollout's target set not in a READY group, if known (carried
     *         from the group before in the same run), otherwise {@code null}
     */
    private GroupFill fillRolloutGroupWithTargets(
            final JpaRollout rollout, final JpaRolloutGroup group, final List<RolloutGroup> rolloutGroups, final Long rolloutCandidates) {
        final boolean retried = RolloutHelper.isRolloutRetried(rollout.getTargetFilterQuery());
        final boolean rolloutTargetSet = retried || ObjectUtils.isEmpty(group.getTargetFilterQuery());
        final String groupTargetFilter = RolloutHelper.getGroupTargetFilter(RolloutHelper.getTargetFilterQuery(rollout), group);
        final long dsTypeId = rollout.getDistributionSet().getTypeId();
        final String failedRolloutId = retried ? RolloutHelper.getIdFromRetriedTargetFilter(rollout.getTargetFilterQuery()) : null;

        final long inGroupBefore = DeploymentHelper.runInNewTransaction(
                "countRolloutTargetGroupByRolloutGroup", _ -> rolloutTargetGroupRepository.countByRolloutGroup(group), txManager);

        // the group itself is not excluded from the count: the targets assigned by an interrupted fill are candidates of the
        // group, so that a resumed fill expects the same size as an uninterrupted one
        final List<Long> otherReadyGroups = rolloutGroups.stream()
                .filter(other -> other.getStatus() == RolloutGroupStatus.READY && !other.equals(group))
                .map(RolloutGroup::getId)
                .toList();
        final long candidates;
        if (rolloutTargetSet && rolloutCandidates != null) {
            candidates = rolloutCandidates;
        } else if (!retried) { // default case
            candidates = DeploymentHelper.runInNewTransaction(
                    "countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable",
                    _ -> countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(groupTargetFilter, otherReadyGroups, dsTypeId),
                    txManager);
        } else { // if it is a rollout retry
            candidates = DeploymentHelper.runInNewTransaction(
                    "countByFailedRolloutAndNotInRolloutGroupsAndCompatible",
                    _ -> countByFailedRolloutAndNotInRolloutGroups(failedRolloutId, otherReadyGroups), txManager);
        }

        final double percentFromTheRest;
        if (rollout.isNewStyleTargetPercent()) { // new style percent - total percent
            percentFromTheRest = RolloutHelper.toPercentFromTheRest(group, rolloutGroups);
        } else { // old style percent - percent from rest
            percentFromTheRest = group.getTargetPercentage();
        }
        final long expectedInGroup = Math.round(percentFromTheRest * candidates / 100);

        // NOT IN guard - the READY groups and the group itself (never empty, an empty IN isn't valid)
        final List<Long> excludedGroups = RolloutHelper.getGroupsByStatusIncludingGroup(rolloutGroups, RolloutGroupStatus.READY, group);
        final List<Long> cursorGroups = rolloutTargetSet ? previousRolloutTargetSetGroup(group, rolloutGroups, retried)
                .map(previous -> List.of(group.getId(), previous.getId()))
                .orElseGet(() -> List.of(group.getId())) : List.of(group.getId());
        try {
            final long startCursor = DeploymentHelper.runInNewTransaction("findMaxTargetIdOfRolloutGroups", _ -> cursorGroups.stream()
                    .map(rolloutTargetGroupRepository::findMaxTargetIdByRolloutGroupId)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .max().orElse(0L), txManager);
            long cursor = startCursor;
            boolean fromStart = startCursor == 0;
            long inGroup = inGroupBefore;
            while (inGroup < expectedInGroup) {
                final long afterId = cursor;
                final int limit = (int) Math.min(Constants.MAX_ENTRIES_IN_STATEMENT, expectedInGroup - inGroup);
                // a page in its own transaction: only the rollout target group rows - the group itself is updated by this
                // (outer) transaction below, as before, so its instance here stays current and committed pages are kept
                final List<Long> targetIds = DeploymentHelper.runInNewTransaction("assignTargetsToRolloutGroup", _ -> {
                    final List<Long> pageTargetIds = retried
                            ? findIdsByFailedRolloutAndNotInRolloutGroups(failedRolloutId, excludedGroups, afterId, limit)
                            : findIdsByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(
                                    excludedGroups, groupTargetFilter, dsTypeId, afterId, limit);
                    final JpaRolloutGroup groupReference = entityManager.getReference(JpaRolloutGroup.class, group.getId());
                    // persist with references instead of saveAll: the RolloutTargetGroup has an assigned (composite) id, so saveAll
                    // would merge, i.e. read every row first - and the targets themselves are not needed, only their ids
                    pageTargetIds.forEach(targetId -> entityManager.persist(
                            new RolloutTargetGroup(groupReference, entityManager.getReference(JpaTarget.class, targetId))));
                    return pageTargetIds;
                }, txManager);
                inGroup += targetIds.size();
                if (!targetIds.isEmpty()) {
                    cursor = targetIds.getLast();
                }
                if (targetIds.size() < limit) {
                    if (fromStart) {
                        break; // no more candidates
                    }
                    // no more candidates after the cursor - targets with a lower id may match by now (e.g. changed since the
                    // group before was filled), so read once more from the start (the NOT IN guard skips the assigned ones)
                    fromStart = true;
                    cursor = 0;
                }
            }

            group.setStatus(RolloutGroupStatus.READY);
            group.setTotalTargets(Math.toIntExact(inGroup));
            rolloutGroupRepository.save(group);

            // carry the candidates count to the next group - only if this group has been filled from scratch in this run, so
            // that all its targets are of the counted ones (all targets of a group are of the rollout's target set)
            final long assigned = inGroup - inGroupBefore;
            final Long rolloutCandidatesLeft;
            if (inGroupBefore != 0) {
                rolloutCandidatesLeft = null;
            } else if (rolloutTargetSet) {
                rolloutCandidatesLeft = candidates - assigned;
            } else {
                rolloutCandidatesLeft = rolloutCandidates == null ? null : rolloutCandidates - assigned;
            }
            return new GroupFill(true, Math.toIntExact(inGroup), rolloutCandidatesLeft);
        } catch (final TransactionException e) {
            log.warn(TRANSACTION_ASSIGNING_TARGETS_TO_ROLLOUT_GROUP_FAILED, e);
            return new GroupFill(false, 0, null);
        }
    }

    // the nearest READY group before the group that selects from the rollout's target set as is and has targets - it has the
    // highest target id of these groups, as each of them continues after the one before
    private static Optional<RolloutGroup> previousRolloutTargetSetGroup(
            final RolloutGroup group, final List<RolloutGroup> rolloutGroups, final boolean retried) {
        final int index = rolloutGroups.indexOf(group);
        for (int i = index - 1; i >= 0; i--) {
            final RolloutGroup previous = rolloutGroups.get(i);
            if (previous.getStatus() == RolloutGroupStatus.READY && previous.getTotalTargets() > 0
                    && (retried || ObjectUtils.isEmpty(previous.getTargetFilterQuery()))) {
                return Optional.of(previous);
            }
        }
        return Optional.empty();
    }

    // result of a group fill: if the group got READY, its total targets and the count of the rollout's target set candidates
    // left for the next group (null - unknown)
    private record GroupFill(boolean ready, int totalTargets, Long rolloutCandidatesLeft) {}

    // return if group change is made
    private boolean fillDynamicRolloutGroupsWithTargets(final JpaRollout rollout) {
        final AtomicLong lastFill = lastDynamicGroupFill.computeIfAbsent(rollout.getId(), id -> new AtomicLong(0));
        final long now = System.currentTimeMillis();
        if (now - lastFill.get() < repositoryProperties.getDynamicRolloutsMinInvolvePeriodMS()) {
            // too early to make another dynamic involvement attempt
            return false;
        }

        final List<RolloutGroup> rolloutGroups = rollout.getRolloutGroups();
        final JpaRolloutGroup group = (JpaRolloutGroup) rolloutGroups.get(rolloutGroups.size() - 1);
        final long expectedInGroup = Math.max((int) group.getTargetPercentage(), 1);
        final long currentlyInGroup = group.getTotalTargets();
        if (currentlyInGroup >= expectedInGroup || group.getStatus() == RolloutGroupStatus.FINISHED) {
            // the last one is full. create new and start filling it on the next iteration
            createDynamicGroup(rollout, group, rolloutGroups.size(), RolloutGroupStatus.SCHEDULED);
            // don't update lastFill - want to run again next time to start filling in
            return true;
        }

        // there are more to be filled for the last group do this until there are more matching
        try {
            long targetsLeftToAdd = expectedInGroup - currentlyInGroup;
            final String groupTargetFilter = RolloutHelper.getGroupTargetFilter(
                    // don't use RolloutHelper.getTargetFilterQuery(rollout)
                    // since it contains condition for device to be created before the rollout
                    rollout.getTargetFilterQuery(), group);

            final Slice<Target> targets = findByRsqlAndNoOverridingActionsAndNotInRolloutAndCompatibleAndUpdatable(
                    rollout.getId(), groupTargetFilter, rollout.getDistributionSet().getTypeId(),
                    PageRequest.of(0, Math.toIntExact(Math.min(TRANSACTION_TARGETS, targetsLeftToAdd))));
            if (targets.getNumberOfElements() > 0) {
                final List<Action> newActions = createActions(
                        targets.getContent(), rollout.getDistributionSet(), rollout.getActionType(), rollout.getForcedTime(),
                        rollout, group);
                if (!newActions.isEmpty() && group.getStatus() == RolloutGroupStatus.RUNNING) {
                    deploymentManagement.startScheduledActions(newActions);
                }

                // updates the total targets of the current group and the rollout - new actions size is the same as targets size
                group.setTotalTargets(group.getTotalTargets() + newActions.size());
                rolloutGroupRepository.save(group);
                rollout.setTotalTargets(rollout.getTotalTargets() + newActions.size());
                rolloutRepository.save(rollout);

                // TODO - try to return false and proceed with handleRunningRollout
                // the problem is that OptimisticLockException is thrown in that case

                // don't update lastFill - want to run again next time in case there are more
                return true;
            }
        } catch (final TransactionException e) {
            log.warn(TRANSACTION_ASSIGNING_TARGETS_TO_ROLLOUT_GROUP_FAILED, e);
        }

        // set to skip for some time
        lastFill.set(now);
        return false;
    }

    private void createDynamicGroup(
            final JpaRollout rollout, final JpaRolloutGroup lastGroup, final int groupCount, final RolloutGroupStatus status) {
        try {
            RolloutHelper.verifyRolloutGroupAmount(groupCount + 1, quotaManagement);
        } catch (final AssignmentQuotaExceededException e) {
            log.warn("Quota exceeded for dynamic rollout group creation: {}. Stop it", e.getMessage());
            if (isRolloutComplete(rollout)) {
                rollout.setStatus(RolloutStatus.STOPPED);
                rolloutRepository.save(rollout);
            }
            return;
        }

        final JpaRolloutGroup group = new JpaRolloutGroup();
        final String lastGroupWithoutSuffix = "group-" + groupCount;
        final String suffix = lastGroup.getName().startsWith(lastGroupWithoutSuffix)
                ? lastGroup.getName().substring(lastGroupWithoutSuffix.length())
                : "";
        final String nameAndDesc = "group-" + (groupCount + 1) + suffix;
        group.setName(nameAndDesc);
        group.setDescription(nameAndDesc);
        group.setRollout(rollout);
        group.setParent(lastGroup);
        group.setDynamic(true);
        // no need to be filled with targets, directly in ready (if first on create - it will be scheduled on start)
        // or scheduled state (for next dynamic groups)
        group.setStatus(status);
        group.setConfirmationRequired(lastGroup.isConfirmationRequired());

        // for dynamic groups the target count is kept in target percentage
        group.setTargetPercentage(lastGroup.isDynamic() ? lastGroup.getTargetPercentage() : lastGroup.getTotalTargets());
        group.setTargetFilterQuery(lastGroup.getTargetFilterQuery());

        JpaRolloutManagement.addSuccessAndErrorConditionsAndActions(group, lastGroup.getSuccessCondition(),
                lastGroup.getSuccessConditionExp(), lastGroup.getSuccessAction(),
                lastGroup.getSuccessActionExp(), lastGroup.getErrorCondition(),
                lastGroup.getErrorConditionExp(), lastGroup.getErrorAction(),
                lastGroup.getErrorActionExp());

        final JpaRolloutGroup savedGroup = rolloutGroupRepository.save(group);
        rollout.setRolloutGroupsCreated(rollout.getRolloutGroupsCreated() + 1);
        rolloutRepository.save(rollout);
        ((JpaRolloutManagement) rolloutManagement).publishRolloutGroupCreatedEventAfterCommit(savedGroup, rollout);
    }

    /**
     * Schedules a group of the rollout. Scheduled Actions are created to achieve this. The creation of those Actions is allowed to fail.
     */
    private boolean scheduleRolloutGroup(final JpaRollout rollout, final JpaRolloutGroup group) {
        final long targetsInGroup = rolloutTargetGroupRepository.countByRolloutGroup(group);
        final long countOfActions = actionRepository.countByRolloutAndRolloutGroup(rollout, group);

        long actionsLeft = targetsInGroup - countOfActions;
        if (actionsLeft > 0) {
            actionsLeft -= createActionsForRolloutGroup(rollout, group);
        }

        if (actionsLeft > 0) {
            return false;
        } else {
            if (group.getStatus() != RolloutGroupStatus.SCHEDULED && group.getStatus() != RolloutGroupStatus.RUNNING) { // dynamic groups could already be running
                group.setStatus(RolloutGroupStatus.SCHEDULED);
                rolloutGroupRepository.save(group);
            }
            return true;
        }
    }

    private long createActionsForRolloutGroup(final Rollout rollout, final RolloutGroup group) {
        long totalActionsCreated = 0;
        try {
            long actionsCreated;
            do {
                actionsCreated = createActionsForTargetsInNewTransaction(rollout, group);
                totalActionsCreated += actionsCreated;
            } while (actionsCreated > 0);
        } catch (final TransactionException e) {
            log.warn(TRANSACTION_ASSIGNING_TARGETS_TO_ROLLOUT_GROUP_FAILED, e);
            return 0;
        }
        return totalActionsCreated;
    }

    private Long createActionsForTargetsInNewTransaction(final Rollout rollout, final RolloutGroup group) {
        // NB: new transaction in transaction
        return DeploymentHelper.runInNewTransaction("createActionsForTargets", status -> {
            final Slice<Target> targets = findByInRolloutGroupWithoutAction(
                    group.getId(), PageRequest.of(0, JpaRolloutExecutor.TRANSACTION_TARGETS));

            if (targets.getNumberOfElements() > 0) {
                final DistributionSet distributionSet = rollout.getDistributionSet();
                entityManager.detach(distributionSet); // LAZY_LOAD - if lazy loaded with different session
                final ActionType actionType = rollout.getActionType();
                final long forceTime = rollout.getForcedTime();
                createActions(targets.getContent(), distributionSet, actionType, forceTime, rollout, group);
            }

            return Long.valueOf(targets.getNumberOfElements());
        }, txManager);
    }

    /**
     * Creates an action entry into the action repository. In case of existing scheduled actions the scheduled actions gets canceled.
     * A scheduled action is created in-active for static and running for dynamic groups.
     */
    private List<Action> createActions(
            final Collection<Target> targets, final DistributionSet distributionSet, final ActionType actionType, final Long forcedTime,
            final Rollout rollout, final RolloutGroup rolloutGroup) {
        // cancel all current scheduled actions for this target. E.g. an action is already scheduled and a next action is created
        // then cancel the current scheduled action to cancel. E.g. a new scheduled action is created.
        final List<Long> targetIds = targets.stream().map(Target::getId).toList();
        deploymentManagement.cancelInactiveScheduledActionsForTargets(targetIds);
        // the action counts of all targets at once (grouped, per chunk), instead of one count per target
        final int quota = quotaManagement.getMaxActionsPerTarget();
        final Map<Long, Long> actionCounts = quota > 0 ? actionRepository.countByTargetIds(targetIds) : Map.of();
        return targets.stream()
                .map(target -> {
                    assertActionsPerTargetQuota(target, 1, quota, actionCounts.getOrDefault(target.getId(), 0L));

                    final JpaAction action = new JpaAction();
                    action.setTarget(target);
                    action.setActive(false);
                    action.setDistributionSet(distributionSet);
                    action.setActionType(actionType);
                    action.setForcedTime(forcedTime);
                    action.setStatus(Status.SCHEDULED);
                    action.setRollout(rollout);
                    action.setRolloutGroup(rolloutGroup);
                    action.setInitiatedBy(rollout.getCreatedBy());
                    rollout.getWeight().ifPresent(action::setWeight);
                    actionRepository.save(action);

                    return action;
                })
                .map(Action.class::cast)
                .toList();
    }

    /**
     * Enforces the quota defining the maximum number of {@link Action}s per {@link Target}.
     *
     * @param target the target
     * @param requested number of actions to check
     * @param quota the maximum number of actions per target
     * @param actionCount the current number of actions of the target
     */
    private void assertActionsPerTargetQuota(final Target target, final long requested, final int quota, final long actionCount) {
        try {
            QuotaHelper.assertAssignmentQuota(target.getId(), requested, quota, Action.class, Target.class, targetId -> actionCount);
        } catch (final AssignmentQuotaExceededException ex) {
            asSystem(() -> deploymentManagement.handleMaxAssignmentsExceeded(target.getId(), requested, ex));
        }
    }

    // target repository access

    // package-private just for testing
    Slice<Target> findByRsqlAndNoOverridingActionsAndNotInRolloutAndCompatibleAndUpdatable(
            final long rolloutId, final String rsql, final long distributionSetTypeId, final Pageable pageable) {
        return targetRepository
                .findAllWithoutCount(AccessController.Operation.UPDATE,
                        combineWithAnd(List.of(
                                QLSupport.getInstance().buildSpec(rsql, TargetFields.class),
                                TargetSpecifications.hasNoOverridingActionsAndNotInRollout(rolloutId),
                                TargetSpecifications.isCompatibleWithDistributionSetType(distributionSetTypeId))),
                        pageable)
                .map(Target.class::cast);
    }

    // Finds all targets for all the given parameter {@link TargetFilterQuery} and that are not assigned to one of the {@link RolloutGroup}s
    // and are compatible with the passed {@link DistributionSetType}
    private List<Long> findIdsByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(
            final Collection<Long> groups, final String rsql, final long dsTypeId, final long afterId, final int maxResults) {
        return findTargetIds(AccessController.Operation.UPDATE,
                combineWithAnd(List.of(
                        QLSupport.getInstance().buildSpec(rsql, TargetFields.class),
                        TargetSpecifications.isNotInRolloutGroups(groups),
                        TargetSpecifications.isCompatibleWithDistributionSetType(dsTypeId))),
                afterId, maxResults);
    }

    // Finds all targets with failed actions for specific Rollout and that are not assigned to one of the retried {@link RolloutGroup}s and
    //  are compatible with the passed {@link DistributionSetType}.
    private List<Long> findIdsByFailedRolloutAndNotInRolloutGroups(final String rolloutId, final Collection<Long> groups,
            final long afterId, final int maxResults) {
        return findTargetIds(AccessController.Operation.READ,
                combineWithAnd(List.of(
                        TargetSpecifications.failedActionsForRollout(rolloutId),
                        TargetSpecifications.isNotInRolloutGroups(groups))),
                afterId, maxResults);
    }

    // the ids of up to maxResults targets matching the specification with an id greater than afterId, in id order (keyset page),
    // with the access rules of the operation applied - as the target repository applies them, but without loading the targets
    private List<Long> findTargetIds(
            final AccessController.Operation operation, final Specification<JpaTarget> spec, final long afterId, final int maxResults) {
        final Specification<JpaTarget> withAccessRules = targetRepository.getAccessController()
                .map(accessController -> accessController.appendAccessRules(operation, spec))
                .orElse(spec);
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> query = cb.createQuery(Long.class);
        final Root<JpaTarget> root = query.from(JpaTarget.class);
        final Path<Long> id = root.get(AbstractJpaBaseEntity_.id);
        final Predicate specPredicate = withAccessRules.toPredicate(root, query, cb);
        final Predicate afterCursor = cb.greaterThan(id, afterId);
        query.select(id)
                .where(specPredicate == null ? afterCursor : cb.and(specPredicate, afterCursor))
                .orderBy(cb.asc(id));
        return entityManager.createQuery(query).setMaxResults(maxResults).getResultList();
    }

    // Finds all targets of the provided {@link RolloutGroup} that have no Action for the RolloutGroup
    private Slice<Target> findByInRolloutGroupWithoutAction(final long group, final Pageable pageable) {
        if (!rolloutGroupRepository.existsById(group)) {
            throw new EntityNotFoundException(RolloutGroup.class, group);
        }

        return JpaManagementHelper.findAllWithoutCountBySpec(
                targetRepository, List.of(TargetSpecifications.hasNoActionInRolloutGroup(group)), pageable);
    }

    private long countByActionsInRolloutGroup(final long rolloutGroupId) {
        return targetRepository.count(TargetSpecifications.isInActionRolloutGroup(rolloutGroupId));
    }

    // Counts all targets for all the given parameter {@link TargetFilterQuery} and that are not assigned to one of the {@link RolloutGroup}s
    // and are compatible with the passed {@link DistributionSetType}
    private long countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(
            final String rsql, final Collection<Long> groups, final long dsTypeId) {
        return targetRepository.count(AccessController.Operation.UPDATE,
                combineWithAnd(groups.isEmpty() // an empty IN isn't valid
                        // @formatter:off
                        ? List.of(
                                QLSupport.getInstance().buildSpec(rsql, TargetFields.class),
                                TargetSpecifications.isCompatibleWithDistributionSetType(dsTypeId))
                        : List.of(
                                QLSupport.getInstance().buildSpec(rsql, TargetFields.class),
                                TargetSpecifications.isNotInRolloutGroups(groups),
                                TargetSpecifications.isCompatibleWithDistributionSetType(dsTypeId))));
                         // @formatter:on
    }

    // Counts all targets with failed actions for specific Rollout and that are not assigned to one of the {@link RolloutGroup}s and are
    // compatible with the passed {@link DistributionSetType}
    private long countByFailedRolloutAndNotInRolloutGroups(final String rolloutId, final Collection<Long> groups) {
        final List<Specification<JpaTarget>> specList = groups.isEmpty() // an empty IN isn't valid
                ? List.of(TargetSpecifications.failedActionsForRollout(rolloutId))
                : List.of(TargetSpecifications.failedActionsForRollout(rolloutId), TargetSpecifications.isNotInRolloutGroups(groups));
        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }
}
