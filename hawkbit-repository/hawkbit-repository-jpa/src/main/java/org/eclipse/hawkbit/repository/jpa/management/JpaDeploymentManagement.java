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

import java.io.Serializable;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Root;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ForceQuitActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.IncompatibleTargetTypeException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.MultiAssignmentIsNotEnabledException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.ActionSpecifications;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.jpa.utils.WeightValidationHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation for {@link DeploymentManagement}.
 */
@Slf4j
@Transactional(readOnly = true)
@Validated
public class JpaDeploymentManagement extends JpaActionManagement implements DeploymentManagement {

    /**
     * Maximum amount of Actions that are started at once.
     */
    private static final int ACTION_PAGE_LIMIT = 1000;

    private static final String QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED_DEFAULT = "DELETE FROM sp_action WHERE tenant=#tenant AND status IN (%s) AND last_modified_at<#last_modified_at LIMIT "
            + ACTION_PAGE_LIMIT;

    private static final EnumMap<Database, String> QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED;

    static {
        QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED = new EnumMap<>(Database.class);
        QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED.put(Database.SQL_SERVER, "DELETE TOP (" + ACTION_PAGE_LIMIT
                + ") FROM sp_action WHERE tenant=#tenant AND status IN (%s) AND last_modified_at<#last_modified_at ");
        QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED.put(Database.POSTGRESQL,
                "DELETE FROM sp_action WHERE id IN (SELECT id FROM sp_action WHERE tenant=#tenant AND status IN (%s) AND last_modified_at<#last_modified_at LIMIT "
                        + ACTION_PAGE_LIMIT + ")");
    }

    private final EntityManager entityManager;
    private final DistributionSetManagement distributionSetManagement;
    private final TargetRepository targetRepository;
    private final AuditorAware<String> auditorProvider;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final PlatformTransactionManager txManager;
    private final OnlineDsAssignmentStrategy onlineDsAssignmentStrategy;
    private final OfflineDsAssignmentStrategy offlineDsAssignmentStrategy;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final TenantAware tenantAware;
    private final AuditorAware<String> auditorAware;
    private final Database database;
    private final RetryTemplate retryTemplate;

