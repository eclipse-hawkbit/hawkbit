/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Root;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ForceQuitActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.IncompatibleTargetTypeException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.MultiAssignmentIsNotEnabledException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.jpa.Jpa;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.ActionSpecifications;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.jpa.utils.WeightValidationHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.repository.qfields.ActionFields;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Transactional(readOnly = true)
@Validated
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "deployment-management" }, matchIfMissing = true)
public class JpaDeploymentManagement extends JpaActionManagement implements DeploymentManagement {

    /**
     * Maximum amount of Actions that are started at once.
     */
    private static final int ACTION_PAGE_LIMIT = 1000;
    private static final String QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED_DEFAULT = "DELETE FROM sp_action " + "WHERE tenant=" + Jpa.nativeQueryParamPrefix() + "tenant" + " AND status IN (%s)" + " AND last_modified_at<" + Jpa.nativeQueryParamPrefix() + "last_modified_at LIMIT " + ACTION_PAGE_LIMIT;
    private static final EnumMap<Database, String> QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED;

    static {
        QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED = new EnumMap<>(Database.class);
        QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED.put(Database.POSTGRESQL,
                "DELETE FROM sp_action " + "WHERE id IN (SELECT id FROM sp_action " + "WHERE tenant=" + Jpa.nativeQueryParamPrefix() + "tenant" + " AND status IN (%s)" + " AND last_modified_at<" + Jpa.nativeQueryParamPrefix() + "last_modified_at LIMIT " + ACTION_PAGE_LIMIT + ")");
    }

    private final JpaDistributionSetManagement distributionSetManagement;
    private final TargetRepository targetRepository;
    private final EntityManager entityManager;
    private final PlatformTransactionManager txManager;
    private final Database database;

    private final RetryTemplate retryTemplate;
    private final OnlineDsAssignmentStrategy onlineDsAssignmentStrategy;
    private final OfflineDsAssignmentStrategy offlineDsAssignmentStrategy;

