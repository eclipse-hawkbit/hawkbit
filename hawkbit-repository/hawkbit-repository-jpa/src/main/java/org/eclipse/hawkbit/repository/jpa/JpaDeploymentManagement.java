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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ForceQuitActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Lists;

/**
 * JPA implementation for {@link DeploymentManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaDeploymentManagement implements DeploymentManagement {
    private static final Logger LOG = LoggerFactory.getLogger(JpaDeploymentManagement.class);

    /**
     * Maximum amount of Actions that are started at once.
     */
    private static final int ACTION_PAGE_LIMIT = 1000;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private DistributionSetRepository distributionSetRepository;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private ActionStatusRepository actionStatusRepository;

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private AuditorAware<String> auditorProvider;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    @Autowired
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Autowired
    private PlatformTransactionManager txManager;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetAssignmentResult assignDistributionSet(final Long dsID, final ActionType actionType,
            final long forcedTimestamp, final Collection<String> targetIDs) {

        return assignDistributionSetToTargets(dsID, targetIDs.stream()
                .map(t -> new TargetWithActionType(t, actionType, forcedTimestamp)).collect(Collectors.toList()), null);

    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetAssignmentResult assignDistributionSet(final Long dsID,
            final Collection<TargetWithActionType> targets) {

        return assignDistributionSetToTargets(dsID, targets, null);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetAssignmentResult assignDistributionSet(final Long dsID,
            final Collection<TargetWithActionType> targets, final String actionMessage) {

        return assignDistributionSetToTargets(dsID, targets, actionMessage);
    }

    /**
     * method assigns the {@link DistributionSet} to all {@link Target}s by
     * their IDs with a specific {@link ActionType} and {@code forcetime}.
     *
     * @param dsID
     *            the ID of the distribution set to assign
     * @param targetsWithActionType
     *            a list of all targets and their action type
     * @param actionMessage
     *            an optional message to be written into the action status
     * @return the assignment result
     *
     * @throw IncompleteDistributionSetException if mandatory
     *        {@link SoftwareModuleType} are not assigned as define by the
     *        {@link DistributionSetType}.
     */
    private DistributionSetAssignmentResult assignDistributionSetToTargets(final Long dsID,
            final Collection<TargetWithActionType> targetsWithActionType, final String actionMessage) {

        final JpaDistributionSet set = distributionSetRepository.findOne(dsID);
        if (set == null) {
            throw new EntityNotFoundException(DistributionSet.class, dsID);
        }

        if (!set.isComplete()) {
            throw new IncompleteDistributionSetException(
                    "Distribution set of type " + set.getType().getKey() + " is incomplete: " + set.getId());
        }

        final List<String> controllerIDs = targetsWithActionType.stream().map(TargetWithActionType::getControllerId)
                .collect(Collectors.toList());

        LOG.debug("assignDistribution({}) to {} targets", set, controllerIDs.size());

        final Map<String, TargetWithActionType> targetsWithActionMap = targetsWithActionType.stream()
                .collect(Collectors.toMap(TargetWithActionType::getControllerId, Function.identity()));

        // split tIDs length into max entries in-statement because many database
        // have constraint of max entries in in-statements e.g. Oracle with
        // maximum 1000 elements, so we need to split the entries here and
        // execute multiple statements we take the target only into account if
        // the requested operation is no duplicate of a previous one
        final List<JpaTarget> targets = Lists.partition(controllerIDs, Constants.MAX_ENTRIES_IN_STATEMENT).stream()
                .map(ids -> targetRepository
                        .findAll(TargetSpecifications.hasControllerIdAndAssignedDistributionSetIdNot(ids, set.getId())))
                .flatMap(List::stream).collect(Collectors.toList());

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
        final Set<Long> targetIdsCancellList = targetIds.stream().map(this::overrideObsoleteUpdateActions)
                .flatMap(Collection::stream).collect(Collectors.toSet());

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

        targetIds.forEach(tIds -> targetRepository.setAssignedDistributionSetAndUpdateStatus(TargetUpdateStatus.PENDING,
                set, System.currentTimeMillis(), currentUser, tIds));
        final Map<String, JpaAction> targetIdsToActions = targets.stream()
                .map(t -> actionRepository.save(createTargetAction(targetsWithActionMap, t, set)))
                .collect(Collectors.toMap(a -> a.getTarget().getControllerId(), Function.identity()));

        // create initial action status when action is created so we remember
        // the initial running status because we will change the status
        // of the action itself and with this action status we have a nicer
        // action history.
        targetIdsToActions.values().forEach(action -> setRunningActionStatus(action, actionMessage));

        // detaching as it is not necessary to persist the set itself
        entityManager.detach(set);
        // detaching as the entity has been updated by the JPQL query above
        targets.forEach(entityManager::detach);

        sendAssignmentEvents(targets, targetIdsCancellList, targetIdsToActions);

        return new DistributionSetAssignmentResult(
                targets.stream().map(Target::getControllerId).collect(Collectors.toList()), targets.size(),
                controllerIDs.size() - targets.size(), Lists.newArrayList(targetIdsToActions.values()),
                targetManagement);
    }

    private void sendAssignmentEvents(final List<JpaTarget> targets, final Set<Long> targetIdsCancellList,
            final Map<String, JpaAction> targetIdsToActions) {

        targets.forEach(target -> {
            sendTargetUpdatedEvent(target);
            if (targetIdsCancellList.contains(target.getId())) {
                return;
            }

            sendTargetAssignDistributionSetEvent(targetIdsToActions.get(target.getControllerId()));
        });
    }

    private static JpaAction createTargetAction(final Map<String, TargetWithActionType> targetsWithActionMap,
            final JpaTarget target, final JpaDistributionSet set) {
        final JpaAction actionForTarget = new JpaAction();
        final TargetWithActionType targetWithActionType = targetsWithActionMap.get(target.getControllerId());
        actionForTarget.setActionType(targetWithActionType.getActionType());
        actionForTarget.setForcedTime(targetWithActionType.getForceTime());
        actionForTarget.setActive(true);
        actionForTarget.setStatus(Status.RUNNING);
        actionForTarget.setTarget(target);
        actionForTarget.setDistributionSet(set);
        return actionForTarget;
    }

    private void sendTargetAssignDistributionSetEvent(final Action action) {
        afterCommit.afterCommit(() -> eventPublisher
                .publishEvent(new TargetAssignDistributionSetEvent(action, applicationContext.getId())));
    }

    private void sendTargetUpdatedEvent(final JpaTarget target) {

        // Update is not available in the object as the update was executed
        // through JQL
        target.setUpdateStatus(TargetUpdateStatus.PENDING);

        afterCommit.afterCommit(
                () -> eventPublisher.publishEvent(new TargetUpdatedEvent(target, applicationContext.getId())));
    }

    /**
     * Removes {@link Action}s that are no longer necessary and sends
     * cancellations to the controller.
     *
     * @param targetsIds
     *            to override {@link Action}s
     */
    private List<Long> overrideObsoleteUpdateActions(final Collection<Long> targetsIds) {

        // Figure out if there are potential target/action combinations that
        // need to be considered for cancellation
        final List<JpaAction> activeActions = actionRepository
                .findByActiveAndTargetIdInAndActionStatusNotEqualToAndDistributionSetRequiredMigrationStep(targetsIds,
                        Action.Status.CANCELING);

        return activeActions.stream().map(action -> {
            action.setStatus(Status.CANCELING);
            // document that the status has been retrieved

            actionStatusRepository.save(new JpaActionStatus(action, Status.CANCELING, System.currentTimeMillis(),
                    RepositoryConstants.SERVER_MESSAGE_PREFIX + "cancel obsolete action due to new update"));
            actionRepository.save(action);

            cancelAssignDistributionSetEvent(action.getTarget(), action.getId());

            return action.getTarget().getId();
        }).collect(Collectors.toList());

    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action cancelAction(final Long actionId) {
        LOG.debug("cancelAction({})", actionId);

        final JpaAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (action.isCancelingOrCanceled()) {
            throw new CancelActionNotAllowedException("Actions in canceling or canceled state cannot be canceled");
        }

        if (action.isActive()) {
            LOG.debug("action ({}) was still active. Change to {}.", action, Status.CANCELING);
            action.setStatus(Status.CANCELING);

            // document that the status has been retrieved
            actionStatusRepository.save(new JpaActionStatus(action, Status.CANCELING, System.currentTimeMillis(),
                    RepositoryConstants.SERVER_MESSAGE_PREFIX + "manual cancelation requested"));
            final Action saveAction = actionRepository.save(action);
            cancelAssignDistributionSetEvent(action.getTarget(), action.getId());

            return saveAction;
        } else {
            throw new CancelActionNotAllowedException(action.getId() + " is not active and cannot be canceled");
        }
    }

    /**
     * Sends the {@link CancelTargetAssignmentEvent} for a specific target to
     * the eventPublisher.
     *
     * @param target
     *            the Target which has been assigned to a distribution set
     * @param actionId
     *            the action id of the assignment
     */
    private void cancelAssignDistributionSetEvent(final Target target, final Long actionId) {
        afterCommit.afterCommit(() -> eventPublisher
                .publishEvent(new CancelTargetAssignmentEvent(target, actionId, applicationContext.getId())));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action forceQuitAction(final Long actionId) {
        final JpaAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.isCancelingOrCanceled()) {
            throw new ForceQuitActionNotAllowedException(
                    action.getId() + " is not canceled yet and cannot be force quit");
        }

        if (!action.isActive()) {
            throw new ForceQuitActionNotAllowedException(action.getId() + " is not active and cannot be force quit");
        }

        LOG.warn("action ({}) was still activ and has been force quite.", action);

        // document that the status has been retrieved
        actionStatusRepository.save(new JpaActionStatus(action, Status.CANCELED, System.currentTimeMillis(),
                RepositoryConstants.SERVER_MESSAGE_PREFIX + "A force quit has been performed."));

        DeploymentHelper.successCancellation(action, actionRepository, targetRepository);

        return actionRepository.save(action);
    }

    @Override
    public long startScheduledActionsByRolloutGroupParent(@NotNull final Long rolloutId,
            final Long rolloutGroupParentId) {
        long totalActionsCount = 0L;
        long lastStartedActionsCount;
        do {
            lastStartedActionsCount = startScheduledActionsByRolloutGroupParentInNewTransaction(rolloutId,
                    rolloutGroupParentId, ACTION_PAGE_LIMIT);
            totalActionsCount += lastStartedActionsCount;
        } while (lastStartedActionsCount > 0);

        return totalActionsCount;
    }

    private long startScheduledActionsByRolloutGroupParentInNewTransaction(final Long rolloutId,
            final Long rolloutGroupParentId, final int limit) {
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("startScheduledActions-" + rolloutId);
        def.setReadOnly(false);
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new TransactionTemplate(txManager, def).execute(status -> {
            final Page<Action> rolloutGroupActions = findActionsByRolloutAndRolloutGroupParent(rolloutId,
                    rolloutGroupParentId, limit);

            rolloutGroupActions.map(action -> (JpaAction) action).forEach(this::startScheduledAction);

            return rolloutGroupActions.getTotalElements();
        });
    }

    private Page<Action> findActionsByRolloutAndRolloutGroupParent(final Long rolloutId,
            final Long rolloutGroupParentId, final int limit) {

        final PageRequest pageRequest = new PageRequest(0, limit);
        if (rolloutGroupParentId == null) {
            return actionRepository.findByRolloutIdAndRolloutGroupParentIsNullAndStatus(pageRequest, rolloutId,
                    Action.Status.SCHEDULED);
        } else {
            return actionRepository.findByRolloutIdAndRolloutGroupParentIdAndStatus(pageRequest, rolloutId,
                    rolloutGroupParentId, Action.Status.SCHEDULED);
        }
    }

    private void startScheduledAction(final JpaAction action) {
        JpaTarget target = (JpaTarget) action.getTarget();

        if (target.getAssignedDistributionSet() != null
                && action.getDistributionSet().getId().equals(target.getAssignedDistributionSet().getId())) {
            // the target has already the distribution set assigned, we don't
            // need to start the scheduled action, just finish it.
            action.setStatus(Status.FINISHED);
            action.setActive(false);
            setSkipActionStatus(action);
            actionRepository.save(action);
            return;
        }

        // check if we need to override running update actions
        final List<Long> overrideObsoleteUpdateActions = overrideObsoleteUpdateActions(
                Collections.singletonList(action.getTarget().getId()));

        action.setActive(true);
        action.setStatus(Status.RUNNING);
        final JpaAction savedAction = actionRepository.save(action);

        setRunningActionStatus(savedAction, null);

        target = (JpaTarget) entityManager.merge(savedAction.getTarget());

        target.setAssignedDistributionSet(savedAction.getDistributionSet());
        target.setUpdateStatus(TargetUpdateStatus.PENDING);
        targetRepository.save(target);

        // in case we canceled an action before for this target, then don't fire
        // assignment event
        if (!overrideObsoleteUpdateActions.contains(savedAction.getId())) {
            afterCommit.afterCommit(() -> eventPublisher
                    .publishEvent(new TargetAssignDistributionSetEvent(savedAction, applicationContext.getId())));
        }
    }

    private void setRunningActionStatus(final JpaAction action, final String actionMessage) {
        final JpaActionStatus actionStatus = new JpaActionStatus();
        actionStatus.setAction(action);
        actionStatus.setOccurredAt(action.getCreatedAt());
        actionStatus.setStatus(Status.RUNNING);
        if (actionMessage != null) {
            actionStatus.addMessage(actionMessage);
        }

        actionStatusRepository.save(actionStatus);
    }

    private void setSkipActionStatus(final JpaAction action) {
        final JpaActionStatus actionStatus = new JpaActionStatus();
        actionStatus.setAction(action);
        actionStatus.setOccurredAt(action.getCreatedAt());
        actionStatus.setStatus(Status.RUNNING);
        actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX
                + "Distribution Set is already assigned. Skipping this action.");
        actionStatusRepository.save(actionStatus);
    }

    @Override
    public Optional<Action> findAction(final Long actionId) {
        return Optional.ofNullable(actionRepository.findOne(actionId));
    }

    @Override
    public Optional<Action> findActionWithDetails(final Long actionId) {
        return actionRepository.getById(actionId);
    }

    @Override
    public Slice<Action> findActionsByTarget(final String controllerId, final Pageable pageable) {
        throwExceptionIfTargetDoesNotExist(controllerId);
        return actionRepository.findByTargetControllerId(pageable, controllerId);
    }

    @Override
    public Page<Action> findActionsByTarget(final String rsqlParam, final String controllerId,
            final Pageable pageable) {
        throwExceptionIfTargetDoesNotExist(controllerId);

        final Specification<JpaAction> byTargetSpec = createSpecificationFor(controllerId, rsqlParam);
        final Page<JpaAction> actions = actionRepository.findAll(byTargetSpec, pageable);
        return convertAcPage(actions, pageable);
    }

    private Specification<JpaAction> createSpecificationFor(final String controllerId, final String rsqlParam) {
        final Specification<JpaAction> spec = RSQLUtility.parse(rsqlParam, ActionFields.class, virtualPropertyReplacer);
        return (root, query, cb) -> cb.and(spec.toPredicate(root, query, cb),
                cb.equal(root.get(JpaAction_.target).get(JpaTarget_.controllerId), controllerId));
    }

    private static Page<Action> convertAcPage(final Page<JpaAction> findAll, final Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Page<Action> findActiveActionsByTarget(final Pageable pageable, final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);

        return actionRepository.findByActiveAndTarget(pageable, controllerId, true);
    }

    @Override
    public Page<Action> findInActiveActionsByTarget(final Pageable pageable, final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);

        return actionRepository.findByActiveAndTarget(pageable, controllerId, false);
    }

    @Override
    public Long countActionsByTarget(final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);

        return actionRepository.countByTargetControllerId(controllerId);
    }

    @Override
    public Long countActionsByTarget(final String rsqlParam, final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);

        return actionRepository.count(createSpecificationFor(controllerId, rsqlParam));
    }

    private void throwExceptionIfTargetDoesNotExist(final String controllerId) {
        if (!targetRepository.existsByControllerId(controllerId)) {
            throw new EntityNotFoundException(Target.class, controllerId);
        }
    }

    private void throwExceptionIfDistributionSetDoesNotExist(final Long dsId) {
        if (!distributionSetRepository.exists(dsId)) {
            throw new EntityNotFoundException(DistributionSet.class, dsId);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action forceTargetAction(final Long actionId) {
        final JpaAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.isForce()) {
            action.setActionType(ActionType.FORCED);
            return actionRepository.save(action);
        }
        return action;
    }

    @Override
    public Page<ActionStatus> findActionStatusByAction(final Pageable pageReq, final Long actionId) {
        if (!actionRepository.exists(actionId)) {
            throw new EntityNotFoundException(Action.class, actionId);
        }

        return actionStatusRepository.findByActionId(pageReq, actionId);
    }

    @Override
    public Page<String> findMessagesByActionStatusId(final Pageable pageable, final Long actionStatusId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        final CriteriaQuery<Long> countMsgQuery = cb.createQuery(Long.class);
        final Root<JpaActionStatus> countMsgQueryFrom = countMsgQuery.distinct(true).from(JpaActionStatus.class);
        final ListJoin<JpaActionStatus, String> cJoin = countMsgQueryFrom.joinList("messages", JoinType.LEFT);
        countMsgQuery.select(cb.count(cJoin))
                .where(cb.equal(countMsgQueryFrom.get(JpaActionStatus_.id), actionStatusId));
        final Long totalCount = entityManager.createQuery(countMsgQuery).getSingleResult();

        final CriteriaQuery<String> msgQuery = cb.createQuery(String.class);
        final Root<JpaActionStatus> as = msgQuery.from(JpaActionStatus.class);
        final ListJoin<JpaActionStatus, String> join = as.joinList("messages", JoinType.LEFT);
        final CriteriaQuery<String> selMsgQuery = msgQuery.select(join);
        selMsgQuery.where(cb.equal(as.get(JpaActionStatus_.id), actionStatusId));

        final List<String> result = entityManager.createQuery(selMsgQuery).setFirstResult(pageable.getOffset())
                .setMaxResults(pageable.getPageSize()).getResultList().stream().collect(Collectors.toList());

        return new PageImpl<>(result, pageable, totalCount);
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
    public Slice<Action> findActionsByDistributionSet(final Pageable pageable, final Long dsId) {
        throwExceptionIfDistributionSetDoesNotExist(dsId);

        return actionRepository.findByDistributionSetId(pageable, dsId);
    }

    @Override
    public Slice<Action> findActionsAll(final Pageable pageable) {
        return convertAcPage(actionRepository.findAll(pageable), pageable);
    }

    @Override
    public Optional<DistributionSet> getAssignedDistributionSet(final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);

        return distributionSetRepository.findAssignedToTarget(controllerId);
    }

    @Override
    public Optional<DistributionSet> getInstalledDistributionSet(final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);

        return distributionSetRepository.findInstalledAtTarget(controllerId);
    }
}