    public JpaDeploymentManagement(final EntityManager entityManager, final ActionRepository actionRepository,
            final DistributionSetManagement distributionSetManagement, final TargetRepository targetRepository,
            final ActionStatusRepository actionStatusRepository, final AuditorAware<String> auditorProvider,
            final EventPublisherHolder eventPublisherHolder, final AfterTransactionCommitExecutor afterCommit,
            final VirtualPropertyReplacer virtualPropertyReplacer, final PlatformTransactionManager txManager,
            final TenantConfigurationManagement tenantConfigurationManagement, final QuotaManagement quotaManagement,
            final SystemSecurityContext systemSecurityContext, final TenantAware tenantAware, final AuditorAware<String> auditorAware,
            final Database database, final RepositoryProperties repositoryProperties) {
        super(actionRepository, actionStatusRepository, quotaManagement, repositoryProperties);
        this.entityManager = entityManager;
        this.distributionSetManagement = distributionSetManagement;
        this.targetRepository = targetRepository;
        this.auditorProvider = auditorProvider;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.txManager = txManager;
        onlineDsAssignmentStrategy = new OnlineDsAssignmentStrategy(targetRepository, afterCommit, eventPublisherHolder,
                actionRepository, actionStatusRepository, quotaManagement, this::isMultiAssignmentsEnabled,
                this::isConfirmationFlowEnabled, repositoryProperties);
        offlineDsAssignmentStrategy = new OfflineDsAssignmentStrategy(targetRepository, afterCommit,
                eventPublisherHolder, actionRepository, actionStatusRepository, quotaManagement,
                this::isMultiAssignmentsEnabled, this::isConfirmationFlowEnabled, repositoryProperties);
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.tenantAware = tenantAware;
        this.auditorAware = auditorAware;
        this.database = database;
        this.retryTemplate = createRetryTemplate();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DistributionSetAssignmentResult> offlineAssignedDistributionSets(
            final Collection<Entry<String, Long>> assignments) {
            return offlineAssignedDistributionSets(assignments,tenantAware.getCurrentUsername());
    }

    @Override
    public List<DistributionSetAssignmentResult> offlineAssignedDistributionSets(
            Collection<Entry<String, Long>> assignments, String initiatedBy) {
        final Collection<Entry<String, Long>> distinctAssignments = assignments.stream().distinct().toList();
        enforceMaxAssignmentsPerRequest(distinctAssignments.size());

        final List<DeploymentRequest> deploymentRequests = distinctAssignments.stream()
                .map(entry -> DeploymentManagement.deploymentRequest(entry.getKey(), entry.getValue()).build())
                .toList();

        return assignDistributionSets(
                auditorAware.getCurrentAuditor().orElse(tenantAware.getCurrentUsername()),
                deploymentRequests, null, offlineDsAssignmentStrategy);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DistributionSetAssignmentResult> assignDistributionSets(
            final List<DeploymentRequest> deploymentRequests) {
        return assignDistributionSets(tenantAware.getCurrentUsername(), deploymentRequests, null);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DistributionSetAssignmentResult> assignDistributionSets(final String initiatedBy,
            final List<DeploymentRequest> deploymentRequests, final String actionMessage) {
        WeightValidationHelper.usingContext(systemSecurityContext, tenantConfigurationManagement)
                .validate(deploymentRequests);
        return assignDistributionSets(initiatedBy, deploymentRequests, actionMessage, onlineDsAssignmentStrategy);
    }

    private List<DistributionSetAssignmentResult> assignDistributionSets(final String initiatedBy,
            final List<DeploymentRequest> deploymentRequests, final String actionMessage,
            final AbstractDsAssignmentStrategy strategy) {
        final List<DeploymentRequest> validatedRequests = validateAndFilterRequestForAssignments(deploymentRequests);
        final Map<Long, List<TargetWithActionType>> assignmentsByDsIds = convertRequest(validatedRequests);

        final List<DistributionSetAssignmentResult> results = assignmentsByDsIds.entrySet().stream()
                .map(entry -> assignDistributionSetToTargetsWithRetry(initiatedBy, entry.getKey(), entry.getValue(),
                        actionMessage, strategy))
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
        final List<String> controllerIds = deploymentRequests.stream().map(DeploymentRequest::getControllerId)
                .distinct().toList();
        final List<Long> distSetIds = deploymentRequests.stream().map(DeploymentRequest::getDistributionSetId)
                .distinct().toList();

        if (controllerIds.size() > 1 && distSetIds.size() > 1) {
            throw new IllegalStateException(
                    "Assigning multiple Distribution Sets to multiple Targets simultaneously is not allowed!");
        }

        if (distSetIds.size() == 1) {
            checkCompatibilityForSingleDsAssignment(distSetIds.iterator().next(), controllerIds);
        } else {
            checkCompatibilityForMultiDsAssignment(controllerIds.iterator().next(), distSetIds);
        }
    }

    private void checkCompatibilityForSingleDsAssignment(final Long distSetId, final List<String> controllerIds) {
        final DistributionSetType distSetType = distributionSetManagement.getValidAndComplete(distSetId).getType();
        final Set<String> incompatibleTargetTypes = ListUtils.partition(controllerIds, Constants.MAX_ENTRIES_IN_STATEMENT)
                .stream()
                .map(ids -> targetRepository.findAll(TargetSpecifications.hasControllerIdIn(ids)
                        .and(TargetSpecifications.notCompatibleWithDistributionSetType(distSetType.getId()))))
                .flatMap(List::stream).map(Target::getTargetType).map(TargetType::getName).collect(Collectors.toSet());

        if (!incompatibleTargetTypes.isEmpty()) {
            throw new IncompatibleTargetTypeException(incompatibleTargetTypes, distSetType.getName());
        }
    }

    private void checkCompatibilityForMultiDsAssignment(final String controllerId, final List<Long> distSetIds) {
        final Target target = targetRepository.findOne(TargetSpecifications.hasControllerId(controllerId))
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));

        if (target.getTargetType() != null) {
            // we assume that list of assigned DS is less than
            // MAX_ENTRIES_IN_STATEMENT
            final Set<DistributionSetType> incompatibleDistSetTypes = distributionSetManagement.get(distSetIds).stream()
                    .map(DistributionSet::getType).collect(Collectors.toSet());
            incompatibleDistSetTypes.removeAll(target.getTargetType().getCompatibleDistributionSetTypes());

            if (!incompatibleDistSetTypes.isEmpty()) {
                final Set<String> distSetTypeNames = incompatibleDistSetTypes.stream().map(DistributionSetType::getName)
                        .collect(Collectors.toSet());
                throw new IncompatibleTargetTypeException(target.getTargetType().getName(), distSetTypeNames);
            }
        }
    }

    private List<DeploymentRequest> filterByTargetUpdatable(final List<DeploymentRequest> deploymentRequests) {
        final List<String> controllerIds = deploymentRequests.stream().map(DeploymentRequest::getControllerId)
                .distinct().toList();

        final List<String> found = targetRepository
                .findAll(AccessController.Operation.UPDATE, TargetSpecifications.hasControllerIdIn(controllerIds))
                .stream().map(JpaTarget::getControllerId).toList();
        if (found.size() != controllerIds.size()) {
            return deploymentRequests.stream()
                    .filter(deploymentRequest -> found.contains(deploymentRequest.getControllerId())).toList();
        }
        return deploymentRequests;
    }

    private static Map<Long, List<TargetWithActionType>> convertRequest(
            final Collection<DeploymentRequest> deploymentRequests) {
        return deploymentRequests.stream().collect(Collectors.groupingBy(DeploymentRequest::getDistributionSetId,
                Collectors.mapping(DeploymentRequest::getTargetWithActionType, Collectors.toList())));
    }

    private DistributionSetAssignmentResult assignDistributionSetToTargetsWithRetry(final String initiatedBy,
            final Long dsId, final Collection<TargetWithActionType> targetsWithActionType, final String actionMessage,
            final AbstractDsAssignmentStrategy assignmentStrategy) {
        final RetryCallback<DistributionSetAssignmentResult, ConcurrencyFailureException> retryCallback =
                retryContext -> assignDistributionSetToTargets(
                        initiatedBy, dsId, targetsWithActionType, actionMessage, assignmentStrategy);
        return retryTemplate.execute(retryCallback);
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
     * @param initiatedBy
     *            the username of the user who initiated the assignment
     * @param dsId
     *            the ID of the distribution set to assign
     * @param targetsWithActionType
     *            a list of all targets and their action type
     * @param actionMessage
     *            an optional message to be written into the action status
     * @param assignmentStrategy
     *            the assignment strategy (online /offline)
     * @return the assignment result
     *
     * @throws IncompleteDistributionSetException
     *             if mandatory {@link SoftwareModuleType} are not assigned as
     *             define by the {@link DistributionSetType}.
     */
    private DistributionSetAssignmentResult assignDistributionSetToTargets(final String initiatedBy, final Long dsId,
            final Collection<TargetWithActionType> targetsWithActionType, final String actionMessage,
            final AbstractDsAssignmentStrategy assignmentStrategy) {
        final JpaDistributionSet distributionSet =
                (JpaDistributionSet) distributionSetManagement.getValidAndComplete(dsId);

        if (((JpaDistributionSetManagement)distributionSetManagement).isImplicitLockApplicable(distributionSet)) {
            // without new transaction DS changed event is not thrown
            DeploymentHelper.runInNewTransaction(txManager, "Implicit lock", status -> {
                distributionSetManagement.lock(distributionSet.getId());
                return null;
            });
        }

        final List<String> providedTargetIds = targetsWithActionType.stream().map(TargetWithActionType::getControllerId)
                .distinct().toList();

        final List<String> existingTargetIds = ListUtils.partition(providedTargetIds, Constants.MAX_ENTRIES_IN_STATEMENT)
                .stream()
                .map(ids -> targetRepository.findAll(AccessController.Operation.UPDATE,
                        TargetSpecifications.hasControllerIdIn(ids)))
                .flatMap(List::stream).map(JpaTarget::getControllerId).toList();

        final List<JpaTarget> targetEntities = assignmentStrategy.findTargetsForAssignment(existingTargetIds,
                distributionSet.getId());

        if (targetEntities.isEmpty()) {
            return allTargetsAlreadyAssignedResult(distributionSet, existingTargetIds.size());
        }

        final List<TargetWithActionType> existingTargetsWithActionType = targetsWithActionType.stream()
                .filter(target -> existingTargetIds.contains(target.getControllerId())).toList();

        final List<JpaAction> assignedActions = doAssignDistributionSetToTargets(initiatedBy,
                existingTargetsWithActionType, actionMessage, assignmentStrategy, distributionSet,
                targetEntities);
        return buildAssignmentResult(distributionSet, assignedActions, existingTargetsWithActionType.size());
    }

    private DistributionSetAssignmentResult allTargetsAlreadyAssignedResult(
            final JpaDistributionSet distributionSetEntity, final int alreadyAssignedCount) {
        // detaching as it is not necessary to persist the set itself
        entityManager.detach(distributionSetEntity);
        // return with nothing as all targets had the DS already assigned
        return new DistributionSetAssignmentResult(distributionSetEntity, alreadyAssignedCount,
                Collections.emptyList());
    }

    private List<JpaAction> doAssignDistributionSetToTargets(final String initiatedBy,
            final Collection<TargetWithActionType> targetsWithActionType, final String actionMessage,
            final AbstractDsAssignmentStrategy assignmentStrategy, final JpaDistributionSet distributionSetEntity,
            final List<JpaTarget> targetEntities) {
        final List<List<Long>> targetEntitiesIdsChunks = getTargetEntitiesAsChunks(targetEntities);

        if (!isMultiAssignmentsEnabled()) {
            closeOrCancelActiveActions(assignmentStrategy, targetEntitiesIdsChunks);
        }
        // cancel all scheduled actions which are in-active, these actions were
        // not active before and the manual assignment which has been done
        // cancels them
        targetEntitiesIdsChunks.forEach(this::cancelInactiveScheduledActionsForTargets);
        setAssignedDistributionSetAndTargetUpdateStatus(assignmentStrategy, distributionSetEntity,
                targetEntitiesIdsChunks);
        final Map<TargetWithActionType, JpaAction> assignedActions = createActions(initiatedBy, targetsWithActionType,
                targetEntities, assignmentStrategy, distributionSetEntity);
        // create initial action status when action is created so we remember
        // the initial running status because we will change the status
        // of the action itself and with this action status we have a nicer
        // action history.
        createActionsStatus(assignedActions, assignmentStrategy, actionMessage);

        detachEntitiesAndSendTargetUpdatedEvents(distributionSetEntity, targetEntities, assignmentStrategy);
        return new ArrayList<>(assignedActions.values());
    }

    /**
     * split tIDs length into max entries in-statement because many database have
     * constraint of max entries in in-statements e.g. Oracle with maximum 1000
     * elements, so we need to split the entries here and execute multiple
     * statements
     */
    private static List<List<Long>> getTargetEntitiesAsChunks(final List<JpaTarget> targetEntities) {
        return ListUtils.partition(targetEntities.stream().map(Target::getId).collect(Collectors.toList()),
                Constants.MAX_ENTRIES_IN_STATEMENT);
    }

    private static DistributionSetAssignmentResult buildAssignmentResult(final JpaDistributionSet distributionSet,
            final List<JpaAction> assignedActions, final int totalTargetsForAssignment) {
        final int alreadyAssignedTargetsCount = totalTargetsForAssignment - assignedActions.size();

        return new DistributionSetAssignmentResult(distributionSet, alreadyAssignedTargetsCount, assignedActions);
    }

    private void enforceMaxAssignmentsPerRequest(final int requestedActions) {
        QuotaHelper.assertAssignmentRequestSizeQuota(requestedActions,
                quotaManagement.getMaxTargetDistributionSetAssignmentsPerManualAssignment());
    }

    private void enforceMaxActionsPerTarget(final Collection<DeploymentRequest> deploymentRequests) {
        final int quota = quotaManagement.getMaxActionsPerTarget();

        final Map<String, Long> countOfTargetInRequest = deploymentRequests.stream()
                .map(DeploymentRequest::getControllerId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        countOfTargetInRequest.forEach((controllerId, count) -> QuotaHelper.assertAssignmentQuota(controllerId, count,
                quota, Action.class, Target.class, actionRepository::countByTargetControllerId));
    }

    private void closeOrCancelActiveActions(final AbstractDsAssignmentStrategy assignmentStrategy,
            final List<List<Long>> targetIdsChunks) {
        if (isActionsAutocloseEnabled()) {
            assignmentStrategy.closeActiveActions(targetIdsChunks);
        } else {
            assignmentStrategy.cancelActiveActions(targetIdsChunks);
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void cancelInactiveScheduledActionsForTargets(final List<Long> targetIds) {
        if (!isMultiAssignmentsEnabled()) {
            targetRepository.getAccessController().ifPresent(v -> {
                if (targetRepository.count(AccessController.Operation.UPDATE,
                        TargetSpecifications.hasIdIn(targetIds)) != targetIds.size()) {
                    throw new EntityNotFoundException(Target.class, targetIds);
                }
            });
            actionRepository.switchStatus(Status.CANCELED, targetIds, false, Status.SCHEDULED);
        } else {
            log.debug("The Multi Assignments feature is enabled: No need to cancel inactive scheduled actions.");
        }
    }

    private void setAssignedDistributionSetAndTargetUpdateStatus(final AbstractDsAssignmentStrategy assignmentStrategy,
            final JpaDistributionSet set, final List<List<Long>> targetIdsChunks) {
        final String currentUser = auditorProvider.getCurrentAuditor().orElse(null);
        assignmentStrategy.setAssignedDistributionSetAndTargetStatus(set, targetIdsChunks, currentUser);
    }

    private Map<TargetWithActionType, JpaAction> createActions(final String initiatedBy,
            final Collection<TargetWithActionType> targetsWithActionType, final List<JpaTarget> targets,
            final AbstractDsAssignmentStrategy assignmentStrategy, final JpaDistributionSet set) {

        final Map<TargetWithActionType, JpaAction> persistedActions = new LinkedHashMap<>();

        for (final TargetWithActionType twt : targetsWithActionType) {
            final JpaAction targetAction = assignmentStrategy.createTargetAction(initiatedBy, twt, targets, set);
            if (targetAction != null) {
                persistedActions.put(twt, actionRepository.save(targetAction));
            }
        }
        return persistedActions;
    }

    private void createActionsStatus(final Map<TargetWithActionType, JpaAction> actions,
            final AbstractDsAssignmentStrategy assignmentStrategy, final String actionMessage) {
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
                    actionStatus.addMessage(
                            String.format("Assignment confirmed by initiator [%s].", action.getInitiatedBy()));
                } else if (action.getTarget().getAutoConfirmationStatus() != null) {
                    // auto-confirmation is configured
                    actionStatus.addMessage(action.getTarget().getAutoConfirmationStatus().constructActionMessage());
                } else {
                    throw new IllegalStateException("Action in RUNNING state without given confirmation.");
                }

            } else {
                actionStatus
                        .addMessage("Waiting for the confirmation by the device before processing with the deployment");
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

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action cancelAction(final long actionId) {
        log.debug("cancelAction({})", actionId);

        final JpaAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (action.isCancelingOrCanceled()) {
            throw new CancelActionNotAllowedException("Actions in canceling or canceled state cannot be canceled");
        }

        assertTargetUpdateAllowed(action);

        if (action.isActive()) {
            log.debug("action ({}) was still active. Change to {}.", action, Status.CANCELING);
            action.setStatus(Status.CANCELING);

            // document that the status has been retrieved
            actionStatusRepository.save(new JpaActionStatus(action, Status.CANCELING, System.currentTimeMillis(),
                    RepositoryConstants.SERVER_MESSAGE_PREFIX + "manual cancelation requested"));
            final Action saveAction = actionRepository.save(action);

            onlineDsAssignmentStrategy.cancelAssignment(action);

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

        assertTargetUpdateAllowed(action);

        log.warn("action ({}) was still active and has been force quite.", action);

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
            lastStartedActionsCount = DeploymentHelper.runInNewTransaction(
                    txManager,
                    "startScheduledActions-" + rolloutId,
                    status -> {
                        final Page<Action> rolloutGroupActions = findActionsByRolloutAndRolloutGroupParent(
                                rolloutId, rolloutGroupParentId, ACTION_PAGE_LIMIT);

                        if (rolloutGroupActions.getContent().isEmpty()) {
                            return 0L;
                        }

                        // self invocation won't check @PreAuthorize but it is already checked for the method
                        startScheduledActions(rolloutGroupActions.getContent());

                        return rolloutGroupActions.getTotalElements();
                    });
            totalActionsCount += lastStartedActionsCount;
        } while (lastStartedActionsCount > 0);

        return totalActionsCount;
    }


    @Override
    public void startScheduledActions(final List<Action> rolloutGroupActions) {
        // Close actions already assigned and collect pending assignments
        final List<JpaAction> pendingTargetAssignments = rolloutGroupActions.stream()
                .map(JpaAction.class::cast).map(this::closeActionIfSetWasAlreadyAssigned).filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (pendingTargetAssignments.isEmpty()) {
            return;
        }
        // check if old actions needs to be canceled first
        final List<Action> newTargetAssignments = startScheduledActionsAndHandleOpenCancellationFirst(pendingTargetAssignments);
        if (!newTargetAssignments.isEmpty()) {
            onlineDsAssignmentStrategy.sendDeploymentEvents(newTargetAssignments.get(0).getDistributionSet().getId(), newTargetAssignments);
        }
    }

    private Page<Action> findActionsByRolloutAndRolloutGroupParent(final Long rolloutId,
            final Long rolloutGroupParentId, final int limit) {

        final PageRequest pageRequest = PageRequest.of(0, limit);
        if (rolloutGroupParentId == null) {
            return actionRepository.findByRolloutIdAndRolloutGroupParentIsNullAndStatus(pageRequest, rolloutId,
                    Action.Status.SCHEDULED);
        } else {
            return actionRepository.findByRolloutIdAndRolloutGroupParentIdAndStatus(pageRequest, rolloutId,
                    rolloutGroupParentId, Action.Status.SCHEDULED);
        }
    }

    private JpaAction closeActionIfSetWasAlreadyAssigned(final JpaAction action) {

        if (isMultiAssignmentsEnabled()) {
            return action;
        }

        final JpaTarget target = (JpaTarget) action.getTarget();
        if (target.getAssignedDistributionSet() != null
                && action.getDistributionSet().getId().equals(target.getAssignedDistributionSet().getId())) {
            // the target has already the distribution set assigned, we don't
            // need to start the scheduled action, just finish it.
            log.debug("Target {} has distribution set {} assigned. Closing action...", target.getControllerId(),
                    action.getDistributionSet().getName());
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
        final List<Long> targetIds = actions.stream().map(JpaAction::getTarget).map(Target::getId)
                .collect(Collectors.toList());
        if (isActionsAutocloseEnabled()) {
            onlineDsAssignmentStrategy.closeObsoleteUpdateActions(targetIds);
        } else {
            onlineDsAssignmentStrategy.overrideObsoleteUpdateActions(targetIds);
        }
    }

    private List<JpaAction> activateActionsOfRolloutGroup(final List<JpaAction> actions) {
        actions.forEach(action -> {
            action.setActive(true);
            final boolean confirmationRequired = action.getRolloutGroup().isConfirmationRequired()
                    && action.getTarget().getAutoConfirmationStatus() == null;
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
            final JpaTarget mergedTarget = (JpaTarget) entityManager.merge(savedAction.getTarget());
            mergedTarget.setAssignedDistributionSet(savedAction.getDistributionSet());
            mergedTarget.setUpdateStatus(TargetUpdateStatus.PENDING);
            return mergedTarget;
        }).collect(Collectors.toList());

        targetRepository.saveAll(assignedDsTargets);
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
        return actionRepository.findById(actionId)
                .filter(action -> targetRepository.exists(TargetSpecifications.hasId(action.getTarget().getId())))
                .map(JpaAction.class::cast);
    }

    @Override
    public Optional<Action> findActionWithDetails(final long actionId) {
        return actionRepository.findWithDetailsById(actionId)
                .filter(action -> targetRepository.exists(TargetSpecifications.hasId(action.getTarget().getId())));
    }

    @Override
    public Slice<Action> findActionsByTarget(final String controllerId, final Pageable pageable) {
        assertTargetReadAllowed(controllerId);
        return actionRepository.findAll(ActionSpecifications.byTargetControllerId(controllerId), pageable)
                .map(Action.class::cast);
    }

    @Override
    public Page<Action> findActionsByTarget(final String rsqlParam, final String controllerId,
            final Pageable pageable) {
        assertTargetReadAllowed(controllerId);

        final List<Specification<JpaAction>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, ActionFields.class, virtualPropertyReplacer, database),
                ActionSpecifications.byTargetControllerId(controllerId));

        return JpaManagementHelper.findAllWithCountBySpec(actionRepository, pageable, specList);
    }

    @Override
    public Page<Action> findActiveActionsByTarget(final Pageable pageable, final String controllerId) {
        assertTargetReadAllowed(controllerId);
        return actionRepository
                .findAll(ActionSpecifications.byTargetControllerIdAndActive(controllerId, true), pageable)
                .map(Action.class::cast);
    }

    @Override
    public Page<Action> findInActiveActionsByTarget(final Pageable pageable, final String controllerId) {
        assertTargetReadAllowed(controllerId);
        return actionRepository
                .findAll(ActionSpecifications.byTargetControllerIdAndActive(controllerId, false), pageable)
                .map(Action.class::cast);
    }

    @Override
    public List<Action> findActiveActionsWithHighestWeight(final String controllerId, final int maxActionCount) {
        assertTargetReadAllowed(controllerId);
        return findActiveActionsWithHighestWeightConsideringDefault(controllerId, maxActionCount);
    }

    @Override
    public long countActionsByTarget(final String controllerId) {
        assertTargetReadAllowed(controllerId);
        return actionRepository.countByTargetControllerId(controllerId);
    }

    @Override
    public long countActionsByTarget(final String rsqlParam, final String controllerId) {
        assertTargetReadAllowed(controllerId);

        final List<Specification<JpaAction>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, ActionFields.class, virtualPropertyReplacer, database),
                ActionSpecifications.byTargetControllerId(controllerId));

        return JpaManagementHelper.countBySpec(actionRepository, specList);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action forceTargetAction(final long actionId) {
        final JpaAction action = actionRepository.findById(actionId).map(this::assertTargetUpdateAllowed)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.isForcedOrTimeForced()) {
            action.setActionType(ActionType.FORCED);
            return actionRepository.save(action);
        }
        return action;
    }

    @Override
    public Page<ActionStatus> findActionStatusByAction(final Pageable pageReq, final long actionId) {
        assertActionExistsAndAccessible(actionId);

        return actionStatusRepository.findByActionId(pageReq, actionId);
    }

    @Override
    public long countActionStatusByAction(final long actionId) {
        assertActionExistsAndAccessible(actionId);

        return actionStatusRepository.countByActionId(actionId);
    }

    // action is already got and there are checked read permissions - do not check
    // permissions
    // and UI which is to be removed
    @Override
    public Page<String> findMessagesByActionStatusId(final Pageable pageable, final long actionStatusId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        final CriteriaQuery<String> msgQuery = cb.createQuery(String.class);
        final Root<JpaActionStatus> as = msgQuery.from(JpaActionStatus.class);
        final ListJoin<JpaActionStatus, String> join = as.joinList("messages", JoinType.LEFT);
        final CriteriaQuery<String> selMsgQuery = msgQuery.select(join);
        selMsgQuery.where(cb.equal(as.get(JpaActionStatus_.id), actionStatusId));

        final List<String> result = new ArrayList<>(entityManager.createQuery(selMsgQuery)
                .setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList());

        return new PageImpl<>(result, pageable, result.size());
    }

    @Override
    public long countMessagesByActionStatusId(final long actionStatusId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        final CriteriaQuery<Long> countMsgQuery = cb.createQuery(Long.class);
        final Root<JpaActionStatus> countMsgQueryFrom = countMsgQuery.distinct(true).from(JpaActionStatus.class);
        final ListJoin<JpaActionStatus, String> cJoin = countMsgQueryFrom.joinList("messages", JoinType.LEFT);
        countMsgQuery.select(cb.count(cJoin))
                .where(cb.equal(countMsgQueryFrom.get(JpaActionStatus_.id), actionStatusId));

        return entityManager.createQuery(countMsgQuery).getSingleResult();
    }

    @Override
    public long countActionsAll() {
        return actionRepository.count();
    }

    @Override
    public long countActions(final String rsqlParam) {
        final List<Specification<JpaAction>> specList = List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, ActionFields.class, virtualPropertyReplacer, database));
        return JpaManagementHelper.countBySpec(actionRepository, specList);
    }

