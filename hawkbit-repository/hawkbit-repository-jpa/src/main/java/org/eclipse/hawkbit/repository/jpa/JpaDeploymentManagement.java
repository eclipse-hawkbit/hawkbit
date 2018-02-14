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
import java.util.Objects;
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

import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
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
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.util.CollectionUtils;
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

    private final EntityManager entityManager;
    private final ActionRepository actionRepository;
    private final DistributionSetRepository distributionSetRepository;
    private final TargetRepository targetRepository;
    private final ActionStatusRepository actionStatusRepository;
    private final TargetManagement targetManagement;
    private final AuditorAware<String> auditorProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationContext applicationContext;
    private final AfterTransactionCommitExecutor afterCommit;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final PlatformTransactionManager txManager;
    private final OnlineDsAssignmentStrategy onlineDsAssignmentStrategy;
    private final OfflineDsAssignmentStrategy offlineDsAssignmentStrategy;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SystemSecurityContext systemSecurityContext;

    JpaDeploymentManagement(final EntityManager entityManager, final ActionRepository actionRepository,
            final DistributionSetRepository distributionSetRepository, final TargetRepository targetRepository,
            final ActionStatusRepository actionStatusRepository, final TargetManagement targetManagement,
            final AuditorAware<String> auditorProvider, final ApplicationEventPublisher eventPublisher,
            final ApplicationContext applicationContext, final AfterTransactionCommitExecutor afterCommit,
            final VirtualPropertyReplacer virtualPropertyReplacer, final PlatformTransactionManager txManager,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemSecurityContext systemSecurityContext) {
        this.entityManager = entityManager;
        this.actionRepository = actionRepository;
        this.distributionSetRepository = distributionSetRepository;
        this.targetRepository = targetRepository;
        this.actionStatusRepository = actionStatusRepository;
        this.targetManagement = targetManagement;
        this.auditorProvider = auditorProvider;
        this.eventPublisher = eventPublisher;
        this.applicationContext = applicationContext;
        this.afterCommit = afterCommit;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.txManager = txManager;
        onlineDsAssignmentStrategy = new OnlineDsAssignmentStrategy(targetRepository, afterCommit, eventPublisher,
                applicationContext, actionRepository, actionStatusRepository);
        offlineDsAssignmentStrategy = new OfflineDsAssignmentStrategy(targetRepository, afterCommit, eventPublisher,
                applicationContext, actionRepository, actionStatusRepository);
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetAssignmentResult offlineAssignedDistributionSet(final Long dsID,
            final Collection<String> controllerIDs) {
        return assignDistributionSetToTargets(dsID,
                controllerIDs.stream()
                        .map(controllerId -> new TargetWithActionType(controllerId, ActionType.FORCED, -1))
                        .collect(Collectors.toList()),
                null, offlineDsAssignmentStrategy);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetAssignmentResult assignDistributionSet(final long dsID, final ActionType actionType,
            final long forcedTimestamp, final Collection<String> controllerIDs) {

        return assignDistributionSetToTargets(dsID,
                controllerIDs.stream()
                        .map(controllerId -> new TargetWithActionType(controllerId, actionType, forcedTimestamp))
                        .collect(Collectors.toList()),
                null, onlineDsAssignmentStrategy);

    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetAssignmentResult assignDistributionSet(final long dsID,
            final Collection<TargetWithActionType> targets) {

        return assignDistributionSetToTargets(dsID, targets, null, onlineDsAssignmentStrategy);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetAssignmentResult assignDistributionSet(final long dsID,
            final Collection<TargetWithActionType> targets, final String actionMessage) {

        return assignDistributionSetToTargets(dsID, targets, actionMessage, onlineDsAssignmentStrategy);
    }

    /**
     * method assigns the {@link DistributionSet} to all {@link Target}s by
     * their IDs with a specific {@link ActionType} and {@code forcetime}.
     * 
     * 
     * In case the update was executed offline (i.e. not managed by hawkBit) the
     * handling differs my means that:<br/>
     * A. it ignores targets completely that are in
     * {@link TargetUpdateStatus#PENDING}.<br/>
     * B. it created completed actions.<br/>
     * C. sets both installed and assigned DS on the target and switches the
     * status to {@link TargetUpdateStatus#IN_SYNC} <br/>
     * D. does not send a {@link TargetAssignDistributionSetEvent}.<br/>
     *
     * @param dsID
     *            the ID of the distribution set to assign
     * @param targetsWithActionType
     *            a list of all targets and their action type
     * @param actionMessage
     *            an optional message to be written into the action status
     * @param offline
     *            to <code>true</code> in offline case
     * @return the assignment result
     *
     * @throw IncompleteDistributionSetException if mandatory
     *        {@link SoftwareModuleType} are not assigned as define by the
     *        {@link DistributionSetType}.
     */
    private DistributionSetAssignmentResult assignDistributionSetToTargets(final Long dsID,
            final Collection<TargetWithActionType> targetsWithActionType, final String actionMessage,
            final AbstractDsAssignmentStrategy assignmentStrategy) {

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
        final List<JpaTarget> targets = assignmentStrategy.findTargetsForAssignment(controllerIDs, set.getId());

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
        final Set<Long> targetIdsCancellList;

        if (systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, Boolean.class)
                .getValue())) {
            targetIdsCancellList = Collections.emptySet();
            assignmentStrategy.closeActiveActions(targetIds);
        } else {
            targetIdsCancellList = assignmentStrategy.cancelActiveActions(targetIds);
        }

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

        assignmentStrategy.updateTargetStatus(set, targetIds, currentUser);

        final Map<String, JpaAction> targetIdsToActions = targets.stream()
                .map(t -> actionRepository.save(assignmentStrategy.createTargetAction(targetsWithActionMap, t, set)))
                .collect(Collectors.toMap(a -> a.getTarget().getControllerId(), Function.identity()));

        // create initial action status when action is created so we remember
        // the initial running status because we will change the status
        // of the action itself and with this action status we have a nicer
        // action history.
        actionStatusRepository.save(targetIdsToActions.values().stream()
                .map(action -> assignmentStrategy.createActionStatus(action, actionMessage))
                .collect(Collectors.toList()));

        // detaching as it is not necessary to persist the set itself
        entityManager.detach(set);
        // detaching as the entity has been updated by the JPQL query above
        targets.forEach(entityManager::detach);

        assignmentStrategy.sendAssignmentEvents(set, targets, targetIdsCancellList, targetIdsToActions);

        return new DistributionSetAssignmentResult(
                targets.stream().map(Target::getControllerId).collect(Collectors.toList()), targets.size(),
                controllerIDs.size() - targets.size(), Lists.newArrayList(targetIdsToActions.values()),
                targetManagement);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action cancelAction(final long actionId) {
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
            onlineDsAssignmentStrategy.cancelAssignDistributionSetEvent(action.getTarget(), action.getId());

            return saveAction;
        } else {
            throw new CancelActionNotAllowedException(action.getId() + " is not active and cannot be canceled");
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action forceQuitAction(final long actionId) {
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
    public long startScheduledActionsByRolloutGroupParent(final long rolloutId, final long distributionSetId,
            final Long rolloutGroupParentId) {
        long totalActionsCount = 0L;
        long lastStartedActionsCount;
        do {
            lastStartedActionsCount = startScheduledActionsByRolloutGroupParentInNewTransaction(rolloutId,
                    distributionSetId, rolloutGroupParentId, ACTION_PAGE_LIMIT);
            totalActionsCount += lastStartedActionsCount;
        } while (lastStartedActionsCount > 0);

        return totalActionsCount;
    }

    private long startScheduledActionsByRolloutGroupParentInNewTransaction(final Long rolloutId,
            final Long distributionSetId, final Long rolloutGroupParentId, final int limit) {
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("startScheduledActions-" + rolloutId);
        def.setReadOnly(false);
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new TransactionTemplate(txManager, def).execute(status -> {
            final Page<Action> rolloutGroupActions = findActionsByRolloutAndRolloutGroupParent(rolloutId,
                    rolloutGroupParentId, limit);

            if (rolloutGroupActions.getContent().isEmpty()) {
                return 0L;
            }

            final String tenant = rolloutGroupActions.getContent().get(0).getTenant();

            final List<Action> targetAssignments = rolloutGroupActions.getContent().stream()
                    .map(action -> (JpaAction) action).map(this::closeActionIfSetWasAlreadyAssigned)
                    .filter(Objects::nonNull).map(this::startScheduledActionIfNoCancelationHasToBeHandledFirst)
                    .filter(Objects::nonNull).collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(targetAssignments)) {
                afterCommit.afterCommit(() -> eventPublisher.publishEvent(new TargetAssignDistributionSetEvent(tenant,
                        distributionSetId, targetAssignments, applicationContext.getId())));
            }

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

    private JpaAction closeActionIfSetWasAlreadyAssigned(final JpaAction action) {
        final JpaTarget target = (JpaTarget) action.getTarget();
        if (target.getAssignedDistributionSet() != null
                && action.getDistributionSet().getId().equals(target.getAssignedDistributionSet().getId())) {
            // the target has already the distribution set assigned, we don't
            // need to start the scheduled action, just finish it.
            action.setStatus(Status.FINISHED);
            action.setActive(false);
            setSkipActionStatus(action);
            actionRepository.save(action);
            return null;
        }

        return action;
    }

    private JpaAction startScheduledActionIfNoCancelationHasToBeHandledFirst(final JpaAction action) {
        // check if we need to override running update actions
        final List<Long> overrideObsoleteUpdateActions = onlineDsAssignmentStrategy
                .overrideObsoleteUpdateActions(Collections.singletonList(action.getTarget().getId()));

        action.setActive(true);
        action.setStatus(Status.RUNNING);
        final JpaAction savedAction = actionRepository.save(action);

        actionStatusRepository.save(onlineDsAssignmentStrategy.createActionStatus(savedAction, null));

        final JpaTarget target = (JpaTarget) entityManager.merge(savedAction.getTarget());

        target.setAssignedDistributionSet(savedAction.getDistributionSet());
        target.setUpdateStatus(TargetUpdateStatus.PENDING);
        targetRepository.save(target);

        // in case we canceled an action before for this target, then don't fire
        // assignment event
        if (overrideObsoleteUpdateActions.contains(savedAction.getId())) {
            return null;
        }

        return savedAction;
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
    public Optional<Action> findAction(final long actionId) {
        return Optional.ofNullable(actionRepository.findOne(actionId));
    }

    @Override
    public Optional<Action> findActionWithDetails(final long actionId) {
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
    public long countActionsByTarget(final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);

        return actionRepository.countByTargetControllerId(controllerId);
    }

    @Override
    public long countActionsByTarget(final String rsqlParam, final String controllerId) {
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
    public Action forceTargetAction(final long actionId) {
        final JpaAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.isForce()) {
            action.setActionType(ActionType.FORCED);
            return actionRepository.save(action);
        }
        return action;
    }

    @Override
    public Page<ActionStatus> findActionStatusByAction(final Pageable pageReq, final long actionId) {
        if (!actionRepository.exists(actionId)) {
            throw new EntityNotFoundException(Action.class, actionId);
        }

        return actionStatusRepository.findByActionId(pageReq, actionId);
    }

    @Override
    public Page<String> findMessagesByActionStatusId(final Pageable pageable, final long actionStatusId) {
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
    public long countActionStatusAll() {
        return actionStatusRepository.count();
    }

    @Override
    public long countActionsAll() {
        return actionRepository.count();
    }

    @Override
    public Slice<Action> findActionsByDistributionSet(final Pageable pageable, final long dsId) {
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
