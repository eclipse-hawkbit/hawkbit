/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutGroupCreate.addSuccessAndErrorConditionsAndActions;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManager;
import javax.validation.ConstraintDeclarationException;
import javax.validation.ValidationException;

import org.eclipse.hawkbit.repository.AbstractRolloutManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutFields;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutHelper;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.builder.GenericRolloutUpdate;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.event.remote.RolloutGroupDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupActionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupConditionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.RolloutSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.jpa.utils.WeightValidationHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroupsValidation;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Lists;

/**
 * JPA implementation of {@link RolloutManagement}.
 */
@Validated
@Transactional(readOnly = true)
public class JpaRolloutManagement extends AbstractRolloutManagement {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaRolloutManagement.class);

    /**
     * Max amount of targets that are handled in one transaction.
     */
    private static final int TRANSACTION_TARGETS = 5_000;

    /**
     * Maximum amount of actions that are deleted in one transaction.
     */
    private static final int TRANSACTION_ACTIONS = 5_000;

    private static final List<RolloutStatus> ACTIVE_ROLLOUTS = Arrays.asList(RolloutStatus.CREATING,
            RolloutStatus.DELETING, RolloutStatus.STARTING, RolloutStatus.READY, RolloutStatus.RUNNING);

    // In case of DOWNLOAD_ONLY, actions can be finished with DOWNLOADED status.
    private static final List<Status> DOWNLOAD_ONLY_ACTION_TERMINATION_STATUSES = Arrays.asList(Status.ERROR,
            Status.FINISHED, Status.CANCELED, Status.DOWNLOADED);
    private static final List<Status> DEFAULT_ACTION_TERMINATION_STATUSES = Arrays.asList(Status.ERROR, Status.FINISHED,
            Status.CANCELED);

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private RolloutTargetGroupRepository rolloutTargetGroupRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private QuotaManagement quotaManagement;

    @Autowired
    private RolloutStatusCache rolloutStatusCache;

    private final EventPublisherHolder eventPublisherHolder;

    private final Database database;

    JpaRolloutManagement(final TargetManagement targetManagement, final DeploymentManagement deploymentManagement,
            final RolloutGroupManagement rolloutGroupManagement,
            final DistributionSetManagement distributionSetManagement, final ApplicationContext context,
            final EventPublisherHolder eventPublisherHolder, final VirtualPropertyReplacer virtualPropertyReplacer,
            final PlatformTransactionManager txManager, final TenantAware tenantAware, final LockRegistry lockRegistry,
            final Database database, final RolloutApprovalStrategy rolloutApprovalStrategy,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemSecurityContext systemSecurityContext) {
        super(targetManagement, deploymentManagement, rolloutGroupManagement, distributionSetManagement, context,
                virtualPropertyReplacer, txManager, tenantAware, lockRegistry, rolloutApprovalStrategy,
                tenantConfigurationManagement, systemSecurityContext);
        this.eventPublisherHolder = eventPublisherHolder;
        this.database = database;
    }

    @Override
    public Page<Rollout> findAll(final Pageable pageable, final boolean deleted) {
        final Specification<JpaRollout> spec = RolloutSpecification.isDeletedWithDistributionSet(deleted);
        return JpaRolloutHelper.convertPage(rolloutRepository.findAll(spec, pageable), pageable);
    }

    @Override
    public Page<Rollout> findByRsql(final Pageable pageable, final String rsqlParam, final boolean deleted) {
        final List<Specification<JpaRollout>> specList = Lists.newArrayListWithExpectedSize(2);
        specList.add(RSQLUtility.parse(rsqlParam, RolloutFields.class, virtualPropertyReplacer, database));
        specList.add(RolloutSpecification.isDeletedWithDistributionSet(deleted));

        return JpaRolloutHelper.convertPage(findByCriteriaAPI(pageable, specList), pageable);
    }

    /**
     * Executes findAll with the given {@link Rollout} {@link Specification}s.
     */
    private Page<JpaRollout> findByCriteriaAPI(final Pageable pageable,
            final List<Specification<JpaRollout>> specList) {
        if (CollectionUtils.isEmpty(specList)) {
            return rolloutRepository.findAll(pageable);
        }

        return rolloutRepository.findAll(SpecificationsBuilder.combineWithAnd(specList), pageable);
    }

    @Override
    public Optional<Rollout> get(final long rolloutId) {
        return rolloutRepository.findById(rolloutId).map(r -> (Rollout) r);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout create(final RolloutCreate rollout, final int amountGroup, final RolloutGroupConditions conditions) {
        RolloutHelper.verifyRolloutGroupParameter(amountGroup, quotaManagement);
        final JpaRollout savedRollout = createRollout((JpaRollout) rollout.build());
        return createRolloutGroups(amountGroup, conditions, savedRollout);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout create(final RolloutCreate rollout, final List<RolloutGroupCreate> groups,
            final RolloutGroupConditions conditions) {
        RolloutHelper.verifyRolloutGroupParameter(groups.size(), quotaManagement);
        final JpaRollout savedRollout = createRollout((JpaRollout) rollout.build());
        return createRolloutGroups(groups, conditions, savedRollout);
    }

    private JpaRollout createRollout(final JpaRollout rollout) {
        WeightValidationHelper.usingContext(systemSecurityContext, tenantConfigurationManagement).validate(rollout);
        final Long totalTargets = targetManagement.countByRsql(rollout.getTargetFilterQuery());
        if (totalTargets == 0) {
            throw new ValidationException("Rollout does not match any existing targets");
        }
        rollout.setTotalTargets(totalTargets);
        return rolloutRepository.save(rollout);
    }

    private Rollout createRolloutGroups(final int amountOfGroups, final RolloutGroupConditions conditions,
            final JpaRollout rollout) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.CREATING);
        RolloutHelper.verifyRolloutGroupConditions(conditions);

        final JpaRollout savedRollout = rollout;

        // we can enforce the 'max targets per group' quota right here because
        // we want to distribute the targets equally to the different groups
        assertTargetsPerRolloutGroupQuota(rollout.getTotalTargets() / amountOfGroups);

        RolloutGroup lastSavedGroup = null;
        for (int i = 0; i < amountOfGroups; i++) {
            final String nameAndDesc = "group-" + (i + 1);
            final JpaRolloutGroup group = new JpaRolloutGroup();
            group.setName(nameAndDesc);
            group.setDescription(nameAndDesc);
            group.setRollout(savedRollout);
            group.setParent(lastSavedGroup);
            group.setStatus(RolloutGroupStatus.CREATING);

            addSuccessAndErrorConditionsAndActions(group, conditions);

            group.setTargetPercentage(1.0F / (amountOfGroups - i) * 100);

            lastSavedGroup = rolloutGroupRepository.save(group);
            publishRolloutGroupCreatedEventAfterCommit(lastSavedGroup, rollout);
        }

        savedRollout.setRolloutGroupsCreated(amountOfGroups);
        return rolloutRepository.save(savedRollout);
    }

    private Rollout createRolloutGroups(final List<RolloutGroupCreate> groupList,
            final RolloutGroupConditions conditions, final Rollout rollout) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.CREATING);
        final JpaRollout savedRollout = (JpaRollout) rollout;

        // prepare the groups
        final List<RolloutGroup> groups = groupList.stream()
                .map(group -> JpaRolloutHelper.prepareRolloutGroupWithDefaultConditions(group, conditions))
                .collect(Collectors.toList());
        groups.forEach(RolloutHelper::verifyRolloutGroupHasConditions);

        RolloutHelper.verifyRemainingTargets(
                calculateRemainingTargets(groups, savedRollout.getTargetFilterQuery(), savedRollout.getCreatedAt()));

        // check if we need to enforce the 'max targets per group' quota
        if (quotaManagement.getMaxTargetsPerRolloutGroup() > 0) {
            validateTargetsInGroups(groups, savedRollout.getTargetFilterQuery(), savedRollout.getCreatedAt())
                    .getTargetsPerGroup().forEach(this::assertTargetsPerRolloutGroupQuota);
        }

        // create and persist the groups (w/o filling them with targets)
        RolloutGroup lastSavedGroup = null;
        for (final RolloutGroup srcGroup : groups) {
            final JpaRolloutGroup group = new JpaRolloutGroup();
            group.setName(srcGroup.getName());
            group.setDescription(srcGroup.getDescription());
            group.setRollout(savedRollout);
            group.setParent(lastSavedGroup);
            group.setStatus(RolloutGroupStatus.CREATING);

            group.setTargetPercentage(srcGroup.getTargetPercentage());
            if (srcGroup.getTargetFilterQuery() != null) {
                group.setTargetFilterQuery(srcGroup.getTargetFilterQuery());
            } else {
                group.setTargetFilterQuery("");
            }

            addSuccessAndErrorConditionsAndActions(group, srcGroup.getSuccessCondition(),
                    srcGroup.getSuccessConditionExp(), srcGroup.getSuccessAction(), srcGroup.getSuccessActionExp(),
                    srcGroup.getErrorCondition(), srcGroup.getErrorConditionExp(), srcGroup.getErrorAction(),
                    srcGroup.getErrorActionExp());

            lastSavedGroup = rolloutGroupRepository.save(group);
            publishRolloutGroupCreatedEventAfterCommit(lastSavedGroup, rollout);
        }

        savedRollout.setRolloutGroupsCreated(groups.size());
        return rolloutRepository.save(savedRollout);
    }

    private void publishRolloutGroupCreatedEventAfterCommit(final RolloutGroup group, final Rollout rollout) {
        afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher().publishEvent(
                new RolloutGroupCreatedEvent(group, rollout.getId(), eventPublisherHolder.getApplicationId())));
    }

    private void handleCreateRollout(final JpaRollout rollout) {
        LOGGER.debug("handleCreateRollout called for rollout {}", rollout.getId());

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

            final RolloutGroup filledGroup = fillRolloutGroupWithTargets(rollout, group);
            if (RolloutGroupStatus.READY == filledGroup.getStatus()) {
                readyGroups++;
                totalTargets += filledGroup.getTotalTargets();
            }
        }

        // When all groups are ready the rollout status can be changed to be
        // ready, too.
        if (readyGroups == rolloutGroups.size()) {
            if (!rolloutApprovalStrategy.isApprovalNeeded(rollout)) {
                rollout.setStatus(RolloutStatus.READY);
                LOGGER.debug("rollout {} creation done. Switch to READY.", rollout.getId());
            } else {
                LOGGER.debug("rollout {} creation done. Switch to WAITING_FOR_APPROVAL.", rollout.getId());
                rollout.setStatus(RolloutStatus.WAITING_FOR_APPROVAL);
                rolloutApprovalStrategy.onApprovalRequired(rollout);
            }
            rollout.setLastCheck(0);
            rollout.setTotalTargets(totalTargets);
            rolloutRepository.save(rollout);
        }
    }

    private RolloutGroup fillRolloutGroupWithTargets(final JpaRollout rollout, final RolloutGroup group1) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.CREATING);

        final JpaRolloutGroup group = (JpaRolloutGroup) group1;

        final String baseFilter = RolloutHelper.getTargetFilterQuery(rollout);
        final String groupTargetFilter;
        if (StringUtils.isEmpty(group.getTargetFilterQuery())) {
            groupTargetFilter = baseFilter;
        } else {
            groupTargetFilter = baseFilter + ";" + group.getTargetFilterQuery();
        }

        final List<Long> readyGroups = RolloutHelper.getGroupsByStatusIncludingGroup(rollout.getRolloutGroups(),
                RolloutGroupStatus.READY, group);

        final long targetsInGroupFilter = DeploymentHelper.runInNewTransaction(txManager,
                "countAllTargetsByTargetFilterQueryAndNotInRolloutGroups",
                count -> targetManagement.countByRsqlAndNotInRolloutGroups(readyGroups, groupTargetFilter));
        final long expectedInGroup = Math.round(group.getTargetPercentage() / 100 * (double) targetsInGroupFilter);
        final long currentlyInGroup = DeploymentHelper.runInNewTransaction(txManager,
                "countRolloutTargetGroupByRolloutGroup",
                count -> rolloutTargetGroupRepository.countByRolloutGroup(group));

        // Switch the Group status to READY, when there are enough Targets in
        // the Group
        if (currentlyInGroup >= expectedInGroup) {
            group.setStatus(RolloutGroupStatus.READY);
            return rolloutGroupRepository.save(group);
        }

        try {

            long targetsLeftToAdd = expectedInGroup - currentlyInGroup;

            do {
                // Add up to TRANSACTION_TARGETS of the left targets
                // In case a TransactionException is thrown this loop aborts
                targetsLeftToAdd -= assignTargetsToGroupInNewTransaction(rollout, group, groupTargetFilter,
                        Math.min(TRANSACTION_TARGETS, targetsLeftToAdd));
            } while (targetsLeftToAdd > 0);

            group.setStatus(RolloutGroupStatus.READY);
            group.setTotalTargets(
                    DeploymentHelper.runInNewTransaction(txManager, "countRolloutTargetGroupByRolloutGroup",
                            count -> rolloutTargetGroupRepository.countByRolloutGroup(group)).intValue());
            return rolloutGroupRepository.save(group);

        } catch (final TransactionException e) {
            LOGGER.warn("Transaction assigning Targets to RolloutGroup failed", e);
            return group;
        }
    }

    private Long assignTargetsToGroupInNewTransaction(final JpaRollout rollout, final RolloutGroup group,
            final String targetFilter, final long limit) {

        return DeploymentHelper.runInNewTransaction(txManager, "assignTargetsToRolloutGroup", status -> {
            final PageRequest pageRequest = PageRequest.of(0, Math.toIntExact(limit));
            final List<Long> readyGroups = RolloutHelper.getGroupsByStatusIncludingGroup(rollout.getRolloutGroups(),
                    RolloutGroupStatus.READY, group);
            final Page<Target> targets = targetManagement.findByTargetFilterQueryAndNotInRolloutGroups(pageRequest,
                    readyGroups, targetFilter);

            createAssignmentOfTargetsToGroup(targets, group);

            return Long.valueOf(targets.getNumberOfElements());
        });
    }

    private void createAssignmentOfTargetsToGroup(final Page<Target> targets, final RolloutGroup group) {
        targets.forEach(target -> rolloutTargetGroupRepository.save(new RolloutTargetGroup(group, target)));
    }

    @Override
    @Async
    public ListenableFuture<RolloutGroupsValidation> validateTargetsInGroups(final List<RolloutGroupCreate> groups,
            final String targetFilter, final Long createdAt) {

        final String baseFilter = RolloutHelper.getTargetFilterQuery(targetFilter, createdAt);
        final long totalTargets = targetManagement.countByRsql(baseFilter);
        if (totalTargets == 0) {
            throw new ConstraintDeclarationException("Rollout target filter does not match any targets");
        }

        return new AsyncResult<>(validateTargetsInGroups(
                groups.stream().map(RolloutGroupCreate::build).collect(Collectors.toList()), baseFilter, totalTargets));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout approveOrDeny(final long rolloutId, final Rollout.ApprovalDecision decision) {
        return this.approveOrDeny(rolloutId, decision, null);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout approveOrDeny(final long rolloutId, final Rollout.ApprovalDecision decision, final String remark) {
        LOGGER.debug("approveOrDeny rollout called for rollout {} with decision {}", rolloutId, decision);
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.WAITING_FOR_APPROVAL);
        switch (decision) {
        case APPROVED:
            rollout.setStatus(RolloutStatus.READY);
            break;
        case DENIED:
            rollout.setStatus(RolloutStatus.APPROVAL_DENIED);
            break;
        default:
            throw new IllegalArgumentException("Unknown approval decision: " + decision);
        }
        rollout.setApprovalDecidedBy(rolloutApprovalStrategy.getApprovalUser(rollout));
        if (remark != null) {
            rollout.setApprovalRemark(remark);
        }
        return rolloutRepository.save(rollout);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout start(final long rolloutId) {
        LOGGER.debug("startRollout called for rollout {}", rolloutId);

        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        RolloutHelper.checkIfRolloutCanStarted(rollout, rollout);
        rollout.setStatus(RolloutStatus.STARTING);
        rollout.setLastCheck(0);
        return rolloutRepository.save(rollout);
    }

    private void startFirstRolloutGroup(final Rollout rollout) {
        LOGGER.debug("startFirstRolloutGroup called for rollout {}", rollout.getId());
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
            group.setStatus(RolloutGroupStatus.SCHEDULED);
            rolloutGroupRepository.save(group);
            return true;
        }
        return false;
    }

    private long createActionsForRolloutGroup(final Rollout rollout, final RolloutGroup group) {
        long totalActionsCreated = 0;
        try {
            long actionsCreated;
            do {
                actionsCreated = createActionsForTargetsInNewTransaction(rollout.getId(), group.getId(), TRANSACTION_TARGETS);
                totalActionsCreated += actionsCreated;
            } while (actionsCreated > 0);

        } catch (final TransactionException e) {
            LOGGER.warn("Transaction assigning Targets to RolloutGroup failed", e);
            return 0;
        }
        return totalActionsCreated;
    }

    private Long createActionsForTargetsInNewTransaction(final long rolloutId, final long groupId,
            final int limit) {
        return DeploymentHelper.runInNewTransaction(txManager, "createActionsForTargets", status -> {
            final PageRequest pageRequest = PageRequest.of(0, limit);
            final Rollout rollout = rolloutRepository.findById(rolloutId)
                    .orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
            final RolloutGroup group = rolloutGroupRepository.findById(groupId)
                    .orElseThrow(() -> new EntityNotFoundException(RolloutGroup.class, groupId));

            final DistributionSet distributionSet = rollout.getDistributionSet();
            final ActionType actionType = rollout.getActionType();
            final long forceTime = rollout.getForcedTime();

            final Page<Target> targets = targetManagement.findByInRolloutGroupWithoutAction(pageRequest, groupId);
            if (targets.getTotalElements() > 0) {
                createScheduledAction(targets.getContent(), distributionSet, actionType, forceTime, rollout, group);
            }

            return Long.valueOf(targets.getNumberOfElements());
        });
    }

    /**
     * Creates an action entry into the action repository. In case of existing
     * scheduled actions the scheduled actions gets canceled. A scheduled action
     * is created in-active.
     */
    private void createScheduledAction(final Collection<Target> targets, final DistributionSet distributionSet,
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
        });
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void pauseRollout(final long rolloutId) {
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        if (RolloutStatus.RUNNING != rollout.getStatus()) {
            throw new RolloutIllegalStateException("Rollout can only be paused in state running but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
        // setting the complete rollout only in paused state. This is sufficient
        // due the currently running groups will be completed and new groups are
        // not started until rollout goes back to running state again. The
        // periodically check for running rollouts will skip rollouts in pause
        // state.
        rollout.setStatus(RolloutStatus.PAUSED);
        rolloutRepository.save(rollout);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void resumeRollout(final long rolloutId) {
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        if (RolloutStatus.PAUSED != rollout.getStatus()) {
            throw new RolloutIllegalStateException("Rollout can only be resumed in state paused but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
        rollout.setStatus(RolloutStatus.RUNNING);
        rolloutRepository.save(rollout);
    }

    private void handleRunningRollout(final JpaRollout rollout) {
        LOGGER.debug("handleRunningRollout called for rollout {}", rollout.getId());

        final List<JpaRolloutGroup> rolloutGroupsRunning = rolloutGroupRepository.findByRolloutAndStatus(rollout,
                RolloutGroupStatus.RUNNING);

        if (rolloutGroupsRunning.isEmpty()) {
            // no running rollouts, probably there was an error
            // somewhere at the latest group. And the latest group has
            // been switched from running into error state. So we need
            // to find the latest group which
            executeLatestRolloutGroup(rollout);
        } else {
            LOGGER.debug("Rollout {} has {} running groups", rollout.getId(), rolloutGroupsRunning.size());
            executeRolloutGroups(rollout, rolloutGroupsRunning);
        }

        if (isRolloutComplete(rollout)) {
            LOGGER.info("Rollout {} is finished, setting FINISHED status", rollout);
            rollout.setStatus(RolloutStatus.FINISHED);
            rolloutRepository.save(rollout);
        }
    }

    private void executeRolloutGroups(final JpaRollout rollout, final List<JpaRolloutGroup> rolloutGroups) {
        for (final JpaRolloutGroup rolloutGroup : rolloutGroups) {

            final long targetCount = countTargetsFrom(rolloutGroup);
            if (rolloutGroup.getTotalTargets() != targetCount) {
                updateTotalTargetCount(rolloutGroup, targetCount);
            }

            // error state check, do we need to stop the whole
            // rollout because of error?
            final boolean isError = checkErrorState(rollout, rolloutGroup);
            if (isError) {
                LOGGER.info("Rollout {} {} has error, calling error action", rollout.getName(), rollout.getId());
                callErrorAction(rollout, rolloutGroup);
            } else {
                // not in error so check finished state, do we need to
                // start the next group?
                final RolloutGroupSuccessCondition finishedCondition = rolloutGroup.getSuccessCondition();
                checkFinishCondition(rollout, rolloutGroup, finishedCondition);
                if (isRolloutGroupComplete(rollout, rolloutGroup)) {
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
        return rolloutGroupManagement.countTargetsOfRolloutsGroup(rolloutGroup.getId());
    }

    private void executeLatestRolloutGroup(final JpaRollout rollout) {
        final List<JpaRolloutGroup> latestRolloutGroup = rolloutGroupRepository
                .findByRolloutAndStatusNotOrderByIdDesc(rollout, RolloutGroupStatus.SCHEDULED);
        if (latestRolloutGroup.isEmpty()) {
            return;
        }
        executeRolloutGroupSuccessAction(rollout, latestRolloutGroup.get(0));
    }

    private void callErrorAction(final Rollout rollout, final RolloutGroup rolloutGroup) {
        try {
            context.getBean(rolloutGroup.getErrorAction().getBeanName(), RolloutGroupActionEvaluator.class)
                    .eval(rollout, rolloutGroup, rolloutGroup.getErrorActionExp());
        } catch (final BeansException e) {
            LOGGER.error("Something bad happend when accessing the error action bean {}",
                    rolloutGroup.getErrorAction().getBeanName(), e);
        }
    }

    private boolean isRolloutComplete(final JpaRollout rollout) {
        // ensure that changes in the same transaction count
        entityManager.flush();
        final Long groupsActiveLeft = rolloutGroupRepository.countByRolloutIdAndStatusOrStatus(rollout.getId(),
                RolloutGroupStatus.RUNNING, RolloutGroupStatus.SCHEDULED);
        return groupsActiveLeft == 0;
    }

    private boolean isRolloutGroupComplete(final JpaRollout rollout, final JpaRolloutGroup rolloutGroup) {
        final Long actionsLeftForRollout = ActionType.DOWNLOAD_ONLY == rollout.getActionType()
                ? actionRepository.countByRolloutAndRolloutGroupAndStatusNotIn(rollout, rolloutGroup,
                        DOWNLOAD_ONLY_ACTION_TERMINATION_STATUSES)
                : actionRepository.countByRolloutAndRolloutGroupAndStatusNotIn(rollout, rolloutGroup,
                        DEFAULT_ACTION_TERMINATION_STATUSES);
        return actionsLeftForRollout == 0;
    }

    private boolean checkErrorState(final Rollout rollout, final RolloutGroup rolloutGroup) {

        final RolloutGroupErrorCondition errorCondition = rolloutGroup.getErrorCondition();

        if (errorCondition == null) {
            // there is no error condition, so return false, don't have error.
            return false;
        }
        try {
            return context.getBean(errorCondition.getBeanName(), RolloutGroupConditionEvaluator.class).eval(rollout,
                    rolloutGroup, rolloutGroup.getErrorConditionExp());
        } catch (final BeansException e) {
            LOGGER.error("Something bad happend when accessing the error condition bean {}",
                    errorCondition.getBeanName(), e);
            return false;
        }
    }

    private boolean checkFinishCondition(final Rollout rollout, final RolloutGroup rolloutGroup,
            final RolloutGroupSuccessCondition finishCondition) {
        LOGGER.trace("Checking finish condition {} on rolloutgroup {}", finishCondition, rolloutGroup);
        try {
            final boolean isFinished = context
                    .getBean(finishCondition.getBeanName(), RolloutGroupConditionEvaluator.class)
                    .eval(rollout, rolloutGroup, rolloutGroup.getSuccessConditionExp());
            if (isFinished) {
                LOGGER.debug("Rolloutgroup {} is finished, starting next group", rolloutGroup);
                executeRolloutGroupSuccessAction(rollout, rolloutGroup);
            } else {
                LOGGER.debug("Rolloutgroup {} is still running", rolloutGroup);
            }
            return isFinished;
        } catch (final BeansException e) {
            LOGGER.error("Something bad happend when accessing the finish condition bean {}",
                    finishCondition.getBeanName(), e);
            return false;
        }
    }

    private void executeRolloutGroupSuccessAction(final Rollout rollout, final RolloutGroup rolloutGroup) {
        context.getBean(rolloutGroup.getSuccessAction().getBeanName(), RolloutGroupActionEvaluator.class).eval(rollout,
                rolloutGroup, rolloutGroup.getSuccessActionExp());
    }

    @Override
    // No transaction, will be created per handled rollout
    @Transactional(propagation = Propagation.NEVER)
    public void handleRollouts() {
        final List<Long> rollouts = rolloutRepository.findByStatusIn(ACTIVE_ROLLOUTS);

        if (rollouts.isEmpty()) {
            return;
        }

        final String tenant = tenantAware.getCurrentTenant();

        final String handlerId = tenant + "-rollout";
        final Lock lock = lockRegistry.obtain(handlerId);
        if (!lock.tryLock()) {
            return;
        }

        try {
            rollouts.forEach(rolloutId -> DeploymentHelper.runInNewTransaction(txManager, handlerId + "-" + rolloutId,
                    status -> handleRollout(rolloutId)));
        } finally {
            lock.unlock();
        }
    }

    private long handleRollout(final long rolloutId) {
        final JpaRollout rollout = rolloutRepository.findById(rolloutId)
                .orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
        runInUserContext(rollout, () -> handleRollout(rollout));
        return 0;
    }

    private void handleRollout(final JpaRollout rollout) {
        LOGGER.debug("Handle rollout {}", rollout.getId());
        switch (rollout.getStatus()) {
        case CREATING:
            handleCreateRollout(rollout);
            break;
        case DELETING:
            handleDeleteRollout(rollout);
            break;
        case READY:
            handleReadyRollout(rollout);
            break;
        case STARTING:
            handleStartingRollout(rollout);
            break;
        case RUNNING:
            handleRunningRollout(rollout);
            break;
        default:
            LOGGER.error("Rollout in status {} not supposed to be handled!", rollout.getStatus());
            break;
        }
    }

    private void handleStartingRollout(final Rollout rollout) {
        LOGGER.debug("handleStartingRollout called for rollout {}", rollout.getId());

        if (ensureAllGroupsAreScheduled(rollout)) {
            startFirstRolloutGroup(rollout);
        }
    }

    private void handleReadyRollout(final Rollout rollout) {
        if (rollout.getStartAt() != null && rollout.getStartAt() <= System.currentTimeMillis()) {
            LOGGER.debug(
                    "handleReadyRollout called for rollout {} with autostart beyond define time. Switch to STARTING",
                    rollout.getId());
            start(rollout.getId());
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long rolloutId) {
        final JpaRollout jpaRollout = rolloutRepository.findById(rolloutId)
                .orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));

        if (jpaRollout == null) {
            throw new EntityNotFoundException(Rollout.class, rolloutId);
        }

        if (RolloutStatus.DELETING == jpaRollout.getStatus()) {
            return;
        }

        jpaRollout.setStatus(RolloutStatus.DELETING);
        rolloutRepository.save(jpaRollout);
    }

    private void handleDeleteRollout(final JpaRollout rollout) {
        LOGGER.debug("handleDeleteRollout called for {}", rollout.getId());

        // check if there are actions beyond schedule
        boolean hardDeleteRolloutGroups = !actionRepository.existsByRolloutIdAndStatusNotIn(rollout.getId(),
                Status.SCHEDULED);
        if (hardDeleteRolloutGroups) {
            LOGGER.debug("Rollout {} has no actions other than scheduled -> hard delete", rollout.getId());
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

    private void sendRolloutGroupDeletedEvents(final JpaRollout rollout) {
        final List<Long> groupIds = rollout.getRolloutGroups().stream().map(RolloutGroup::getId)
                .collect(Collectors.toList());

        afterCommit.afterCommit(() -> groupIds.forEach(rolloutGroupId -> eventPublisherHolder.getEventPublisher()
                .publishEvent(new RolloutGroupDeletedEvent(tenantAware.getCurrentTenant(), rolloutGroupId,
                        JpaRolloutGroup.class.getName(), eventPublisherHolder.getApplicationId()))));
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
                LOGGER.error("Exception during deletion of actions of rollout {}", rollout, e);
            }
        }
    }

    private Slice<JpaAction> findScheduledActionsByRollout(final JpaRollout rollout) {
        return actionRepository.findByRolloutIdAndStatus(PageRequest.of(0, TRANSACTION_ACTIONS), rollout.getId(),
                Status.SCHEDULED);
    }

    @Override
    public long count() {
        return rolloutRepository.count(RolloutSpecification.isDeletedWithDistributionSet(false));
    }

    @Override
    public long countByFilters(final String searchText) {
        return rolloutRepository.count(JpaRolloutHelper.likeNameOrDescription(searchText, false));
    }

    @Override
    public Slice<Rollout> findByFiltersWithDetailedStatus(final Pageable pageable, final String searchText,
            final boolean deleted) {
        final Slice<JpaRollout> findAll = findByCriteriaAPI(pageable,
                Arrays.asList(JpaRolloutHelper.likeNameOrDescription(searchText, deleted)));
        setRolloutStatusDetails(findAll);
        return JpaRolloutHelper.convertPage(findAll, pageable);
    }

    @Override
    public Optional<Rollout> getByName(final String rolloutName) {
        return rolloutRepository.findByName(rolloutName);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout update(final RolloutUpdate u) {
        final GenericRolloutUpdate update = (GenericRolloutUpdate) u;
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(update.getId());

        checkIfDeleted(update.getId(), rollout.getStatus());
        update.getName().ifPresent(rollout::setName);
        update.getDescription().ifPresent(rollout::setDescription);
        update.getActionType().ifPresent(rollout::setActionType);
        update.getForcedTime().ifPresent(rollout::setForcedTime);
        update.getWeight().ifPresent(rollout::setWeight);
        // reseting back to manual start is done by setting start at time to
        // null
        rollout.setStartAt(update.getStartAt().orElse(null));
        update.getSet().ifPresent(setId -> {
            final DistributionSet set = distributionSetManagement.get(setId)
                    .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, setId));

            rollout.setDistributionSet(set);
        });
        if (rolloutApprovalStrategy.isApprovalNeeded(rollout)) {
            rollout.setStatus(RolloutStatus.WAITING_FOR_APPROVAL);
            rollout.setApprovalDecidedBy(null);
            rollout.setApprovalRemark(null);
        }

        return rolloutRepository.save(rollout);
    }

    private static void checkIfDeleted(final Long rolloutId, final RolloutStatus status) {
        if (RolloutStatus.DELETING == status || RolloutStatus.DELETED == status) {
            throw new EntityReadOnlyException("Rollout " + rolloutId + " is soft deleted and cannot be changed");
        }
    }

    private JpaRollout getRolloutAndThrowExceptionIfNotFound(final Long rolloutId) {
        return rolloutRepository.findById(rolloutId)
                .orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
    }

    @Override
    public Page<Rollout> findAllWithDetailedStatus(final Pageable pageable, final boolean deleted) {
        Page<JpaRollout> rollouts;
        final Specification<JpaRollout> spec = RolloutSpecification.isDeletedWithDistributionSet(deleted);
        rollouts = rolloutRepository.findAll(spec, pageable);
        setRolloutStatusDetails(rollouts);
        return JpaRolloutHelper.convertPage(rollouts, pageable);
    }

    @Override
    public Optional<Rollout> getWithDetailedStatus(final long rolloutId) {
        final Optional<Rollout> rollout = get(rolloutId);

        if (!rollout.isPresent()) {
            return rollout;
        }

        List<TotalTargetCountActionStatus> rolloutStatusCountItems = rolloutStatusCache.getRolloutStatus(rolloutId);

        if (CollectionUtils.isEmpty(rolloutStatusCountItems)) {
            rolloutStatusCountItems = actionRepository.getStatusCountByRolloutId(rolloutId);
            rolloutStatusCache.putRolloutStatus(rolloutId, rolloutStatusCountItems);
        }

        final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(rolloutStatusCountItems,
                rollout.get().getTotalTargets(), rollout.get().getActionType());
        ((JpaRollout) rollout.get()).setTotalTargetCountStatus(totalTargetCountStatus);
        return rollout;
    }

    @Override
    public boolean exists(final long rolloutId) {
        return rolloutRepository.existsById(rolloutId);
    }

    private Map<Long, List<TotalTargetCountActionStatus>> getStatusCountItemForRollout(final List<Long> rollouts) {
        if (rollouts.isEmpty()) {
            return null;
        }

        final Map<Long, List<TotalTargetCountActionStatus>> fromCache = rolloutStatusCache.getRolloutStatus(rollouts);

        final List<Long> rolloutIds = rollouts.stream().filter(id -> !fromCache.containsKey(id))
                .collect(Collectors.toList());

        if (!rolloutIds.isEmpty()) {
            final List<TotalTargetCountActionStatus> resultList = actionRepository
                    .getStatusCountByRolloutId(rolloutIds);
            final Map<Long, List<TotalTargetCountActionStatus>> fromDb = resultList.stream()
                    .collect(Collectors.groupingBy(TotalTargetCountActionStatus::getId));

            rolloutStatusCache.putRolloutStatus(fromDb);

            fromCache.putAll(fromDb);
        }

        return fromCache;
    }

    private void setRolloutStatusDetails(final Slice<JpaRollout> rollouts) {
        final List<Long> rolloutIds = rollouts.getContent().stream().map(Rollout::getId).collect(Collectors.toList());
        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRollout(
                rolloutIds);

        if (allStatesForRollout != null) {
            rollouts.forEach(rollout -> {
                final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                        allStatesForRollout.get(rollout.getId()), rollout.getTotalTargets(), rollout.getActionType());
                rollout.setTotalTargetCountStatus(totalTargetCountStatus);
            });
        }
    }

    /**
     * Enforces the quota defining the maximum number of {@link Target}s per
     * {@link RolloutGroup}.
     *
     * @param group
     *            The rollout group
     * @param requested
     *            number of targets to check
     */
    private void assertTargetsPerRolloutGroupQuota(final long requested) {
        final int quota = quotaManagement.getMaxTargetsPerRolloutGroup();
        QuotaHelper.assertAssignmentQuota(requested, quota, Target.class, RolloutGroup.class);
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

    private void runInUserContext(final BaseEntity rollout, final Runnable handler) {
        DeploymentHelper.runInNonSystemContext(handler, () -> Objects.requireNonNull(rollout.getCreatedBy()), tenantAware);
    }

}