    @SuppressWarnings("java:S107")
    protected JpaDeploymentManagement(
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository,
            final QuotaManagement quotaManagement, final RepositoryProperties repositoryProperties,
            final JpaDistributionSetManagement distributionSetManagement, final TargetRepository targetRepository,
            final EntityManager entityManager, final PlatformTransactionManager txManager, final JpaProperties jpaProperties) {
        super(actionRepository, actionStatusRepository, quotaManagement, repositoryProperties);
        this.distributionSetManagement = distributionSetManagement;
        this.targetRepository = targetRepository;
        this.entityManager = entityManager;
        this.txManager = txManager;
        this.database = jpaProperties.getDatabase();

        retryTemplate = createRetryTemplate();
        final Consumer<MaxAssignmentsExceededInfo> maxAssignmentsExceededHandler = maxAssignmentsExceededInfo ->
                handleMaxAssignmentsExceeded(
                        maxAssignmentsExceededInfo.targetId,
                        maxAssignmentsExceededInfo.requested,
                        maxAssignmentsExceededInfo.quotaExceededException);
        onlineDsAssignmentStrategy = new OnlineDsAssignmentStrategy(targetRepository, actionRepository, actionStatusRepository,
                quotaManagement, this::isMultiAssignmentsEnabled, this::isConfirmationFlowEnabled, repositoryProperties,
                maxAssignmentsExceededHandler);
        offlineDsAssignmentStrategy = new OfflineDsAssignmentStrategy(targetRepository, actionRepository, actionStatusRepository,
                quotaManagement, this::isMultiAssignmentsEnabled, this::isConfirmationFlowEnabled, repositoryProperties,
                maxAssignmentsExceededHandler);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DistributionSetAssignmentResult> assignDistributionSets(
            final List<DeploymentRequest> deploymentRequests, final String actionMessage) {
        WeightValidationHelper.validate(deploymentRequests);
        return assignDistributionSets(deploymentRequests, actionMessage, onlineDsAssignmentStrategy);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DistributionSetAssignmentResult> offlineAssignedDistributionSets(final Collection<Entry<String, Long>> assignments) {
        final Collection<Entry<String, Long>> distinctAssignments = assignments.stream().distinct().toList();
        enforceMaxAssignmentsPerRequest(distinctAssignments.size());

        final List<DeploymentRequest> deploymentRequests = distinctAssignments.stream()
                .map(entry -> DeploymentRequest.builder(entry.getKey(), entry.getValue()).build()).toList();

        return assignDistributionSets(deploymentRequests, null, offlineDsAssignmentStrategy);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action cancelAction(final long actionId) {
        return cancelAction0(actionId);
    }

    private Action cancelAction0(final long actionId) {
        log.debug("cancelAction({})", actionId);

        final JpaAction action = actionRepository.getById(actionId);

        if (action.isCancelingOrCanceled()) {
            throw new CancelActionNotAllowedException("Actions in canceling or canceled state cannot be canceled");
        }

        assertTargetUpdateAllowed(action);

        if (action.isActive()) {
            log.debug("action ({}) was still active. Change to {}.", action, Status.CANCELING);
            action.setStatus(Status.CANCELING);

            // document that the status has been retrieved
            actionStatusRepository.save(new JpaActionStatus(action, Status.CANCELING, java.lang.System.currentTimeMillis(),
                    RepositoryConstants.SERVER_MESSAGE_PREFIX + "manual cancelation requested"));
            final Action saveAction = actionRepository.save(action);

            onlineDsAssignmentStrategy.sendCancellationMessage(action);

            return saveAction;
        } else {
            throw new CancelActionNotAllowedException(action.getId() + " is not active and cannot be canceled");
        }
    }

    @Override
    public long countActionsByTarget(final String rsql, final String controllerId) {
        assertTargetReadAllowed(controllerId);

        final List<Specification<JpaAction>> specList = Arrays.asList(
                QLSupport.getInstance().buildSpec(rsql, ActionFields.class),
                ActionSpecifications.byTargetControllerId(controllerId));

        return JpaManagementHelper.countBySpec(actionRepository, specList);
    }

    @Override
    public long countActionsAll() {
        return actionRepository.count();
    }

    @Override
    public long countActions(final String rsql) {
        final List<Specification<JpaAction>> specList = List.of(QLSupport.getInstance().buildSpec(rsql, ActionFields.class));
        return JpaManagementHelper.countBySpec(actionRepository, specList);
    }

    @Override
    public long countActionsByTarget(final String controllerId) {
        assertTargetReadAllowed(controllerId);
        return actionRepository.countByTargetControllerId(controllerId);
    }

    @Override
    public Optional<Action> findAction(final long actionId) {
        return actionRepository.findById(actionId)
                .filter(action -> targetRepository.exists(TargetSpecifications.hasId(action.getTarget().getId()))).map(JpaAction.class::cast);
    }

    @Override
    public Slice<Action> findActionsAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(actionRepository, null, pageable);
    }

    @Override
    public Slice<Action> findActions(final String rsql, final Pageable pageable) {
        final List<Specification<JpaAction>> specList = List.of(QLSupport.getInstance().buildSpec(rsql, ActionFields.class));
        return JpaManagementHelper.findAllWithoutCountBySpec(actionRepository, specList, pageable);
    }

    @Override
    public Page<Action> findActionsByTarget(final String rsql, final String controllerId, final Pageable pageable) {
        assertTargetReadAllowed(controllerId);

        final List<Specification<JpaAction>> specList = Arrays.asList(
                QLSupport.getInstance().buildSpec(rsql, ActionFields.class),
                ActionSpecifications.byTargetControllerId(controllerId));

        return JpaManagementHelper.findAllWithCountBySpec(actionRepository, specList, pageable);
    }

    @Override
    public Slice<Action> findActionsByTarget(final String controllerId, final Pageable pageable) {
        assertTargetReadAllowed(controllerId);
        return actionRepository.findAll(ActionSpecifications.byTargetControllerId(controllerId), pageable).map(Action.class::cast);
    }

    @Override
    public Page<ActionStatus> findActionStatusByAction(final long actionId, final Pageable pageable) {
        assertActionExistsAndAccessible(actionId);

        return actionStatusRepository.findByActionId(pageable, actionId);
    }

    // action is already got and there are checked read permissions - do not check permissions and UI which is to be removed
    @Override
    public Page<String> findMessagesByActionStatusId(final long actionStatusId, final Pageable pageable) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        final CriteriaQuery<String> msgQuery = cb.createQuery(String.class);
        final Root<JpaActionStatus> as = msgQuery.from(JpaActionStatus.class);
        final ListJoin<JpaActionStatus, String> join = as.joinList("messages", JoinType.LEFT);
        final CriteriaQuery<String> selMsgQuery = msgQuery.select(join);
        selMsgQuery.where(cb.equal(as.get(AbstractJpaBaseEntity_.id), actionStatusId));

        final List<String> result = new ArrayList<>(
                entityManager.createQuery(selMsgQuery).setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize())
                        .getResultList());

        return new PageImpl<>(result, pageable, result.size());
    }

    @Override
    public Optional<Action> findActionWithDetails(final long actionId) {
        return actionRepository.findWithDetailsById(actionId)
                .filter(action -> targetRepository.exists(TargetSpecifications.hasId(action.getTarget().getId())));
    }

    @Override
    public Page<Action> findActiveActionsByTarget(final String controllerId, final Pageable pageable) {
        assertTargetReadAllowed(controllerId);
        return actionRepository.findAll(ActionSpecifications.byTargetControllerIdAndActive(controllerId, true), pageable)
                .map(Action.class::cast);
    }

