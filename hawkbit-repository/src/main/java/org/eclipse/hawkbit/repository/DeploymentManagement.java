/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.Constants;
import org.eclipse.hawkbit.eventbus.event.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.eventbus.event.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.eventbus.event.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ForceQuitActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.ActionWithStatusCount;
import org.eclipse.hawkbit.repository.model.Action_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSet_;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Rollout_;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.specifications.TargetSpecifications;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

/**
 * Business service facade for managing all deployment related data and actions.
 *
 *
 */
@Transactional(readOnly = true)
@Validated
@Service
public class DeploymentManagement {
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentManagement.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private DistributionSetRepository distributoinSetRepository;

    @Autowired
    private SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private ActionStatusRepository actionStatusRepository;

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private TargetInfoRepository targetInfoRepository;

    @Autowired
    private AuditorAware<String> auditorProvider;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    /**
     * method assigns the {@link DistributionSet} to all {@link Target}s.
     *
     * @param pset
     *            {@link DistributionSet} which is assigned to the
     *            {@link Target}s
     * @param targets
     *            the {@link Target}s which should obtain the
     *            {@link DistributionSet}
     *
     * @return the changed targets
     *
     * @throw IncompleteDistributionSetException if mandatory
     *        {@link SoftwareModuleType} are not assigned as define by the
     *        {@link DistributionSetType}. *
     */
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    @CacheEvict(value = { "distributionUsageAssigned" }, allEntries = true)
    public DistributionSetAssignmentResult assignDistributionSet(@NotNull final DistributionSet pset,
            @NotEmpty final List<Target> targets) {

        return assignDistributionSetByTargetId(pset,
                targets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()),
                ActionType.FORCED, Action.NO_FORCE_TIME);

    }

    /**
     * method assigns the {@link DistributionSet} to all {@link Target}s by
     * their IDs.
     *
     * @param dsID
     *            {@link DistributionSet} which is assigned to the
     *            {@link Target}s
     * @param targetIDs
     *            IDs of the {@link Target}s which should obtain the
     *            {@link DistributionSet}
     *
     * @return the changed targets
     *
     * @throws EntityNotFoundException
     *             if {@link DistributionSet} does not exist.
     *
     * @throw IncompleteDistributionSetException if mandatory
     *        {@link SoftwareModuleType} are not assigned as define by the
     *        {@link DistributionSetType}.
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    @CacheEvict(value = { "distributionUsageAssigned" }, allEntries = true)
    public DistributionSetAssignmentResult assignDistributionSet(@NotNull final Long dsID,
            @NotEmpty final String... targetIDs) {
        return assignDistributionSet(dsID, ActionType.FORCED, Action.NO_FORCE_TIME, targetIDs);
    }

    /**
     * method assigns the {@link DistributionSet} to all {@link Target}s by
     * their IDs with a specific {@link ActionType} and {@code forcetime}.
     *
     * @param dsID
     *            the ID of the distribution set to assign
     * @param actionType
     *            the type of the action to apply on the assignment
     * @param forcedTimestamp
     *            the time when the action should be forced, only necessary for
     *            {@link ActionType#TIMEFORCED}
     * @param targetIDs
     *            the IDs of the target to assign the distribution set
     * @return the assignment result
     *
     * @throw IncompleteDistributionSetException if mandatory
     *        {@link SoftwareModuleType} are not assigned as define by the
     *        {@link DistributionSetType}.
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    @CacheEvict(value = { "distributionUsageAssigned" }, allEntries = true)
    public DistributionSetAssignmentResult assignDistributionSet(@NotNull final Long dsID, final ActionType actionType,
            final long forcedTimestamp, @NotEmpty final String... targetIDs) {
        return assignDistributionSet(dsID, Arrays.stream(targetIDs)
                .map(t -> new TargetWithActionType(t, actionType, forcedTimestamp)).collect(Collectors.toList()));
    }

    /**
     * method assigns the {@link DistributionSet} to all {@link Target}s by
     * their IDs with a specific {@link ActionType} and {@code forcetime}.
     *
     * @param dsID
     *            the ID of the distribution set to assign
     * @param targets
     *            a list of all targets and their action type
     * @return the assignment result
     *
     * @throw IncompleteDistributionSetException if mandatory
     *        {@link SoftwareModuleType} are not assigned as define by the
     *        {@link DistributionSetType}.
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    @CacheEvict(value = { "distributionUsageAssigned" }, allEntries = true)
    public DistributionSetAssignmentResult assignDistributionSet(@NotNull final Long dsID,
            final List<TargetWithActionType> targets) {
        final DistributionSet set = distributoinSetRepository.findOne(dsID);
        if (set == null) {
            throw new EntityNotFoundException(
                    String.format("no %s with id %d found", DistributionSet.class.getSimpleName(), dsID));
        }

        return assignDistributionSetToTargets(set, targets, null, null);
    }

    /**
     * method assigns the {@link DistributionSet} to all {@link Target}s by
     * their IDs with a specific {@link ActionType} and {@code forcetime}.
     *
     * @param dsID
     *            the ID of the distribution set to assign
     * @param targets
     *            a list of all targets and their action type
     * @param rollout
     *            the rollout for this assignment
     * @param rolloutgroup
     *            the rolloutgroup for this assignment
     * @return the assignment result
     *
     * @throw IncompleteDistributionSetException if mandatory
     *        {@link SoftwareModuleType} are not assigned as define by the
     *        {@link DistributionSetType}.
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    @CacheEvict(value = { "distributionUsageAssigned" }, allEntries = true)
    public DistributionSetAssignmentResult assignDistributionSet(@NotNull final Long dsID,
            final List<TargetWithActionType> targets, final Rollout rollout, final RolloutGroup rolloutGroup) {
        final DistributionSet set = distributoinSetRepository.findOne(dsID);
        if (set == null) {
            throw new EntityNotFoundException(
                    String.format("no %s with id %d found", DistributionSet.class.getSimpleName(), dsID));
        }

        return assignDistributionSetToTargets(set, targets, rollout, rolloutGroup);
    }

    /**
     * method assigns the {@link DistributionSet} to all {@link Target}s by
     * their IDs with a specific {@link ActionType} and {@code forcetime}.
     *
     * @param dsID
     *            the ID of the distribution set to assign
     * @param targets
     *            a list of all targets and their action type
     * @param rollout
     *            the rollout for this assignment
     * @param rolloutgroup
     *            the rolloutgroup for this assignment
     * @return the assignment result
     *
     * @throw IncompleteDistributionSetException if mandatory
     *        {@link SoftwareModuleType} are not assigned as define by the
     *        {@link DistributionSetType}.
     */
    private DistributionSetAssignmentResult assignDistributionSetToTargets(@NotNull final DistributionSet set,
            final List<TargetWithActionType> targetsWithActionType, final Rollout rollout,
            final RolloutGroup rolloutGroup) {

        if (!set.isComplete()) {
            throw new IncompleteDistributionSetException(
                    "Distribution set of type " + set.getType().getKey() + " is incomplete: " + set.getId());
        }

        final List<String> controllerIDs = targetsWithActionType.stream().map(TargetWithActionType::getTargetId)
                .collect(Collectors.toList());

        LOG.debug("assignDistribution({}) to {} targets", set, controllerIDs.size());

        final Map<String, TargetWithActionType> targetsWithActionMap = targetsWithActionType.stream()
                .collect(Collectors.toMap(TargetWithActionType::getTargetId, Function.identity()));

        // split tIDs length into max entries in-statement because many database
        // have constraint of
        // max entries in in-statements e.g. Oracle with maximum 1000 elements,
        // so we need to split
        // the entries here and execute multiple statements
        // we take the target only into account if the requested operation is no
        // duplicate of a
        // previous one
        final List<Target> targets = Lists.partition(controllerIDs, Constants.MAX_ENTRIES_IN_STATEMENT).stream()
                .map(ids -> targetRepository
                        .findAll(TargetSpecifications.hasControllerIdAndAssignedDistributionSetIdNot(ids, set.getId())))
                .flatMap(t -> t.stream()).collect(Collectors.toList());

        if (targets.isEmpty()) {
            // detaching as it is not necessary to persist the set itself
            entityManager.detach(set);
            // return with nothing as all targets had the DS already assigned
            return new DistributionSetAssignmentResult(Collections.emptyList(), 0, targetsWithActionType.size(),
                    Collections.emptyList(), targetManagement);
        }

        final List<List<Long>> targetIds = Lists.partition(
                targets.stream().map(Target::getId).collect(Collectors.toList()), Constants.MAX_ENTRIES_IN_STATEMENT);

        // override all active actions and set them into canceling state, we
        // need to remember which
        // one we have been switched to canceling state because for targets
        // which we have changed to
        // canceling we don't want to publish the new action update event.
        final Set<Long> targetIdsCancellList = new HashSet<Long>();
        targetIds.forEach(ids -> targetIdsCancellList.addAll(overrideObsoleteUpdateActions(ids)));

        // cancel all scheduled actions which are in-active, these actions were
        // not active before and the manual assignment which has been done
        // cancels the
        targetIds.forEach(tIds -> actionRepository.switchStatus(Status.CANCELED, tIds, false, Status.SCHEDULED));

        // set assigned distribution set and TargetUpdateStatus
        final String currentUser;
        if (auditorProvider != null) {
            currentUser = auditorProvider.getCurrentAuditor();
        } else {
            currentUser = null;
        }

        targetIds.forEach(tIds -> targetRepository.setAssignedDistributionSet(set, System.currentTimeMillis(),
                currentUser, tIds));
        targetIds.forEach(tIds -> targetInfoRepository.setTargetUpdateStatus(TargetUpdateStatus.PENDING, tIds));

        final Map<String, Action> targetIdsToActions = actionRepository.save(targets.stream().map(t -> {
            final Action tAction = new Action();
            final TargetWithActionType targetWithActionType = targetsWithActionMap.get(t.getControllerId());
            tAction.setActionType(targetWithActionType.getActionType());
            tAction.setForcedTime(targetWithActionType.getForceTime());
            tAction.setActive(true);
            tAction.setStatus(Status.RUNNING);
            tAction.setTarget(t);
            tAction.setDistributionSet(set);
            tAction.setRollout(rollout);
            tAction.setRolloutGroup(rolloutGroup);
            return tAction;
        }).collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(a -> a.getTarget().getControllerId(), Function.identity()));

        // create initial action status when action is created so we
        // remember the initial
        // running status because we will change the status of the action itself
        // and with this action
        // status we have a nicer action history.
        targetIdsToActions.values().forEach(action -> {
            final ActionStatus actionStatus = new ActionStatus();
            actionStatus.setAction(action);
            actionStatus.setOccurredAt(action.getCreatedAt());
            actionStatus.setStatus(Status.RUNNING);
            actionStatusRepository.save(actionStatus);
        });

        // flush to get action IDs
        entityManager.flush();
        // collect updated target and actions IDs in order to return them
        final DistributionSetAssignmentResult result = new DistributionSetAssignmentResult(
                targets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()), targets.size(),
                controllerIDs.size() - targets.size(),
                targetIdsToActions.values().stream().map(Action::getId).collect(Collectors.toList()), targetManagement);

        LOG.debug("assignDistribution({}) finished {}", set, result);

        final List<SoftwareModule> softwareModules = softwareModuleRepository.findByAssignedTo(set);

        // detaching as it is not necessary to persist the set itself
        entityManager.detach(set);

        sendDistributionSetAssignmentEvent(targets, targetIdsCancellList, targetIdsToActions, softwareModules);

        return result;
    }

    private void sendDistributionSetAssignmentEvent(final List<Target> targets, final Set<Long> targetIdsCancellList,
            final Map<String, Action> targetIdsToActions, final List<SoftwareModule> softwareModules) {
        targets.stream().filter(t -> !!!targetIdsCancellList.contains(t.getId()))
                .forEach(t -> assignDistributionSetEvent(t, targetIdsToActions.get(t.getControllerId()).getId(),
                        softwareModules));
    }

    /**
     * Sends the {@link TargetAssignDistributionSetEvent} for a specific target
     * to the {@link EventBus}.
     *
     * @param target
     *            the Target which has been assigned to a distribution set
     * @param actionId
     *            the action id of the assignment
     * @param softwareModules
     *            the software modules which have been assigned
     */
    private void assignDistributionSetEvent(final Target target, final Long actionId,
            final List<SoftwareModule> softwareModules) {
        target.getTargetInfo().setUpdateStatus(TargetUpdateStatus.PENDING);
        afterCommit.afterCommit(() -> {
            eventBus.post(new TargetInfoUpdateEvent(target.getTargetInfo()));
            eventBus.post(new TargetAssignDistributionSetEvent(target.getOptLockRevision(), target.getTenant(),
                    target.getControllerId(), actionId, softwareModules, target.getTargetInfo().getAddress()));
        });
    }

    /**
     * Removes {@link UpdateAction}s that are no longer necessary and sends
     * cancelations to the controller.
     *
     * @param myTarget
     *            to override {@link UpdateAction}s
     */
    private Set<Long> overrideObsoleteUpdateActions(final List<Long> targetsIds) {

        final Set<Long> cancelledTargetIds = new HashSet<Long>();

        // Figure out if there are potential target/action combinations that
        // need to be considered
        // for cancelation
        final List<Action> activeActions = actionRepository
                .findByActiveAndTargetIdInAndActionStatusNotEqualToAndDistributionSetRequiredMigrationStep(targetsIds,
                        Action.Status.CANCELING);
        activeActions.forEach(action -> {
            action.setStatus(Status.CANCELING);
            // document that the status has been retrieved

            actionStatusRepository.save(new ActionStatus(action, Status.CANCELING, System.currentTimeMillis(),
                    "manual cancelation requested"));

            cancelAssignDistributionSetEvent(action.getTarget(), action.getId());

            cancelledTargetIds.add(action.getTarget().getId());
        });

        actionRepository.save(activeActions);

        return cancelledTargetIds;

    }

    private DistributionSetAssignmentResult assignDistributionSetByTargetId(@NotNull final DistributionSet set,
            @NotEmpty final List<String> tIDs, final ActionType actionType, final long forcedTime) {

        return assignDistributionSetToTargets(set, tIDs.stream()
                .map(t -> new TargetWithActionType(t, actionType, forcedTime)).collect(Collectors.toList()), null,
                null);

    }

    /**
     * Internal helper method used only inside service level. As a result is no
     * additional security necessary.
     *
     * @param target
     *            to update
     * @param status
     *            of the target
     * @param setInstalledDate
     *            to set
     *
     * @return updated target
     */
    Target updateTargetInfo(@NotNull final Target target, @NotNull final TargetUpdateStatus status,
            final boolean setInstalledDate) {
        final TargetInfo ts = target.getTargetInfo();
        ts.setUpdateStatus(status);

        if (setInstalledDate) {
            ts.setInstallationDate(System.currentTimeMillis());
        }
        targetInfoRepository.save(ts);
        return entityManager.merge(target);
    }

    /**
     * Cancels given {@link Action} for given {@link Target}. The method will
     * immediately add a {@link ActionStatus.Status#CANCELED} status to the
     * action. However, it might be possible that the controller will continue
     * to work on the cancellation.
     *
     * @param action
     *            to be canceled
     * @param target
     *            for which the action needs cancellation
     *
     * @return generated {@link CancelAction} or <code>null</code> if not in
     *         {@link Target#getActiveActions()}.
     * @throws CancelActionNotAllowedException
     *             in case the given action is not active or is already a cancel
     *             action
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public Action cancelAction(@NotNull final Action action, @NotNull final Target target) {
        LOG.debug("cancelAction({}, {})", action, target);
        if (action.isCancelingOrCanceled()) {
            throw new CancelActionNotAllowedException("Actions in canceling or canceled state cannot be canceled");
        }
        final Action myAction = entityManager.merge(action);

        if (myAction.isActive()) {
            LOG.debug("action ({}) was still active. Change to {}.", action, Status.CANCELING);
            myAction.setStatus(Status.CANCELING);

            // document that the status has been retrieved
            actionStatusRepository.save(new ActionStatus(myAction, Status.CANCELING, System.currentTimeMillis(),
                    "manual cancelation requested"));
            final Action saveAction = actionRepository.save(myAction);
            cancelAssignDistributionSetEvent(target, myAction.getId());

            return saveAction;
        } else {
            throw new CancelActionNotAllowedException(
                    "Action [id: " + action.getId() + "] is not active and cannot be canceled");
        }
    }

    /**
     * Sends the {@link CancelTargetAssignmentEvent} for a specific target to
     * the {@link EventBus}.
     *
     * @param target
     *            the Target which has been assigned to a distribution set
     * @param actionId
     *            the action id of the assignment
     */
    private void cancelAssignDistributionSetEvent(final Target target, final Long actionId) {
        afterCommit.afterCommit(() -> eventBus.post(new CancelTargetAssignmentEvent(target.getOptLockRevision(),
                target.getTenant(), target.getControllerId(), actionId, target.getTargetInfo().getAddress())));
    }

    /**
     * Force cancels given {@link Action} for given {@link Target}. Force
     * canceling means that the action is marked as canceled on the SP server
     * and a cancel request is sent to the target. But however it's not tracked,
     * if the targets handles the cancel request or not.
     *
     * @param action
     *            to be canceled
     * @param target
     *            for which the action needs cancellation
     *
     * @return generated {@link CancelAction} or <code>null</code> if not in
     *         {@link Target#getActiveActions()}.
     * @throws CancelActionNotAllowedException
     *             in case the given action is not active
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public Action forceQuitAction(@NotNull final Action action, @NotNull final Target target) {
        final Action mergedAction = entityManager.merge(action);

        if (!mergedAction.isCancelingOrCanceled()) {
            throw new ForceQuitActionNotAllowedException(
                    "Action [id: " + action.getId() + "] is not canceled yet and cannot be force quit");
        }

        if (!mergedAction.isActive()) {
            throw new ForceQuitActionNotAllowedException(
                    "Action [id: " + action.getId() + "] is not active and cannot be force quit");
        }

        LOG.warn("action ({}) was still activ and has been force quite.", action);

        // document that the status has been retrieved
        actionStatusRepository.save(new ActionStatus(mergedAction, Status.CANCELED, System.currentTimeMillis(),
                "A force quit has been performed."));

        successCancellation(mergedAction);

        return actionRepository.save(mergedAction);
    }

    /**
     * Creates an action entry into the action repository. In case of existing
     * scheduled actions the scheduled actions gets canceled. A scheduled action
     * is created in-active.
     *
     * @param targets
     *            the targets to create scheduled actions for
     * @param distributionSet
     *            the distribution set for the actions
     * @param actionType
     *            the action type for the action
     * @param forcedTime
     *            the forcedTime of the action
     * @param rollout
     *            the rollout for this action
     * @param rolloutGroup
     *            the rolloutgroup for this action
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public void createScheduledAction(final List<Target> targets, final DistributionSet distributionSet,
            final ActionType actionType, final long forcedTime, final Rollout rollout,
            final RolloutGroup rolloutGroup) {
        // cancel all current scheduled actions for this target. E.g. an action
        // is already scheduled and a next action is created then cancel the
        // current scheduled action to cancel. E.g. a new scheduled action is
        // created.
        final List<Long> targetIds = targets.stream().map(t -> t.getId()).collect(Collectors.toList());
        actionRepository.switchStatus(Action.Status.CANCELED, targetIds, false, Action.Status.SCHEDULED);
        targets.forEach(target -> {
            final Action action = new Action();
            action.setTarget(target);
            action.setActive(false);
            action.setDistributionSet(distributionSet);
            action.setActionType(actionType);
            action.setForcedTime(forcedTime);
            action.setStatus(Status.SCHEDULED);
            action.setRollout(rollout);
            action.setRolloutGroup(rolloutGroup);
            actionRepository.save(action);
        });
    }

    /**
     * Starting an action which is scheduled, e.g. in case of rollout a
     * scheduled action must be started now.
     *
     * @param action
     *            the action to start now.
     * @return the action which has been started
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    public Action startScheduledAction(@NotNull final Action action) {

        final Action mergedAction = entityManager.merge(action);
        final Target mergedTarget = entityManager.merge(action.getTarget());

        // check if we need to override running update actions
        final Set<Long> overrideObsoleteUpdateActions = overrideObsoleteUpdateActions(
                Collections.singletonList(action.getTarget().getId()));

        final boolean hasDistributionSetAlreadyAssigned = targetRepository
                .count(TargetSpecifications.hasControllerIdAndAssignedDistributionSetIdNot(
                        Collections.singletonList(mergedTarget.getControllerId()),
                        action.getDistributionSet().getId())) == 0;
        if (hasDistributionSetAlreadyAssigned) {
            // the target has already the distribution set assigned, we don't
            // need to start the scheduled action, just finished it.
            mergedAction.setStatus(Status.FINISHED);
            mergedAction.setActive(false);
            return actionRepository.save(mergedAction);
        }

        mergedAction.setActive(true);
        mergedAction.setStatus(Status.RUNNING);
        final Action savedAction = actionRepository.save(mergedAction);

        final ActionStatus actionStatus = new ActionStatus();
        actionStatus.setAction(action);
        actionStatus.setOccurredAt(action.getCreatedAt());
        actionStatus.setStatus(Status.RUNNING);
        actionStatusRepository.save(actionStatus);

        mergedTarget.setAssignedDistributionSet(action.getDistributionSet());
        final TargetInfo targetInfo = mergedTarget.getTargetInfo();
        targetInfo.setUpdateStatus(TargetUpdateStatus.PENDING);
        targetRepository.save(mergedTarget);
        targetInfoRepository.save(targetInfo);

        // in case we canceled an action before for this target, then don't fire
        // assignment event
        if (!overrideObsoleteUpdateActions.contains(savedAction.getId())) {
            final List<SoftwareModule> softwareModules = softwareModuleRepository
                    .findByAssignedTo(action.getDistributionSet());
            // send distribution set assignment event

            assignDistributionSetEvent(mergedAction.getTarget(), mergedAction.getId(), softwareModules);
        }
        return savedAction;
    }

    /**
     * Get the {@link Action} entity for given actionId.
     *
     * @param actionId
     *            to be id of the action
     * @return the corresponding {@link Action}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Action findAction(@NotNull final Long actionId) {
        return actionRepository.findOne(actionId);
    }

    /**
     * Get the {@link Action} entity for given actionId with all lazy attributes
     * (i.e. distributionSet, target, target.assignedDs).
     *
     * @param actionId
     *            to be id of the action
     * @return the corresponding {@link Action}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Action findActionWithDetails(@NotNull final Long actionId) {
        return actionRepository.findById(actionId);
    }

    /**
     * Retrieves all {@link Action}s of a specific target.
     *
     * @param pageable
     *            pagination parameter
     * @param target
     *            of which the actions have to be searched
     * @return a paged list of actions associated with the given target
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Slice<Action> findActionsByTarget(final Pageable pageable, final Target target) {
        return actionRepository.findByTarget(pageable, target);
    }

    /**
     * Retrieves all {@link Action}s of a specific target ordered by action ID.
     *
     * @param target
     *            the target associated with the actions
     * @return a list of actions associated with the given target ordered by
     *         action ID
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<Action> findActionsByTarget(final Target target) {
        return actionRepository.findByTarget(target);
    }

    /**
     * Retrieves all {@link Action}s of a specific target ordered by action ID.
     *
     * @param target
     *            the target associated with the actions
     * @return a list of actions associated with the given target ordered by
     *         action ID
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<ActionWithStatusCount> findActionsWithStatusCountByTargetOrderByIdDesc(final Target target) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ActionWithStatusCount> query = cb.createQuery(ActionWithStatusCount.class);
        final Root<Action> actionRoot = query.from(Action.class);
        final ListJoin<Action, ActionStatus> actionStatusJoin = actionRoot.join(Action_.actionStatus, JoinType.LEFT);
        final Join<Action, DistributionSet> actionDsJoin = actionRoot.join(Action_.distributionSet);
        final Join<Action, Rollout> actionRolloutJoin = actionRoot.join(Action_.rollout, JoinType.LEFT);

        final CriteriaQuery<ActionWithStatusCount> multiselect = query.distinct(true).multiselect(
                actionRoot.get(Action_.id), actionRoot.get(Action_.actionType), actionRoot.get(Action_.active),
                actionRoot.get(Action_.forcedTime), actionRoot.get(Action_.status), actionRoot.get(Action_.createdAt),
                actionRoot.get(Action_.lastModifiedAt), actionDsJoin.get(DistributionSet_.id),
                actionDsJoin.get(DistributionSet_.name), actionDsJoin.get(DistributionSet_.version),
                cb.count(actionStatusJoin), actionRolloutJoin.get(Rollout_.name));
        multiselect.where(cb.equal(actionRoot.get(Action_.target), target));
        multiselect.orderBy(cb.desc(actionRoot.get(Action_.id)));
        multiselect.groupBy(actionRoot.get(Action_.id));
        final List<ActionWithStatusCount> resultList = entityManager.createQuery(multiselect).getResultList();
        return resultList;
    }

    /**
     * Retrieves all {@link Action}s assigned to a specific {@link Target} and a
     * given specification.
     *
     * @param specifiction
     *            the specification to narrow down the search
     * @param target
     *            the target which must be assigned to the actions
     * @param pageable
     *            the page request
     * @return a slice of actions assigned to the specific target and the
     *         specification
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Slice<Action> findActionsByTarget(final Specification<Action> specifiction, final Target target,
            final Pageable pageable) {

        return actionRepository.findAll((Specification<Action>) (root, query, cb) -> cb
                .and(specifiction.toPredicate(root, query, cb), cb.equal(root.get(Action_.target), target)), pageable);
    }

    /**
     * Retrieves all {@link Action}s which are referring the given
     * {@link Target}.
     *
     * @param foundTarget
     *            the target to find actions for
     * @param pageable
     *            the pageable request to limit, sort the actions
     * @return a slice of actions found for a specific target
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Slice<Action> findActionsByTarget(final Target foundTarget, final Pageable pageable) {
        return actionRepository.findByTarget(pageable, foundTarget);
    }

    /**
     * Retrieves all active {@link Action}s of a specific target ordered by
     * action ID.
     *
     * @param pageable
     *            the pagination parameter
     * @param target
     *            the target associated with the actions
     * @return a paged list of actions associated with the given target
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Page<Action> findActiveActionsByTarget(final Pageable pageable, final Target target) {
        return actionRepository.findByActiveAndTarget(pageable, target, true);
    }

    /**
     * Retrieves all active {@link Action}s of a specific target ordered by
     * action ID.
     *
     * @param target
     *            the target associated with the actions
     * @return a list of actions associated with the given target
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<Action> findActiveActionsByTarget(final Target target) {
        return actionRepository.findByActiveAndTarget(target, true);
    }

    /**
     * Retrieves all inactive {@link Action}s of a specific target ordered by
     * action ID.
     *
     * @param target
     *            the target associated with the actions
     * @return a list of actions associated with the given target
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<Action> findInActiveActionsByTarget(final Target target) {
        return actionRepository.findByActiveAndTarget(target, false);
    }

    /**
     * Retrieves all inactive {@link Action}s of a specific target ordered by
     * action ID.
     *
     * @param pageable
     *            the pagination parameter
     * @param target
     *            the target associated with the actions
     * @return a paged list of actions associated with the given target
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Page<Action> findInActiveActionsByTarget(final Pageable pageable, final Target target) {
        return actionRepository.findByActiveAndTarget(pageable, target, false);
    }

    /**
     * counts all actions associated to a specific target.
     *
     * @param target
     *            the target associated to the actions to count
     * @return the count value of found actions associated to the target
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Long countActionsByTarget(@NotNull final Target target) {
        return actionRepository.countByTarget(target);
    }

    /**
     * counts all actions associated to a specific target.
     *
     * @param spec
     *            the specification to filter the count result
     * @param target
     *            the target associated to the actions to count
     * @return the count value of found actions associated to the target
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Long countActionsByTarget(@NotNull final Specification<Action> spec, @NotNull final Target target) {
        return actionRepository.count((root, query, cb) -> cb.and(spec.toPredicate(root, query, cb),
                cb.equal(root.get(Action_.target), target)));
    }

    /**
     * Updates a {@link TargetAction} and forces the {@link TargetAction} if
     * it's not already forced.
     *
     * @param targetId
     *            the ID of the target
     * @param actionId
     *            the ID of the action
     * @return the updated or the found {@link TargetAction}
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public Action forceTargetAction(final Long actionId) {
        final Action action = actionRepository.findOne(actionId);
        if (action != null && !action.isForced()) {
            action.setActionType(ActionType.FORCED);
            return actionRepository.save(action);
        }
        return action;
    }

    /**
     * retrieves all the {@link ActionStatus} entries of the given
     * {@link Action} and {@link Target} in the order latest first.
     *
     * @param pageReq
     *            pagination parameter
     * @param action
     *            to be filtered on
     * @param withMessages
     *            to <code>true</code> if {@link ActionStatus#getMessages()}
     *            need to be fetched.
     * @return the corresponding {@link Page} of {@link ActionStatus}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Page<ActionStatus> findActionStatusMessagesByActionInDescOrder(final Pageable pageReq, final Action action,
            final boolean withMessages) {
        if (withMessages) {
            return actionStatusRepository.getByActionOrderByIdDesc(pageReq, action);
        } else {
            return actionStatusRepository.findByActionOrderByIdDesc(pageReq, action);
        }
    }

    /**
     * This method is called, when cancellation has been successful. It sets the
     * action to canceled, resets the meta data of the target and in case there
     * is a new action this action is triggered.
     *
     * @param action
     *            the action which is set to canceled
     */
    void successCancellation(final Action action) {

        // set action inactive
        action.setActive(false);
        action.setStatus(Status.CANCELED);

        final Target target = action.getTarget();
        final List<Action> nextActiveActions = actionRepository.findByTargetAndActiveOrderByIdAsc(target, true).stream()
                .filter(a -> !a.getId().equals(action.getId())).collect(Collectors.toList());

        if (nextActiveActions.isEmpty()) {
            target.setAssignedDistributionSet(target.getTargetInfo().getInstalledDistributionSet());
            updateTargetInfo(target, TargetUpdateStatus.IN_SYNC, false);
        } else {
            target.setAssignedDistributionSet(nextActiveActions.get(0).getDistributionSet());
        }
        targetManagement.updateTarget(target);
    }

    /**
     * Retrieving all actions referring to a given rollout with a specific
     * action as parent reference and a specific status.
     *
     * Finding all actions of a specific rolloutgroup parent relation.
     *
     * @param rollout
     *            the rollout the actions belong to
     * @param rolloutGroupParent
     *            the parent rolloutgroup the actions should reference
     * @param actionStatus
     *            the status the actions have
     * @return the actions referring a specific rollout and a specific parent
     *         rolloutgroup in a specific status
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    public List<Action> findActionsByRolloutGroupParentAndStatus(final Rollout rollout,
            final RolloutGroup rolloutGroupParent, final Action.Status actionStatus) {
        return actionRepository.findByRolloutAndRolloutGroupParentAndStatus(rollout, rolloutGroupParent, actionStatus);
    }

    /**
     * Retrieves all actions for a specific rollout and in a specific status.
     *
     * @param rollout
     *            the rollout the actions beglong to
     * @param actionStatus
     *            the status of the actions
     * @return the actions referring a specific rollout an in a specific status
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<Action> findActionsByRolloutAndStatus(final Rollout rollout, final Action.Status actionStatus) {
        return actionRepository.findByRolloutAndStatus(rollout, actionStatus);
    }
}