    @Override
    public Slice<Action> findActionsAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(actionRepository, pageable, null);
    }

    @Override
    public Slice<Action> findActions(final String rsqlParam, final Pageable pageable) {
        final List<Specification<JpaAction>> specList = List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, ActionFields.class, virtualPropertyReplacer, database));
        return JpaManagementHelper.findAllWithoutCountBySpec(actionRepository, pageable, specList);
    }

    @Override
    public Optional<DistributionSet> getAssignedDistributionSet(final String controllerId) {
        return targetRepository
                .findOne(TargetSpecifications.hasControllerId(controllerId))
                .map(JpaTarget::getAssignedDistributionSet);
    }

    @Override
    public Optional<DistributionSet> getInstalledDistributionSet(final String controllerId) {
        return targetRepository
                .findOne(TargetSpecifications.hasControllerId(controllerId))
                .map(JpaTarget::getInstalledDistributionSet);
    }

    @Override
    @Transactional(readOnly = false)
    public int deleteActionsByStatusAndLastModifiedBefore(final Set<Status> status, final long lastModified) {
        if (status.isEmpty()) {
            return 0;
        }
        /*
         * We use a native query here because Spring JPA does not support to specify a
         * LIMIT clause on a DELETE statement. However, for this specific use case
         * (action cleanup), we must specify a row limit to reduce the overall load on
         * the database.
         */

        final int statusCount = status.size();
        final Status[] statusArr = status.toArray(new Status[statusCount]);

        final String queryStr = String.format(getQueryForDeleteActionsByStatusAndLastModifiedBeforeString(database),
                formatInClauseWithNumberKeys(statusCount));
        final Query deleteQuery = entityManager.createNativeQuery(queryStr);

        IntStream.range(0, statusCount)
                .forEach(i -> deleteQuery.setParameter(String.valueOf(i), statusArr[i].ordinal()));
        deleteQuery.setParameter("tenant", tenantAware.getCurrentTenant().toUpperCase());
        deleteQuery.setParameter("last_modified_at", lastModified);

        log.debug("Action cleanup: Executing the following (native) query: {}", deleteQuery);
        return deleteQuery.executeUpdate();
    }

    @Override
    public boolean hasPendingCancellations(final Long targetId) {
        // target access checked in assertTargetReadAllowed
        assertTargetReadAllowed(targetId);
        return actionRepository
                .exists(ActionSpecifications.byTargetIdAndIsActiveAndStatus(targetId, Action.Status.CANCELING));
    }

    private static String getQueryForDeleteActionsByStatusAndLastModifiedBeforeString(final Database database) {
        return QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED.getOrDefault(database,
                QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED_DEFAULT);
    }

    private static String formatInClauseWithNumberKeys(final int count) {
        return formatInClause(IntStream.range(0, count).mapToObj(String::valueOf).collect(Collectors.toList()));
    }

    private static String formatInClause(final Collection<String> elements) {
        return "#" + String.join(",#", elements);
    }

    protected ActionRepository getActionRepository() {
        return actionRepository;
    }

    protected boolean isActionsAutocloseEnabled() {
        return getConfigValue(REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, Boolean.class);
    }

    private boolean isMultiAssignmentsEnabled() {
        return TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement)
                .isMultiAssignmentsEnabled();
    }

    private boolean isConfirmationFlowEnabled() {
        return TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement)
                .isConfirmationFlowEnabled();
    }

    private <T extends Serializable> T getConfigValue(final String key, final Class<T> valueType) {
        return systemSecurityContext
                .runAsSystem(() -> tenantConfigurationManagement.getConfigurationValue(key, valueType).getValue());
    }

    private static RetryTemplate createRetryTemplate() {
        final RetryTemplate template = new RetryTemplate();

        final FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(Constants.TX_RT_DELAY);
        template.setBackOffPolicy(backOffPolicy);

        final SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(Constants.TX_RT_MAX,
                Collections.singletonMap(ConcurrencyFailureException.class, true));
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    @Override
    @Transactional
    public void cancelActionsForDistributionSet(final CancelationType cancelationType,
            final DistributionSet distributionSet) {
        actionRepository.findAll(ActionSpecifications
                .byDistributionSetIdAndActiveAndStatusIsNot(distributionSet.getId(), Status.CANCELING))
                .forEach(action -> {
                    try {
                        assertTargetUpdateAllowed(action);
                        cancelAction(action.getId());
                        log.debug("Action {} canceled", action.getId());
                    } catch (final InsufficientPermissionException e) {
                        log.trace("Could not cancel action {} due to insufficient permissions.", action.getId(), e);
                    } catch (final EntityNotFoundException e) {
                        log.trace("Could not cancel action {} due to entity not found exception.", action.getId(), e);
                    }
                });
        if (cancelationType == CancelationType.FORCE) {
            actionRepository.findAll(ActionSpecifications.byDistributionSetIdAndActive(distributionSet.getId()))
                    .forEach(action -> {
                        try {
                            assertTargetUpdateAllowed(action);
                            forceQuitAction(action.getId());
                            log.debug("Action {} force canceled", action.getId());
                        } catch (final InsufficientPermissionException e) {
                            log.trace("Could not cancel action {} due to insufficient permissions.", action.getId(), e);
                        } catch (final EntityNotFoundException e) {
                            log.trace("Could not cancel action {} due to entity not found exception.", action.getId(),
                                    e);
                        }
                    });
        }
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
        targetRepository.findOne(TargetSpecifications.hasId(action.getTarget().getId())).ifPresentOrElse(target -> {
            targetRepository.getAccessController()
                    .ifPresent(acm -> acm.assertOperationAllowed(AccessController.Operation.UPDATE, target));
        }, () -> {
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