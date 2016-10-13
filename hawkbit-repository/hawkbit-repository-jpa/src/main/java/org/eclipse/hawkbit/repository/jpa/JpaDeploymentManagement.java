/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import org.eclipse.hawkbit.eventbus.event.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.eventbus.event.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ForceQuitActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionWithStatusCount;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.ActionWithStatusCount;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

/**
 * JPA implementation for {@link DeploymentManagement}.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
public class JpaDeploymentManagement implements DeploymentManagement {
    private static final Logger LOG = LoggerFactory.getLogger(JpaDeploymentManagement.class);

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

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Autowired
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Modifying
    @CacheEvict(value = { "distributionUsageAssigned" }, allEntries = true)
    public DistributionSetAssignmentResult assignDistributionSet(final DistributionSet pset,
            final List<Target> targets) {

        return assignDistributionSetByTargetId((JpaDistributionSet) pset,
                targets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()),
                ActionType.FORCED, org.eclipse.hawkbit.repository.model.RepositoryModelConstants.NO_FORCE_TIME);

    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = { "distributionUsageAssigned" }, allEntries = true)
    public DistributionSetAssignmentResult assignDistributionSet(final Long dsID, final String... targetIDs) {
        return assignDistributionSet(dsID, ActionType.FORCED,
                org.eclipse.hawkbit.repository.model.RepositoryModelConstants.NO_FORCE_TIME, targetIDs);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @CacheEvict(value = { "distributionUsageAssigned" }, allEntries = true)
    // Exception squid:S2095: see
    // https://jira.sonarsource.com/browse/SONARJAVA-1478
    @SuppressWarnings({ "squid:S2095" })
    public DistributionSetAssignmentResult assignDistributionSet(final Long dsID, final ActionType actionType,
            final long forcedTimestamp, final String... targetIDs) {
        return assignDistributionSet(dsID, Arrays.stream(targetIDs)
                .map(t -> new TargetWithActionType(t, actionType, forcedTimestamp)).collect(Collectors.toList()));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = { "distributionUsageAssigned" }, allEntries = true)
    public DistributionSetAssignmentResult assignDistributionSet(final Long dsID,
            final Collection<TargetWithActionType> targets) {
        return assignDistributionSet(dsID, targets, null);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = { "distributionUsageAssigned" }, allEntries = true)
    public DistributionSetAssignmentResult assignDistributionSet(final Long dsID,
                                                                 final Collection<TargetWithActionType> targets,
                                                                 final String actionMessage) {
        final JpaDistributionSet set = distributoinSetRepository.findOne(dsID);
        if (set == null) {
            throw new EntityNotFoundException(
                    String.format("no %s with id %d found", DistributionSet.class.getSimpleName(), dsID));
        }

        return assignDistributionSetToTargets(set, targets, null, null, actionMessage);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = { "distributionUsageAssigned" }, allEntries = true)
    public DistributionSetAssignmentResult assignDistributionSet(final Long dsID,
            final Collection<TargetWithActionType> targets, final Rollout rollout, final RolloutGroup rolloutGroup) {
        final JpaDistributionSet set = distributoinSetRepository.findOne(dsID);
        if (set == null) {
            throw new EntityNotFoundException(
                    String.format("no %s with id %d found", DistributionSet.class.getSimpleName(), dsID));
        }

        return assignDistributionSetToTargets(set, targets, (JpaRollout) rollout, (JpaRolloutGroup) rolloutGroup, null);
    }

    /**
     * method assigns the {@link DistributionSet} to all {@link Target}s by
     * their IDs with a specific {@link ActionType} and {@code forcetime}.
     *
     * @param set
     *            the ID of the distribution set to assign
     * @param targetsWithActionType
     *            a list of all targets and their action type
     * @param rollout
     *            the rollout for this assignment
     * @param rolloutGroup
     *            the rollout group for this assignment
     * @param actionMessage
     *            an optional message to be written into the action status
     * @return the assignment result
     *
     * @throw IncompleteDistributionSetException if mandatory
     *        {@link SoftwareModuleType} are not assigned as define by the
     *        {@link DistributionSetType}.
     */
    private DistributionSetAssignmentResult assignDistributionSetToTargets(@NotNull final JpaDistributionSet set,
            final Collection<TargetWithActionType> targetsWithActionType, final JpaRollout rollout,
            final JpaRolloutGroup rolloutGroup, final String actionMessage) {

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
        // have constraint of max entries in in-statements e.g. Oracle with
        // maximum 1000 elements, so we need to split the entries here and
        // execute multiple statements we take the target only into account if
        // the requested operation is no duplicate of a previous one
        final List<JpaTarget> targets = Lists.partition(controllerIDs, Constants.MAX_ENTRIES_IN_STATEMENT).stream()
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
        // need to remember which one we have been switched to canceling state
        // because for targets which we have changed to canceling we don't want
        // to publish the new action update event.
        final Set<Long> targetIdsCancellList = new HashSet<>();
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
        final Map<String, JpaAction> targetIdsToActions = actionRepository
                .save(targets.stream().map(t -> createTargetAction(targetsWithActionMap, t, set, rollout, rolloutGroup))
                        .collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(a -> a.getTarget().getControllerId(), Function.identity()));

        // create initial action status when action is created so we remember
        // the initial running status because we will change the status
        // of the action itself and with this action status we have a nicer
        // action history.
        targetIdsToActions.values().forEach(action -> {
            final JpaActionStatus actionStatus = new JpaActionStatus();
            actionStatus.setAction(action);
            actionStatus.setOccurredAt(action.getCreatedAt());
            actionStatus.setStatus(Status.RUNNING);
            if(actionMessage != null) {
                actionStatus.addMessage(actionMessage);
            }
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

        final List<JpaSoftwareModule> softwareModules = softwareModuleRepository.findByAssignedTo(set);

        // detaching as it is not necessary to persist the set itself
        entityManager.detach(set);

        sendDistributionSetAssignmentEvent(targets, targetIdsCancellList, targetIdsToActions, softwareModules);

        return result;
    }

    private void sendDistributionSetAssignmentEvent(final List<JpaTarget> targets, final Set<Long> targetIdsCancellList,
            final Map<String, JpaAction> targetIdsToActions, final List<JpaSoftwareModule> softwareModules) {
        targets.stream().filter(t -> !!!targetIdsCancellList.contains(t.getId()))
                .forEach(t -> assignDistributionSetEvent(t, targetIdsToActions.get(t.getControllerId()).getId(),
                        softwareModules));
    }

    private static JpaAction createTargetAction(final Map<String, TargetWithActionType> targetsWithActionMap,
            final JpaTarget target, final JpaDistributionSet set, final JpaRollout rollout,
            final JpaRolloutGroup rolloutGroup) {
        final JpaAction actionForTarget = new JpaAction();
        final TargetWithActionType targetWithActionType = targetsWithActionMap.get(target.getControllerId());
        actionForTarget.setActionType(targetWithActionType.getActionType());
        actionForTarget.setForcedTime(targetWithActionType.getForceTime());
        actionForTarget.setActive(true);
        actionForTarget.setStatus(Status.RUNNING);
        actionForTarget.setTarget(target);
        actionForTarget.setDistributionSet(set);
        actionForTarget.setRollout(rollout);
        actionForTarget.setRolloutGroup(rolloutGroup);
        return actionForTarget;
    }

    /**
     * Sends the {@link TargetAssignDistributionSetEvent} for a specific target
     * to the {@link EventBus}.
     *
     * @param target
     *            the Target which has been assigned to a distribution set
     * @param actionId
     *            the action id of the assignment
     * @param modules
     *            the software modules which have been assigned
     */
    private void assignDistributionSetEvent(final JpaTarget target, final Long actionId,
            final List<JpaSoftwareModule> modules) {
        ((JpaTargetInfo) target.getTargetInfo()).setUpdateStatus(TargetUpdateStatus.PENDING);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Collection<SoftwareModule> softwareModules = (Collection) modules;
        afterCommit.afterCommit(() -> {
            eventBus.post(new TargetInfoUpdateEvent(target.getTargetInfo()));
            eventBus.post(new TargetAssignDistributionSetEvent(target.getOptLockRevision(), target.getTenant(), target,
                    actionId, softwareModules));
        });
    }

    /**
     * Removes {@link Action}s that are no longer necessary and sends
     * cancellations to the controller.
     *
     * @param targetsIds
     *            to override {@link Action}s
     */
    private Set<Long> overrideObsoleteUpdateActions(final List<Long> targetsIds) {

        final Set<Long> cancelledTargetIds = new HashSet<>();

        // Figure out if there are potential target/action combinations that
        // need to be considered
        // for cancelation
        final List<JpaAction> activeActions = actionRepository
                .findByActiveAndTargetIdInAndActionStatusNotEqualToAndDistributionSetRequiredMigrationStep(targetsIds,
                        Action.Status.CANCELING);
        activeActions.forEach(action -> {
            action.setStatus(Status.CANCELING);
            // document that the status has been retrieved

            actionStatusRepository.save(new JpaActionStatus(action, Status.CANCELING, System.currentTimeMillis(),
                    "manual cancelation requested"));

            cancelAssignDistributionSetEvent(action.getTarget(), action.getId());

            cancelledTargetIds.add(action.getTarget().getId());
        });

        actionRepository.save(activeActions);

        return cancelledTargetIds;

    }

    private DistributionSetAssignmentResult assignDistributionSetByTargetId(@NotNull final JpaDistributionSet set,
            @NotEmpty final List<String> tIDs, final ActionType actionType, final long forcedTime) {

        return assignDistributionSetToTargets(set, tIDs.stream()
                .map(t -> new TargetWithActionType(t, actionType, forcedTime)).collect(Collectors.toList()), null,
                null, null);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Action cancelAction(final Action action, final Target target) {
        LOG.debug("cancelAction({}, {})", action, target);
        if (action.isCancelingOrCanceled()) {
            throw new CancelActionNotAllowedException("Actions in canceling or canceled state cannot be canceled");
        }
        final JpaAction myAction = (JpaAction) entityManager.merge(action);

        if (myAction.isActive()) {
            LOG.debug("action ({}) was still active. Change to {}.", action, Status.CANCELING);
            myAction.setStatus(Status.CANCELING);

            // document that the status has been retrieved
            actionStatusRepository.save(new JpaActionStatus(myAction, Status.CANCELING, System.currentTimeMillis(),
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

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Action forceQuitAction(final Action action) {
        final JpaAction mergedAction = (JpaAction) entityManager.merge(action);

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
        actionStatusRepository.save(new JpaActionStatus(mergedAction, Status.CANCELED, System.currentTimeMillis(),
                "A force quit has been performed."));

        DeploymentHelper.successCancellation(mergedAction, actionRepository, targetManagement, targetInfoRepository,
                entityManager);

        return actionRepository.save(mergedAction);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createScheduledAction(final Collection<Target> targets, final DistributionSet distributionSet,
            final ActionType actionType, final Long forcedTime, final Rollout rollout,
            final RolloutGroup rolloutGroup) {
        // cancel all current scheduled actions for this target. E.g. an action
        // is already scheduled and a next action is created then cancel the
        // current scheduled action to cancel. E.g. a new scheduled action is
        // created.
        final List<Long> targetIds = targets.stream().map(t -> t.getId()).collect(Collectors.toList());
        actionRepository.switchStatus(Action.Status.CANCELED, targetIds, false, Action.Status.SCHEDULED);
        targets.forEach(target -> {
            final JpaAction action = new JpaAction();
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

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Action startScheduledAction(final Action action) {

        final JpaAction mergedAction = (JpaAction) entityManager.merge(action);
        final JpaTarget mergedTarget = (JpaTarget) entityManager.merge(action.getTarget());

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

        final JpaActionStatus actionStatus = new JpaActionStatus();
        actionStatus.setAction(action);
        actionStatus.setOccurredAt(action.getCreatedAt());
        actionStatus.setStatus(Status.RUNNING);
        actionStatusRepository.save(actionStatus);

        mergedTarget.setAssignedDistributionSet(action.getDistributionSet());
        final JpaTargetInfo targetInfo = (JpaTargetInfo) mergedTarget.getTargetInfo();
        targetInfo.setUpdateStatus(TargetUpdateStatus.PENDING);
        targetRepository.save(mergedTarget);
        targetInfoRepository.save(targetInfo);

        // in case we canceled an action before for this target, then don't fire
        // assignment event
        if (!overrideObsoleteUpdateActions.contains(savedAction.getId())) {
            final List<JpaSoftwareModule> softwareModules = softwareModuleRepository
                    .findByAssignedTo((JpaDistributionSet) action.getDistributionSet());
            // send distribution set assignment event

            assignDistributionSetEvent((JpaTarget) mergedAction.getTarget(), mergedAction.getId(), softwareModules);
        }
        return savedAction;
    }

    @Override
    public Action findAction(final Long actionId) {
        return actionRepository.findOne(actionId);
    }

    @Override
    public Action findActionWithDetails(final Long actionId) {
        return actionRepository.findById(actionId);
    }

    @Override
    public Slice<Action> findActionsByTarget(final Pageable pageable, final Target target) {
        return actionRepository.findByTarget(pageable, (JpaTarget) target);
    }

    @Override
    public List<Action> findActionsByTarget(final Target target) {
        return actionRepository.findByTarget((JpaTarget) target);
    }

    @Override
    public List<ActionWithStatusCount> findActionsWithStatusCountByTargetOrderByIdDesc(final Target target) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<JpaActionWithStatusCount> query = cb.createQuery(JpaActionWithStatusCount.class);
        final Root<JpaAction> actionRoot = query.from(JpaAction.class);
        final ListJoin<JpaAction, JpaActionStatus> actionStatusJoin = actionRoot.join(JpaAction_.actionStatus,
                JoinType.LEFT);
        final Join<JpaAction, JpaDistributionSet> actionDsJoin = actionRoot.join(JpaAction_.distributionSet);
        final Join<JpaAction, JpaRollout> actionRolloutJoin = actionRoot.join(JpaAction_.rollout, JoinType.LEFT);

        final CriteriaQuery<JpaActionWithStatusCount> multiselect = query.distinct(true).multiselect(
                actionRoot.get(JpaAction_.id), actionRoot.get(JpaAction_.actionType), actionRoot.get(JpaAction_.active),
                actionRoot.get(JpaAction_.forcedTime), actionRoot.get(JpaAction_.status),
                actionRoot.get(JpaAction_.createdAt), actionRoot.get(JpaAction_.lastModifiedAt),
                actionDsJoin.get(JpaDistributionSet_.id), actionDsJoin.get(JpaDistributionSet_.name),
                actionDsJoin.get(JpaDistributionSet_.version), cb.count(actionStatusJoin),
                actionRolloutJoin.get(JpaRollout_.name));
        multiselect.where(cb.equal(actionRoot.get(JpaAction_.target), target));
        multiselect.orderBy(cb.desc(actionRoot.get(JpaAction_.id)));
        multiselect.groupBy(actionRoot.get(JpaAction_.id));
        return Collections.unmodifiableList(entityManager.createQuery(multiselect).getResultList());
    }

    @Override
    public Page<Action> findActionsByTarget(final String rsqlParam, final Target target, final Pageable pageable) {

        final Specification<JpaAction> byTargetSpec = createSpecificationFor(target, rsqlParam);
        final Page<JpaAction> actions = actionRepository.findAll(byTargetSpec, pageable);
        return convertAcPage(actions, pageable);
    }

    private Specification<JpaAction> createSpecificationFor(final Target target, final String rsqlParam) {
        final Specification<JpaAction> spec = RSQLUtility.parse(rsqlParam, ActionFields.class, virtualPropertyReplacer);
        return (root, query, cb) -> cb.and(spec.toPredicate(root, query, cb),
                cb.equal(root.get(JpaAction_.target), target));
    }

    private static Page<Action> convertAcPage(final Page<JpaAction> findAll, final Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Slice<Action> findActionsByTarget(final Target foundTarget, final Pageable pageable) {
        return actionRepository.findByTarget(pageable, (JpaTarget) foundTarget);
    }

    @Override
    public Page<Action> findActiveActionsByTarget(final Pageable pageable, final Target target) {
        return actionRepository.findByActiveAndTarget(pageable, (JpaTarget) target, true);
    }

    @Override
    public List<Action> findActiveActionsByTarget(final Target target) {
        return actionRepository.findByActiveAndTarget((JpaTarget) target, true);
    }

    @Override
    public List<Action> findInActiveActionsByTarget(final Target target) {
        return actionRepository.findByActiveAndTarget((JpaTarget) target, false);
    }

    @Override
    public Page<Action> findInActiveActionsByTarget(final Pageable pageable, final Target target) {
        return actionRepository.findByActiveAndTarget(pageable, (JpaTarget) target, false);
    }

    @Override
    public Long countActionsByTarget(final Target target) {
        return actionRepository.countByTarget((JpaTarget) target);
    }

    @Override
    public Long countActionsByTarget(final String rsqlParam, final Target target) {
        return actionRepository.count(createSpecificationFor(target, rsqlParam));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Action forceTargetAction(final Long actionId) {
        final JpaAction action = actionRepository.findOne(actionId);
        if (action != null && !action.isForced()) {
            action.setActionType(ActionType.FORCED);
            return actionRepository.save(action);
        }
        return action;
    }

    @Override
    public Page<ActionStatus> findActionStatusByAction(final Pageable pageReq, final Action action) {
        return actionStatusRepository.findByAction(pageReq, (JpaAction) action);
    }

    @Override
    public Page<ActionStatus> findActionStatusByActionWithMessages(final Pageable pageReq, final Action action) {
        return actionStatusRepository.getByAction(pageReq, (JpaAction) action);
    }

    @Override
    public List<Action> findActionsByRolloutGroupParentAndStatus(final Rollout rollout,
            final RolloutGroup rolloutGroupParent, final Action.Status actionStatus) {
        return actionRepository.findByRolloutAndRolloutGroupParentAndStatus((JpaRollout) rollout,
                (JpaRolloutGroup) rolloutGroupParent, actionStatus);
    }

    @Override
    public List<Action> findActionsByRolloutAndStatus(final Rollout rollout, final Action.Status actionStatus) {
        return actionRepository.findByRolloutAndStatus((JpaRollout) rollout, actionStatus);
    }

    @Override
    public Page<ActionStatus> findActionStatusAll(final Pageable pageable) {
        return convertAcSPage(actionStatusRepository.findAll(pageable), pageable);
    }

    private static Page<ActionStatus> convertAcSPage(final Page<JpaActionStatus> findAll, final Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Long countActionStatusAll() {
        return actionStatusRepository.count();
    }

    @Override
    public Long countActionsAll() {
        return actionRepository.count();
    }

    @Override
    public Slice<Action> findActionsByDistributionSet(final Pageable pageable, final DistributionSet ds) {
        return actionRepository.findByDistributionSet(pageable, (JpaDistributionSet) ds);
    }

    @Override
    public Slice<Action> findActionsAll(final Pageable pageable) {
        return convertAcPage(actionRepository.findAll(pageable), pageable);
    }
}