    @Override
    public List<Action> findActiveActionsWithHighestWeight(final String controllerId, final int maxActionCount) {
        assertTargetReadAllowed(controllerId);
        return findActiveActionsWithHighestWeightConsideringDefault(controllerId, maxActionCount);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(retryFor = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action forceQuitAction(final long actionId) {
        return forceQuitAction0(actionId);
    }

    private Action forceQuitAction0(final long actionId) {
        final JpaAction action = actionRepository.getById(actionId);

        if (!action.isCancelingOrCanceled()) {
            throw new ForceQuitActionNotAllowedException(action.getId() + " is not canceled yet and cannot be force quit");
        }

        if (!action.isActive()) {
            throw new ForceQuitActionNotAllowedException(action.getId() + " is not active and cannot be force quit");
        }

        assertTargetUpdateAllowed(action);

        log.warn("action ({}) was still active and has been force quite.", action);

        // document that the status has been retrieved
        actionStatusRepository.save(new JpaActionStatus(action, Status.CANCELED, java.lang.System.currentTimeMillis(),
                RepositoryConstants.SERVER_MESSAGE_PREFIX + "A force quit has been performed."));

        DeploymentHelper.successCancellation(action, actionRepository, targetRepository);

        return actionRepository.save(action);
    }

    @Override
    @Transactional
    @Retryable(retryFor = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action forceTargetAction(final long actionId) {
        final JpaAction action = actionRepository.findById(actionId)
                .map(this::assertTargetUpdateAllowed)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.isForcedOrTimeForced()) {
            action.setActionType(ActionType.FORCED);
            return actionRepository.save(action);
        }
        return action;
    }

    @Override
    @Transactional
    public void deleteAction(final long actionId) {
        log.info("Deleting action {}", actionId);
        actionRepository.deleteById(actionId);
    }

    @Override
    @Transactional
    public void deleteActionsByRsql(final String rsql) {
        log.info("Deleting actions matching rsql {}", rsql);
        actionRepository.delete(QLSupport.getInstance().buildSpec(rsql, ActionFields.class));
    }

    @Override
    @Transactional
    public void deleteActionsByIds(final List<Long> actionIds) {
        log.info("Deleting actions with ids {}", actionIds);
        actionRepository.deleteAllById(actionIds);
    }

    @Override
    @Transactional
    public void deleteTargetActionsByIds(final String controllerId, final List<Long> actionsIds) {
        log.info("Delete actions for target {} with action ids {}", controllerId, actionsIds);
        actionRepository.delete(ActionSpecifications.byControllerIdAndIdIn(controllerId, actionsIds));
    }

    @Override
    @Transactional
    public void deleteOldestTargetActions(final String controllerId, final int keepLast) {
        final JpaTarget target = targetRepository.findByControllerId(controllerId).orElseThrow(EntityNotFoundException::new);
        // check access to target since deletion will be executed via native query
        targetRepository.getAccessController().ifPresent(accessController ->
                accessController.assertOperationAllowed(AccessController.Operation.UPDATE, target));
        final long targetActions = actionRepository.countByTargetId(target.getId());
        if (targetActions > keepLast) {
            final long oldestToDelete = targetActions - keepLast;
            deleteOldestTargetActions(target.getId(), (int) oldestToDelete);
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void cancelInactiveScheduledActionsForTargets(final List<Long> targetIds) {
        if (!isMultiAssignmentsEnabled()) {
            targetRepository.getAccessController().ifPresent(v -> {
                if (targetRepository.count(AccessController.Operation.UPDATE, TargetSpecifications.hasIdIn(targetIds)) != targetIds.size()) {
                    throw new EntityNotFoundException(Target.class, targetIds);
                }
            });
            actionRepository.switchStatus(Status.CANCELED, targetIds, false, Status.SCHEDULED);
        } else {
            log.debug("The Multi Assignments feature is enabled: No need to cancel inactive scheduled actions.");
        }
    }

    @Override
    public void startScheduledActionsByRolloutGroupParent(final long rolloutId, final long distributionSetId, final Long rolloutGroupParentId) {
        while (DeploymentHelper.runInNewTransaction(txManager, "startScheduledActions-" + rolloutId, status -> {
            final PageRequest pageRequest = PageRequest.of(0, ACTION_PAGE_LIMIT);
            final Page<Action> groupScheduledActions;
            if (rolloutGroupParentId == null) {
                groupScheduledActions = actionRepository.findByRolloutIdAndRolloutGroupParentIsNullAndStatus(
                        pageRequest, rolloutId, Action.Status.SCHEDULED);
            } else {
                groupScheduledActions = actionRepository.findByRolloutIdAndRolloutGroupParentIdAndStatus(
                        pageRequest, rolloutId, rolloutGroupParentId, Action.Status.SCHEDULED);
            }

            if (groupScheduledActions.getContent().isEmpty()) {
                return 0L;
            } else {
                // self invocation won't check @PreAuthorize but it is already checked for the method
                startScheduledActions(groupScheduledActions.getContent());
                return groupScheduledActions.getTotalElements();
            }
        }) > 0) ;
    }

    @Override
    public void startScheduledActions(final List<Action> rolloutGroupActions) {
        // Close actions already assigned and collect pending assignments
        final List<JpaAction> pendingTargetAssignments = rolloutGroupActions.stream()
                .map(JpaAction.class::cast).map(this::closeActionIfSetWasAlreadyAssigned).filter(Objects::nonNull).toList();
        if (pendingTargetAssignments.isEmpty()) {
            return;
        }
        // check if old actions needs to be canceled first
        final List<Action> newTargetAssignments = startScheduledActionsAndHandleOpenCancellationFirst(pendingTargetAssignments);
        if (!newTargetAssignments.isEmpty()) {
            onlineDsAssignmentStrategy.sendDeploymentEvents(newTargetAssignments.get(0).getDistributionSet().getId(), newTargetAssignments);
        }
    }

    @Override
    public Optional<DistributionSet> findAssignedDistributionSet(final String controllerId) {
        return targetRepository.findWithDetailsByControllerId(controllerId, JpaTarget_.GRAPH_TARGET_ASSIGNED_DISTRIBUTION_SET)
                .map(JpaTarget::getAssignedDistributionSet);
    }

    @Override
    public Optional<DistributionSet> findInstalledDistributionSet(final String controllerId) {
        return targetRepository.findWithDetailsByControllerId(controllerId, JpaTarget_.GRAPH_TARGET_INSTALLED_DISTRIBUTION_SET)
                .map(JpaTarget::getInstalledDistributionSet);
    }

    @Override
    @Transactional
    public int deleteActionsByStatusAndLastModifiedBefore(final Set<Status> status, final long lastModified) {
        if (status.isEmpty()) {
            return 0;
        }

        // We use a native query here because Spring JPA does not support to specify a LIMIT clause on a DELETE statement.
        // However, for this specific use case (action cleanup), we must specify a row limit to reduce the overall load of
        // the database.
        final List<Integer> statusList = status.stream().map(Status::ordinal).toList();

        final Query deleteQuery = entityManager.createNativeQuery(
                String.format(getQueryForDeleteActionsByStatusAndLastModifiedBeforeString(database),
                        Jpa.formatNativeQueryInClause("status", statusList)));

        deleteQuery.setParameter("tenant", AccessContext.tenant().toUpperCase());
        Jpa.setNativeQueryInParameter(deleteQuery, "status", statusList);
        deleteQuery.setParameter("last_modified_at", lastModified);

        log.debug("Action cleanup: Executing the following (native) query: {}", deleteQuery);
        return deleteQuery.executeUpdate();
    }

    @Override
    public boolean hasPendingCancellations(final Long targetId) {
        // target access checked in assertTargetReadAllowed
        assertTargetReadAllowed(targetId);
        return actionRepository.exists(ActionSpecifications.byTargetIdAndIsActiveAndStatus(targetId, Action.Status.CANCELING));
    }

    @Override
    @Transactional
    public void cancelActionsForDistributionSet(final ActionCancellationType cancelationType, final DistributionSet distributionSet) {
        actionRepository.findAll(ActionSpecifications.byDistributionSetIdAndActiveAndStatusIsNot(distributionSet.getId(), Status.CANCELING))
                .forEach(action -> {
                    try {
                        assertTargetUpdateAllowed(action);
                        cancelAction0(action.getId());
                        log.debug("Action {} canceled", action.getId());
                    } catch (final InsufficientPermissionException e) {
                        log.trace("Could not cancel action {} due to insufficient permissions.", action.getId(), e);
                    } catch (final EntityNotFoundException e) {
                        log.trace("Could not cancel action {} due to entity not found exception.", action.getId(), e);
                    }
                });
        if (cancelationType == ActionCancellationType.FORCE) {
            actionRepository.findAll(ActionSpecifications.byDistributionSetIdAndActive(distributionSet.getId())).forEach(action -> {
                try {
                    assertTargetUpdateAllowed(action);
                    forceQuitAction0(action.getId());
                    log.debug("Action {} force canceled (force)", action.getId());
                } catch (final InsufficientPermissionException e) {
                    log.trace("Could not cancel action {} due to insufficient permissions.", action.getId(), e);
                } catch (final EntityNotFoundException e) {
                    log.trace("Could not cancel action {} due to entity not found exception.", action.getId(), e);
                }
            });
        }
    }

    public record MaxAssignmentsExceededInfo(long targetId, long requested, AssignmentQuotaExceededException quotaExceededException) {}

    @Override
    public void handleMaxAssignmentsExceeded(
            final Long targetId, final Long requested, final AssignmentQuotaExceededException quotaExceededException) {
        int actionsPurgePercentage = getActionsPurgePercentage();
        int quota = quotaManagement.getMaxActionsPerTarget();
        if (actionsPurgePercentage > 0 && actionsPurgePercentage < 100) {
            int numberOfActions = (int) ((actionsPurgePercentage / 100.0) * quota);
            if (requested > numberOfActions) {
                log.warn("Requested number of actions {} bigger than configured for deletion {}", requested, numberOfActions);
                throw quotaExceededException;
            }
            int totalTargetActions = Math.toIntExact(actionRepository.countByTargetId(targetId));
            if (totalTargetActions < quota) {
                numberOfActions = totalTargetActions - (quota - numberOfActions);
            }
            log.info("Actions purge percentage {}, will delete {} oldest actions for target {}",
                    actionsPurgePercentage, numberOfActions, targetId);
            deleteOldestTargetActions(targetId, numberOfActions);
        } else {
            throw quotaExceededException;
        }
    }

    /**
     * Deletes the first n target actions of a target
     *
     * @param targetId - target id
     * @param oldestToDelete - number of oldest actions to be deleted
     */
    private void deleteOldestTargetActions(long targetId, int oldestToDelete) {
        // Workaround for the case where JPQL or Criteria API do not support LIMIT
        log.info("Deleting last {} actions of target {}", oldestToDelete, targetId);
        final String SQL = "DELETE FROM sp_action WHERE id IN(" +
                "SELECT id FROM (" +
                "SELECT id FROM sp_action" +
                " WHERE target=" + Jpa.nativeQueryParamPrefix() + "target" +
                " ORDER BY id ASC" +
                " LIMIT " + oldestToDelete
                + ") AS sub"
                + ")";
        final Query query = entityManager.createNativeQuery(SQL);
        query.setParameter("target", targetId);
        query.executeUpdate();
    }

    private int getActionsPurgePercentage() {
        return TenantConfigHelper.getAsSystem(TenantConfigurationKey.ACTION_CLEANUP_ON_QUOTA_HIT_PERCENTAGE, Integer.class);
    }

    protected boolean isActionsAutocloseEnabled() {
        return TenantConfigHelper.getAsSystem(REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, Boolean.class);
    }

    private static Map<Long, List<TargetWithActionType>> convertRequest(final Collection<DeploymentRequest> deploymentRequests) {
        return deploymentRequests.stream().collect(Collectors.groupingBy(DeploymentRequest::getDistributionSetId,
                Collectors.mapping(DeploymentRequest::getTargetWithActionType, Collectors.toList())));
    }

    /**
     * split tIDs length into max entries in-statement because many database have
     * a constraint of max entries in in-statements e.g. Oracle with maximum 1000
     * elements, so we need to split the entries here and execute multiple
     * statements
     */
    private static List<List<Long>> getTargetEntitiesAsChunks(final List<JpaTarget> targetEntities) {
        return ListUtils.partition(targetEntities.stream().map(Target::getId).toList(), Constants.MAX_ENTRIES_IN_STATEMENT);
    }

    private static DistributionSetAssignmentResult buildAssignmentResult(
            final JpaDistributionSet distributionSet, final List<JpaAction> assignedActions, final int totalTargetsForAssignment) {
        final int alreadyAssignedTargetsCount = totalTargetsForAssignment - assignedActions.size();
        return new DistributionSetAssignmentResult(distributionSet, alreadyAssignedTargetsCount, assignedActions);
    }

    private static String getQueryForDeleteActionsByStatusAndLastModifiedBeforeString(final Database database) {
        return QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED.getOrDefault(database, QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED_DEFAULT);
    }

    private static RetryTemplate createRetryTemplate() {
        final RetryTemplate template = new RetryTemplate();

        final FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(Constants.TX_RT_DELAY);
        template.setBackOffPolicy(backOffPolicy);

        final SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                Constants.TX_RT_MAX, Collections.singletonMap(ConcurrencyFailureException.class, true));
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    private List<DistributionSetAssignmentResult> assignDistributionSets(
            final List<DeploymentRequest> deploymentRequests, final String actionMessage, final AbstractDsAssignmentStrategy strategy) {
        final List<DeploymentRequest> validatedRequests = validateAndFilterRequestForAssignments(deploymentRequests);
        final Map<Long, List<TargetWithActionType>> assignmentsByDsIds = convertRequest(validatedRequests);

        final List<DistributionSetAssignmentResult> results = assignmentsByDsIds.entrySet().stream()
                .map(entry -> assignDistributionSetToTargetsWithRetry(entry.getKey(), entry.getValue(), actionMessage, strategy))
                .toList();
        strategy.sendDeploymentEvents(results);
        return results;
    }

    private List<DeploymentRequest> validateAndFilterRequestForAssignments(List<DeploymentRequest> deploymentRequests) {
        if (deploymentRequests.isEmpty()) {
            return deploymentRequests;
        }

        deploymentRequests = deploymentRequests.stream().distinct().toList();
        checkForMultiAssignment(deploymentRequests);
        checkQuotaForAssignment(deploymentRequests);
        // validates READ access to deployment sets, throws exception if deployment set
        // is not accessible
        checkForTargetTypeCompatibility(deploymentRequests);
        // filters only targets that are updatable
        return filterByTargetUpdatable(deploymentRequests);
    }

    private void checkForMultiAssignment(final Collection<DeploymentRequest> deploymentRequests) {
        if (!isMultiAssignmentsEnabled()) {
            final long distinctTargetsInRequest = deploymentRequests.stream()
                    .map(request -> request.getTargetWithActionType().getControllerId()).distinct().count();
            if (distinctTargetsInRequest < deploymentRequests.size()) {
                throw new MultiAssignmentIsNotEnabledException();
            }
        }
    }

    private void checkQuotaForAssignment(final Collection<DeploymentRequest> deploymentRequests) {
        enforceMaxAssignmentsPerRequest(deploymentRequests.size());
        enforceMaxActionsPerTarget(deploymentRequests);
    }

    private void checkForTargetTypeCompatibility(final List<DeploymentRequest> deploymentRequests) {
        final List<String> controllerIds = deploymentRequests.stream().map(DeploymentRequest::getControllerId).distinct().toList();
        final List<Long> distSetIds = deploymentRequests.stream().map(DeploymentRequest::getDistributionSetId).distinct().toList();

        if (controllerIds.size() > 1 && distSetIds.size() > 1) {
            throw new IllegalStateException("Assigning multiple Distribution Sets to multiple Targets simultaneously is not allowed!");
        }

        if (distSetIds.size() == 1) {
            checkCompatibilityForSingleDsAssignment(distSetIds.iterator().next(), controllerIds);
        } else {
            checkCompatibilityForMultiDsAssignment(controllerIds.iterator().next(), distSetIds);
        }
    }

    private void checkCompatibilityForSingleDsAssignment(final Long distSetId, final List<String> controllerIds) {
        final DistributionSetType distSetType = distributionSetManagement.getValidAndComplete(distSetId).getType();
        final Set<String> incompatibleTargetTypes = ListUtils.partition(controllerIds, Constants.MAX_ENTRIES_IN_STATEMENT).stream()
                .map(ids -> targetRepository.findAll(TargetSpecifications.hasControllerIdIn(ids)
                        .and(TargetSpecifications.notCompatibleWithDistributionSetType(distSetType.getId())))).flatMap(List::stream)
                .map(Target::getTargetType).map(TargetType::getName).collect(Collectors.toSet());

        if (!incompatibleTargetTypes.isEmpty()) {
            throw new IncompatibleTargetTypeException(incompatibleTargetTypes, distSetType.getName());
        }
    }

    private void checkCompatibilityForMultiDsAssignment(final String controllerId, final List<Long> distSetIds) {
        final Target target = targetRepository.getByControllerId(controllerId);

        if (target.getTargetType() != null) {
            // we assume that list of assigned DS is less than
            // MAX_ENTRIES_IN_STATEMENT
            final Set<DistributionSetType> incompatibleDistSetTypes = distributionSetManagement.get(distSetIds).stream()
                    .map(DistributionSet::getType).collect(Collectors.toSet());
            incompatibleDistSetTypes.removeAll(target.getTargetType().getDistributionSetTypes());

            if (!incompatibleDistSetTypes.isEmpty()) {
                final Set<String> distSetTypeNames = incompatibleDistSetTypes.stream().map(DistributionSetType::getName)
                        .collect(Collectors.toSet());
                throw new IncompatibleTargetTypeException(target.getTargetType().getName(), distSetTypeNames);
            }
        }
    }

    private List<DeploymentRequest> filterByTargetUpdatable(final List<DeploymentRequest> deploymentRequests) {
        final List<String> controllerIds = deploymentRequests.stream().map(DeploymentRequest::getControllerId).distinct().toList();

        final List<String> found = targetRepository.findAll(AccessController.Operation.UPDATE,
                TargetSpecifications.hasControllerIdIn(controllerIds)).stream().map(JpaTarget::getControllerId).toList();
        if (found.size() != controllerIds.size()) {
            return deploymentRequests.stream().filter(deploymentRequest -> found.contains(deploymentRequest.getControllerId())).toList();
        }
        return deploymentRequests;
    }

    private DistributionSetAssignmentResult assignDistributionSetToTargetsWithRetry(
            final Long dsId, final Collection<TargetWithActionType> targetsWithActionType, final String actionMessage,
            final AbstractDsAssignmentStrategy assignmentStrategy) {
        return retryTemplate.execute(retryContext ->
                assignDistributionSetToTargets(dsId, targetsWithActionType, actionMessage, assignmentStrategy));
    }

    /**
     * method assigns the {@link DistributionSet} to all {@link Target}s by their
     * IDs with a specific {@link ActionType} and {@code forcetime}.
     * <p/>
     * In case the update was executed offline (i.e. not managed by hawkBit) the
     * handling differs my means that:<br/>
     * A. it ignores targets completely that are in
     * {@link TargetUpdateStatus#PENDING}.<br/>
     * B. it created completed actions.<br/>
     * C. sets both installed and assigned DS on the target and switches the status
     * to {@link TargetUpdateStatus#IN_SYNC} <br/>
     * D. does not send a {@link TargetAssignDistributionSetEvent}.<br/>
     *
     * @param dsId the ID of the distribution set to assign
     * @param targetsWithActionType a list of all targets and their action type
     * @param actionMessage an optional message to be written into the action status
     * @param assignmentStrategy the assignment strategy (online /offline)
     * @return the assignment result
     * @throws IncompleteDistributionSetException if mandatory {@link SoftwareModuleType} are not assigned as define by the
     *         {@link DistributionSetType}.
     */
    private DistributionSetAssignmentResult assignDistributionSetToTargets(
            final Long dsId, final Collection<TargetWithActionType> targetsWithActionType, final String actionMessage,
            final AbstractDsAssignmentStrategy assignmentStrategy) {
        final JpaDistributionSet dsValidAndComplete = distributionSetManagement.getValidAndComplete(dsId);
        final JpaDistributionSet distributionSet;
        if (distributionSetManagement.shouldLockImplicitly(dsValidAndComplete)) {
            // implicitly lock, for some reason no update happen if lock in same transaction
            distributionSet = DeploymentHelper.runInNewTransaction(
                    txManager, "lockDistributionSet-" + dsId,
                    status -> {
                        if (entityManager.contains(dsValidAndComplete)) {
                            return distributionSetManagement.lock(dsValidAndComplete);
                        } else {
                            return distributionSetManagement.lock(entityManager.merge(dsValidAndComplete));
                        }
                    });
        } else {
            distributionSet = dsValidAndComplete;
        }

        final List<String> providedTargetIds = targetsWithActionType.stream().map(TargetWithActionType::getControllerId).distinct().toList();

        final List<String> existingTargetIds = ListUtils.partition(providedTargetIds, Constants.MAX_ENTRIES_IN_STATEMENT).stream()
                .map(ids -> targetRepository.findAll(AccessController.Operation.UPDATE, TargetSpecifications.hasControllerIdIn(ids)))
                .flatMap(List::stream).map(JpaTarget::getControllerId).toList();

        final List<JpaTarget> targetEntities = assignmentStrategy.findTargetsForAssignment(existingTargetIds, distributionSet.getId());
        if (targetEntities.isEmpty()) {
            return allTargetsAlreadyAssignedResult(distributionSet, existingTargetIds.size());
        }

        final List<TargetWithActionType> existingTargetsWithActionType = targetsWithActionType.stream()
                .filter(target -> existingTargetIds.contains(target.getControllerId())).toList();

        final List<JpaAction> assignedActions = doAssignDistributionSetToTargets(
                existingTargetsWithActionType, actionMessage, assignmentStrategy, distributionSet, targetEntities);
        return buildAssignmentResult(distributionSet, assignedActions, existingTargetsWithActionType.size());
    }

    private DistributionSetAssignmentResult allTargetsAlreadyAssignedResult(final JpaDistributionSet distributionSetEntity,
            final int alreadyAssignedCount) {
        // detaching as it is not necessary to persist the set itself
        entityManager.detach(distributionSetEntity);
        // return with nothing as all targets had the DS already assigned
        return new DistributionSetAssignmentResult(distributionSetEntity, alreadyAssignedCount, Collections.emptyList());
    }

    private List<JpaAction> doAssignDistributionSetToTargets(
            final Collection<TargetWithActionType> targetsWithActionType, final String actionMessage,
            final AbstractDsAssignmentStrategy assignmentStrategy, final JpaDistributionSet distributionSetEntity,
            final List<JpaTarget> targetEntities) {
        final List<List<Long>> targetEntitiesIdsChunks = getTargetEntitiesAsChunks(targetEntities);

        if (!isMultiAssignmentsEnabled()) {
            closeOrCancelActiveActions(assignmentStrategy, targetEntitiesIdsChunks);
        }
        // cancel all scheduled actions which are in-active, these actions were
        // not active before and the manual assignment which has been done cancels them
        targetEntitiesIdsChunks.forEach(this::cancelInactiveScheduledActionsForTargets);
        setAssignedDistributionSetAndTargetUpdateStatus(assignmentStrategy, distributionSetEntity, targetEntitiesIdsChunks);
        final Map<TargetWithActionType, JpaAction> assignedActions =
                createActions(targetsWithActionType, targetEntities, distributionSetEntity, assignmentStrategy);
        // create initial action status when action is created, so we remember
        // the initial running status because we will change the status
        // of the action itself and with this action status we have a nicer action history.
        createActionsStatus(assignedActions, assignmentStrategy, actionMessage);

        detachEntitiesAndSendTargetUpdatedEvents(distributionSetEntity, targetEntities, assignmentStrategy);
        return new ArrayList<>(assignedActions.values());
    }

    private void enforceMaxAssignmentsPerRequest(final int requestedActions) {
        QuotaHelper.assertAssignmentRequestSizeQuota(requestedActions,
                quotaManagement.getMaxTargetDistributionSetAssignmentsPerManualAssignment());
    }

    private void enforceMaxActionsPerTarget(final Collection<DeploymentRequest> deploymentRequests) {
        final Map<String, Long> countOfTargetInRequest = deploymentRequests.stream().map(DeploymentRequest::getControllerId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        countOfTargetInRequest.forEach(this::checkMaxAssignmentQuota);
    }

    private void checkMaxAssignmentQuota(final String controllerId, final long requested) {
        final int quota = quotaManagement.getMaxActionsPerTarget();
        try {
            AccessContext.asSystem(() -> QuotaHelper.assertAssignmentQuota(
                    controllerId, requested, quota, Action.class, Target.class, actionRepository::countByTargetControllerId));
        } catch (final AssignmentQuotaExceededException ex) {
            targetRepository.findByControllerId(controllerId).ifPresentOrElse(
                    // assume requested are always smaller than int size
                    target -> handleMaxAssignmentsExceeded(target.getId(), requested, ex),
                    () -> {
                        throw new EntityNotFoundException(Target.class, controllerId);
                    });
        }
    }

    private void closeOrCancelActiveActions(final AbstractDsAssignmentStrategy assignmentStrategy, final List<List<Long>> targetIdsChunks) {
        if (isActionsAutocloseEnabled()) {
            assignmentStrategy.closeActiveActions(targetIdsChunks);
        } else {
            assignmentStrategy.cancelActiveActions(targetIdsChunks);
        }
    }

    private void setAssignedDistributionSetAndTargetUpdateStatus(
            final AbstractDsAssignmentStrategy assignmentStrategy,
            final JpaDistributionSet set, final List<List<Long>> targetIdsChunks) {
        assignmentStrategy.setAssignedDistributionSetAndTargetStatus(set, targetIdsChunks);
    }

    private Map<TargetWithActionType, JpaAction> createActions(
            final Collection<TargetWithActionType> targetsWithActionType,
            final List<JpaTarget> targets, final JpaDistributionSet set, final AbstractDsAssignmentStrategy assignmentStrategy) {
        final Map<TargetWithActionType, JpaAction> persistedActions = new LinkedHashMap<>();
        for (final TargetWithActionType twt : targetsWithActionType) {
            final JpaAction targetAction = assignmentStrategy.createTargetAction(twt, targets, set);
            if (targetAction != null) {
                persistedActions.put(twt, actionRepository.save(targetAction));
            }
        }
        return persistedActions;
    }

    private void createActionsStatus(
            final Map<TargetWithActionType, JpaAction> actions, final AbstractDsAssignmentStrategy assignmentStrategy,
            final String actionMessage) {
        actionStatusRepository.saveAll(actions.entrySet().stream().map(entry -> {
            final JpaAction action = entry.getValue();
            final JpaActionStatus actionStatus = assignmentStrategy.createActionStatus(action, actionMessage);
            verifyAndAddConfirmationStatus(action, actionStatus, entry.getKey().isConfirmationRequired());
            return actionStatus;
        }).toList());
    }

    private void setInitialActionStatusOfRolloutGroup(final List<JpaAction> actions) {
        final List<JpaActionStatus> statusList = new ArrayList<>();
        for (final JpaAction action : actions) {
            final JpaActionStatus actionStatus = onlineDsAssignmentStrategy.createActionStatus(action, null);
            verifyAndAddConfirmationStatus(action, actionStatus, action.getRolloutGroup().isConfirmationRequired());
            statusList.add(actionStatus);
        }
        actionStatusRepository.saveAll(statusList);
    }

    private void verifyAndAddConfirmationStatus(final JpaAction action, final JpaActionStatus actionStatus,
            final boolean isConfirmationRequired) {
        if (actionStatus.getStatus() == Status.WAIT_FOR_CONFIRMATION) {
            if (action.getStatus().equals(Status.RUNNING)) {
                // action is in RUNNING state only if it's confirmed during
                // assignment already
                if (!isConfirmationRequired) {
                    // confirmation given on assignment dialog
                    actionStatus.addMessage(String.format("Assignment confirmed by initiator [%s].", action.getInitiatedBy()));
                } else if (action.getTarget().getAutoConfirmationStatus() != null) {
                    // auto-confirmation is configured
                    actionStatus.addMessage(action.getTarget().getAutoConfirmationStatus().constructActionMessage());
                } else {
                    throw new IllegalStateException("Action in RUNNING state without given confirmation.");
                }

            } else {
                actionStatus.addMessage("Waiting for the confirmation by the device before processing with the deployment");
            }
        }
    }

    private void detachEntitiesAndSendTargetUpdatedEvents(final JpaDistributionSet set, final List<JpaTarget> targets,
            final AbstractDsAssignmentStrategy assignmentStrategy) {
        // detaching as it is not necessary to persist the set itself
        entityManager.detach(set);
        // detaching as the entity has been updated by the JPQL query above
        targets.forEach(entityManager::detach);
        assignmentStrategy.sendTargetUpdatedEvents(set, targets);
    }

    private JpaAction closeActionIfSetWasAlreadyAssigned(final JpaAction action) {
        if (isMultiAssignmentsEnabled()) {
            return action;
        }

        final JpaTarget target = action.getTarget();
        if (target.getAssignedDistributionSet() != null && action.getDistributionSet().getId()
                .equals(target.getAssignedDistributionSet().getId())) {
            // the target has already the distribution set assigned, we don't need to start the scheduled action, just finish it.
            log.debug("Target {} has distribution set {} assigned. Closing action...",
                    target.getControllerId(), action.getDistributionSet().getName());
            action.setStatus(Status.FINISHED);
            action.setActive(false);
            setSkipActionStatus(action);
            actionRepository.save(action);
            return null;
        }

        return action;
    }

    private List<Action> startScheduledActionsAndHandleOpenCancellationFirst(final List<JpaAction> actions) {
        if (!isMultiAssignmentsEnabled()) {
            closeOrCancelOpenDeviceActions(actions);
        }
        final List<JpaAction> savedActions = activateActionsOfRolloutGroup(actions);
        setInitialActionStatusOfRolloutGroup(savedActions);
        setAssignmentOnTargets(savedActions);
        return Collections.unmodifiableList(savedActions);
    }

    private void closeOrCancelOpenDeviceActions(final List<JpaAction> actions) {
        final List<Long> targetIds = actions.stream().map(JpaAction::getTarget).map(Target::getId).toList();
        if (isActionsAutocloseEnabled()) {
            onlineDsAssignmentStrategy.closeObsoleteUpdateActions(targetIds);
        } else {
            onlineDsAssignmentStrategy.overrideObsoleteUpdateActions(targetIds);
        }
    }

    private List<JpaAction> activateActionsOfRolloutGroup(final List<JpaAction> actions) {
        actions.forEach(action -> {
            action.setActive(true);
            final boolean confirmationRequired = action.getRolloutGroup().isConfirmationRequired() &&
                    action.getTarget().getAutoConfirmationStatus() == null;
            if (isConfirmationFlowEnabled() && confirmationRequired) {
                action.setStatus(Status.WAIT_FOR_CONFIRMATION);
                return;
            }
            action.setStatus(Status.RUNNING);
        });
        return actionRepository.saveAll(actions);
    }

    private void setAssignmentOnTargets(final List<JpaAction> actions) {
        final List<JpaTarget> assignedDsTargets = actions.stream().map(savedAction -> {
            final JpaTarget mergedTarget = entityManager.merge(savedAction.getTarget());
            mergedTarget.setAssignedDistributionSet(savedAction.getDistributionSet());
            mergedTarget.setUpdateStatus(TargetUpdateStatus.PENDING);
            return mergedTarget;
        }).toList();

        targetRepository.saveAll(assignedDsTargets);
    }

    private void setSkipActionStatus(final JpaAction action) {
        final JpaActionStatus actionStatus = new JpaActionStatus();
        actionStatus.setAction(action);
        actionStatus.setTimestamp(action.getCreatedAt());
        actionStatus.setStatus(Status.RUNNING);
        actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Distribution Set is already assigned. Skipping this action.");
        actionStatusRepository.save(actionStatus);
    }

    private boolean isMultiAssignmentsEnabled() {
        return TenantConfigHelper.isMultiAssignmentsEnabled();
    }

    private boolean isConfirmationFlowEnabled() {
        return TenantConfigHelper.isConfirmationFlowEnabled();
    }

    private void assertTargetReadAllowed(final Long targetId) {
        if (!targetRepository.existsById(targetId)) {
            throw new EntityNotFoundException(Target.class, targetId);
        }
    }

    private void assertTargetReadAllowed(final String controllerId) {
        if (!targetRepository.exists(TargetSpecifications.hasControllerId(controllerId))) {
            throw new EntityNotFoundException(Target.class, controllerId);
        }
    }

    private JpaAction assertTargetUpdateAllowed(final JpaAction action) {
        targetRepository.findOne(TargetSpecifications.hasId(action.getTarget().getId())).ifPresentOrElse(
                target -> targetRepository.getAccessController()
                        .ifPresent(acm -> acm.assertOperationAllowed(AccessController.Operation.UPDATE, target)), () -> {
                    throw new EntityNotFoundException(Action.class, action);
                });
        return action;
    }

    private void assertActionExistsAndAccessible(final long actionId) {
        if (actionRepository.findById(actionId).filter(action -> {
            assertTargetReadAllowed(action.getTarget().getId());
            return true;
        }).isEmpty()) {
            throw new EntityNotFoundException(Action.class, actionId);
        }
    }
}