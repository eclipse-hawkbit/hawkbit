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

import static org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutGroupCreate.addSuccessAndErrorConditionsAndActions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintDeclarationException;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutFields;
import org.eclipse.hawkbit.repository.RolloutHelper;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.builder.DynamicRolloutGroupTemplate;
import org.eclipse.hawkbit.repository.builder.GenericRolloutUpdate;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutGroupCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout_;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.StartNextGroupRolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.RolloutSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.jpa.utils.WeightValidationHelper;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroupsValidation;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link RolloutManagement}.
 */
@Slf4j
@Validated
@Transactional(readOnly = true)
public class JpaRolloutManagement implements RolloutManagement {

    private static final List<RolloutStatus> ACTIVE_ROLLOUTS = Arrays.asList(RolloutStatus.CREATING,
            RolloutStatus.DELETING, RolloutStatus.STARTING, RolloutStatus.READY, RolloutStatus.RUNNING,
            RolloutStatus.STOPPING);

    private static final List<RolloutStatus> ROLLOUT_STATUS_STOPPABLE = Arrays.asList(RolloutStatus.RUNNING,
            RolloutStatus.CREATING, RolloutStatus.PAUSED, RolloutStatus.READY, RolloutStatus.STARTING,
            RolloutStatus.WAITING_FOR_APPROVAL, RolloutStatus.APPROVAL_DENIED);
    private final TargetManagement targetManagement;
    private final DistributionSetManagement distributionSetManagement;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final RolloutApprovalStrategy rolloutApprovalStrategy;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final EventPublisherHolder eventPublisherHolder;
    private final ContextAware contextAware;
    private final Database database;
    @Autowired
    private RepositoryProperties repositoryProperties;
    @Autowired
    private RolloutRepository rolloutRepository;
    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;
    @Autowired
    private ActionRepository actionRepository;
    @Autowired
    private AfterTransactionCommitExecutor afterCommit;
    @Autowired
    private QuotaManagement quotaManagement;
    @Autowired
    private RolloutStatusCache rolloutStatusCache;
    @Autowired
    private StartNextGroupRolloutGroupSuccessAction startNextRolloutGroupAction;

    public JpaRolloutManagement(final TargetManagement targetManagement,
            final DistributionSetManagement distributionSetManagement, final EventPublisherHolder eventPublisherHolder,
            final VirtualPropertyReplacer virtualPropertyReplacer, final Database database,
            final RolloutApprovalStrategy rolloutApprovalStrategy,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemSecurityContext systemSecurityContext,
            final ContextAware contextAware) {
        this.targetManagement = targetManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
        this.rolloutApprovalStrategy = rolloutApprovalStrategy;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.eventPublisherHolder = eventPublisherHolder;
        this.contextAware = contextAware;
    }

    public static String createRolloutLockKey(final String tenant) {
        return tenant + "-rollout";
    }

    public void publishRolloutGroupCreatedEventAfterCommit(final RolloutGroup group, final Rollout rollout) {
        afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher().publishEvent(
                new RolloutGroupCreatedEvent(group, rollout.getId(), eventPublisherHolder.getApplicationId())));
    }

    @Override
    public long count() {
        return rolloutRepository.count(
                RolloutSpecification.isDeletedWithDistributionSet(false, Sort.by(Direction.DESC, JpaRollout_.ID)));
    }

    @Override
    public long countByFilters(final String searchText) {
        return rolloutRepository.count(RolloutSpecification.likeName(searchText, false));
    }

    @Override
    public long countByDistributionSetIdAndRolloutIsStoppable(final long setId) {
        return rolloutRepository.countByDistributionSetIdAndStatusIn(setId, ROLLOUT_STATUS_STOPPABLE);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout create(final RolloutCreate rollout, final int amountGroup, final boolean confirmationRequired,
            final RolloutGroupConditions conditions, final DynamicRolloutGroupTemplate dynamicRolloutGroupTemplate) {
        if (amountGroup < 0) {
            throw new ValidationException("The amount of groups cannot be lower than or equal to zero for static rollouts");
        } else if (amountGroup == 0) {
            if (dynamicRolloutGroupTemplate == null) {
                throw new ValidationException(
                        "When amount of groups is 0, the rollouts shall be dynamic and a dynamic group template must be provided");
            }
        } else {
            RolloutHelper.verifyRolloutGroupAmount(amountGroup, quotaManagement);
        }

        final JpaRollout rolloutRequest = (JpaRollout) rollout.build();
        if (dynamicRolloutGroupTemplate != null && !rolloutRequest.isDynamic()) {
            throw new ValidationException("Dynamic group template is only allowed for dynamic rollouts");
        }

        final JpaRollout savedRollout = createRollout(rolloutRequest, amountGroup == 0);
        return createRolloutGroups(amountGroup, conditions, savedRollout, confirmationRequired, dynamicRolloutGroupTemplate);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout create(@NotNull @Valid RolloutCreate create, int amountGroup, boolean confirmationRequired,
            @NotNull RolloutGroupConditions conditions) {
        return create(create, amountGroup, confirmationRequired, conditions, null);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout create(final RolloutCreate rollout, final List<RolloutGroupCreate> groups,
            final RolloutGroupConditions conditions) {
        if (groups.isEmpty()) {
            throw new ValidationException("The amount of groups cannot be 0");
        }
        RolloutHelper.verifyRolloutGroupAmount(groups.size(), quotaManagement);
        final JpaRollout rolloutRequest = (JpaRollout) rollout.build();
        final JpaRollout savedRollout = createRollout(rolloutRequest, false);
        return createRolloutGroups(groups, conditions, savedRollout);
    }

    @Override
    @Async
    public ListenableFuture<RolloutGroupsValidation> validateTargetsInGroups(final List<RolloutGroupCreate> groups,
            final String targetFilter, final Long createdAt, final Long dsTypeId) {

        final TargetCount targets = calculateTargets(targetFilter, createdAt, dsTypeId);
        long totalTargets = targets.total();
        final String baseFilter = targets.filter();

        if (totalTargets == 0) {
            throw new ConstraintDeclarationException("Rollout target filter does not match any targets");
        }

        return new AsyncResult<>(
                validateTargetsInGroups(groups.stream().map(RolloutGroupCreate::build).collect(Collectors.toList()),
                        baseFilter, totalTargets, dsTypeId));
    }

    @Override
    public Page<Rollout> findAll(final Pageable pageable, final boolean deleted) {
        return JpaManagementHelper.findAllWithCountBySpec(rolloutRepository, pageable, Collections
                .singletonList(RolloutSpecification.isDeletedWithDistributionSet(deleted, pageable.getSort())));
    }

    @Override
    public Slice<Rollout> findAllWithDetailedStatus(final Pageable pageable, final boolean deleted) {
        final Slice<Rollout> rollouts = JpaManagementHelper.findAllWithoutCountBySpec(rolloutRepository, pageable,
                Collections
                        .singletonList(RolloutSpecification.isDeletedWithDistributionSet(deleted, pageable.getSort())));
        setRolloutStatusDetails(rollouts);
        return rollouts;
    }

    @Override
    public Page<Rollout> findByRsql(final Pageable pageable, final String rsqlParam, final boolean deleted) {
        final List<Specification<JpaRollout>> specList = new ArrayList<>(2);
        specList.add(
                RSQLUtility.buildRsqlSpecification(rsqlParam, RolloutFields.class, virtualPropertyReplacer, database));
        specList.add(RolloutSpecification.isDeletedWithDistributionSet(deleted, pageable.getSort()));

        return JpaManagementHelper.findAllWithCountBySpec(rolloutRepository, pageable, specList);
    }

    @Override
    public Slice<Rollout> findByFiltersWithDetailedStatus(final Pageable pageable, final String searchText,
            final boolean deleted) {
        final Slice<Rollout> findAll = JpaManagementHelper.findAllWithoutCountBySpec(rolloutRepository, pageable,
                Collections.singletonList(RolloutSpecification.likeName(searchText, deleted)));
        setRolloutStatusDetails(findAll);
        return findAll;
    }

    @Override
    public List<Long> findActiveRollouts() {
        return rolloutRepository.findByStatusIn(ACTIVE_ROLLOUTS);
    }

    @Override
    public Optional<Rollout> get(final long rolloutId) {
        return rolloutRepository.findById(rolloutId).map(Rollout.class::cast);
    }

    @Override
    public Optional<Rollout> getByName(final String rolloutName) {
        return rolloutRepository.findByName(rolloutName);
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

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void pauseRollout(final long rolloutId) {
        final JpaRollout rollout = getRolloutOrThrowExceptionIfNotFound(rolloutId);
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
        final JpaRollout rollout = getRolloutOrThrowExceptionIfNotFound(rolloutId);
        if (RolloutStatus.PAUSED != rollout.getStatus()) {
            throw new RolloutIllegalStateException("Rollout can only be resumed in state paused but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
        rollout.setStatus(RolloutStatus.RUNNING);
        rolloutRepository.save(rollout);
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
        log.debug("approveOrDeny rollout called for rollout {} with decision {}", rolloutId, decision);
        final JpaRollout rollout = getRolloutOrThrowExceptionIfNotFound(rolloutId);
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
        log.debug("startRollout called for rollout {}", rolloutId);

        final JpaRollout rollout = getRolloutOrThrowExceptionIfNotFound(rolloutId);
        RolloutHelper.checkIfRolloutCanStarted(rollout, rollout);
        rollout.setStatus(RolloutStatus.STARTING);
        rollout.setLastCheck(0);
        return rolloutRepository.save(rollout);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout update(final RolloutUpdate u) {
        final GenericRolloutUpdate update = (GenericRolloutUpdate) u;
        final JpaRollout rollout = getRolloutOrThrowExceptionIfNotFound(update.getId());

        checkIfDeleted(update.getId(), rollout.getStatus());

        update.getName().ifPresent(rollout::setName);
        update.getDescription().ifPresent(rollout::setDescription);
        return rolloutRepository.save(rollout);
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

    @Override
    @Transactional
    public void cancelRolloutsForDistributionSet(final DistributionSet set) {
        // stop all rollouts for this distribution set
        rolloutRepository.findByDistributionSetAndStatusIn(set, ROLLOUT_STATUS_STOPPABLE).forEach(rollout -> {
            final JpaRollout jpaRollout = (JpaRollout) rollout;
            jpaRollout.setStatus(RolloutStatus.STOPPING);
            rolloutRepository.save(jpaRollout);
            log.debug("Rollout {} stopped", jpaRollout.getId());
        });
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void triggerNextGroup(final long rolloutId) {
        final JpaRollout rollout = getRolloutOrThrowExceptionIfNotFound(rolloutId);
        if (RolloutStatus.RUNNING != rollout.getStatus()) {
            throw new RolloutIllegalStateException("Rollout is not in running state");
        }
        final List<RolloutGroup> groups = rollout.getRolloutGroups();

        final boolean isNextGroupTriggerable = groups.stream()
                .anyMatch(g -> RolloutGroupStatus.SCHEDULED.equals(g.getStatus()));

        if (!isNextGroupTriggerable) {
            throw new RolloutIllegalStateException("Rollout does not have any groups left to be triggered");
        }

        final RolloutGroup latestRunning = groups.stream()
                .sorted(Comparator.comparingLong(RolloutGroup::getId).reversed())
                .filter(g -> RolloutGroupStatus.RUNNING.equals(g.getStatus()))
                .findFirst()
                .orElseThrow(() -> new RolloutIllegalStateException("No group is running"));

        startNextRolloutGroupAction.exec(rollout, latestRunning);
    }

    @Override
    public void setRolloutStatusDetails(final Slice<Rollout> rollouts) {
        final List<Long> rolloutIds = rollouts.getContent().stream().map(Rollout::getId).collect(Collectors.toList());
        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRollout(
                rolloutIds);

        if (allStatesForRollout != null) {
            rollouts.forEach(rollout -> {
                final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                        allStatesForRollout.get(rollout.getId()), rollout.getTotalTargets(), rollout.getActionType());
                ((JpaRollout) rollout).setTotalTargetCountStatus(totalTargetCountStatus);
            });
        }
    }

    /**
     * In case the given group is missing conditions or actions, they will be set
     * from the supplied default conditions.
     *
     * @param create group to check
     * @param conditions default conditions and actions
     */
    private static JpaRolloutGroup prepareRolloutGroupWithDefaultConditions(final RolloutGroupCreate create,
            final RolloutGroupConditions conditions) {
        final JpaRolloutGroup group = ((JpaRolloutGroupCreate) create).build();

        if (group.getSuccessCondition() == null) {
            group.setSuccessCondition(conditions.getSuccessCondition());
        }
        if (group.getSuccessConditionExp() == null) {
            group.setSuccessConditionExp(conditions.getSuccessConditionExp());
        }
        if (group.getSuccessAction() == null) {
            group.setSuccessAction(conditions.getSuccessAction());
        }
        if (group.getSuccessActionExp() == null) {
            group.setSuccessActionExp(conditions.getSuccessActionExp());
        }

        if (group.getErrorCondition() == null) {
            group.setErrorCondition(conditions.getErrorCondition());
        }
        if (group.getErrorConditionExp() == null) {
            group.setErrorConditionExp(conditions.getErrorConditionExp());
        }
        if (group.getErrorAction() == null) {
            group.setErrorAction(conditions.getErrorAction());
        }
        if (group.getErrorActionExp() == null) {
            group.setErrorActionExp(conditions.getErrorActionExp());
        }

        return group;
    }

    private static void checkIfDeleted(final Long rolloutId, final RolloutStatus status) {
        if (RolloutStatus.DELETING == status || RolloutStatus.DELETED == status) {
            throw new EntityReadOnlyException("Rollout " + rolloutId + " is soft deleted and cannot be changed");
        }
    }

    private JpaRollout createRollout(final JpaRollout rollout, final boolean pureDynamic) {
        WeightValidationHelper.usingContext(systemSecurityContext, tenantConfigurationManagement).validate(rollout);
        final JpaDistributionSet distributionSet = (JpaDistributionSet) rollout.getDistributionSet();

        if (pureDynamic) {
            rollout.setTotalTargets(0);
        } else {
            final long totalTargets;
            final String errMsg;
            if (RolloutHelper.isRolloutRetried(rollout.getTargetFilterQuery())) {
                totalTargets = targetManagement.countByFailedInRollout(
                        RolloutHelper.getIdFromRetriedTargetFilter(rollout.getTargetFilterQuery()),
                        distributionSet.getType().getId());
                errMsg = "No failed targets in Rollout";
            } else {
                totalTargets = targetManagement.countByRsqlAndCompatible(rollout.getTargetFilterQuery(),
                        distributionSet.getType().getId());
                errMsg = "Rollout does not match any existing targets";
            }
            if (totalTargets == 0) {
                throw new ValidationException(errMsg);
            }
            rollout.setTotalTargets(totalTargets);
        }

        if (((JpaDistributionSetManagement) distributionSetManagement).isImplicitLockApplicable(distributionSet)) {
            distributionSetManagement.lock(distributionSet.getId());
        }

        if (rollout.getWeight().isEmpty()) {
            rollout.setWeight(repositoryProperties.getActionWeightIfAbsent());
        }
        contextAware.getCurrentContext().ifPresent(rollout::setAccessControlContext);
        return rolloutRepository.save(rollout);
    }

    private Rollout createRolloutGroups(final int amountOfGroups, final RolloutGroupConditions conditions,
            final JpaRollout rollout, final boolean isConfirmationRequired, final DynamicRolloutGroupTemplate dynamicRolloutGroupTemplate) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.CREATING);
        RolloutHelper.verifyRolloutGroupConditions(conditions);

        JpaRolloutGroup lastSavedGroup = null;
        if (amountOfGroups == 0) {
            if (dynamicRolloutGroupTemplate == null) {
                throw new ConstraintDeclarationException(
                        "At least one static rollout group must be defined for a static rollout");
            }
        } else {
            // we can enforce the 'max targets per group' quota right here because
            // we want to distribute the targets equally to the different groups
            assertTargetsPerRolloutGroupQuota(rollout.getTotalTargets() / amountOfGroups);

            for (int i = 0; i < amountOfGroups; i++) {
                final String nameAndDesc = "group-" + (i + 1);
                final JpaRolloutGroup group = new JpaRolloutGroup();
                group.setName(nameAndDesc);
                group.setDescription(nameAndDesc);
                group.setRollout(rollout);
                group.setParent(lastSavedGroup);
                group.setStatus(RolloutGroupStatus.CREATING);
                group.setConfirmationRequired(isConfirmationRequired);

                addSuccessAndErrorConditionsAndActions(group, conditions);

                // total percent of the all devices. Before, it was relative percent -
                // the percent of the "rest" of the devices. Thus, if you have
                // first a group 10% (the rest is 90%) and the second group is 50%
                // then the percent would be 50% of 90% - 45%.
                // This is very unintuitive and is switched in order to be interpreted easier.
                // the "new style" (vs "old style") rollouts could be detected by
                // JpaRollout#isNewStyleTargetPercent (which uses that old style rollouts
                // have null as dynamic
                group.setTargetPercentage(100.0F / amountOfGroups);

                lastSavedGroup = rolloutGroupRepository.save(group);
                publishRolloutGroupCreatedEventAfterCommit(lastSavedGroup, rollout);
            }
        }

        if (dynamicRolloutGroupTemplate != null && rollout.isDynamic()) { // if not null then it is a dynamic rollout (already validated), but for sure
            // create first template rollout group
            final String nameAndDesc = "group-" + (amountOfGroups + 1) + dynamicRolloutGroupTemplate.getNameSuffix();
            final JpaRolloutGroup group = new JpaRolloutGroup();
            group.setName(nameAndDesc);
            group.setDescription(nameAndDesc);
            group.setRollout(rollout);
            group.setParent(lastSavedGroup);
            group.setDynamic(true);
            group.setStatus(RolloutGroupStatus.READY);
            group.setConfirmationRequired(isConfirmationRequired);

            addSuccessAndErrorConditionsAndActions(group, conditions);

            // for dynamic groups the target count is kept in target percentage
            group.setTargetPercentage(dynamicRolloutGroupTemplate.getTargetCount());

            lastSavedGroup = rolloutGroupRepository.save(group);
            publishRolloutGroupCreatedEventAfterCommit(lastSavedGroup, rollout);
        }

        // lastSavedGroup is never null! amountOfGroups > 0 (and has static groups) or dynamicRolloutGroupTemplate is
        // not null (validated) and (validated) the rollout is dynamic, so has dynamic group
        rollout.setRolloutGroupsCreated(lastSavedGroup.isDynamic() ? amountOfGroups + 1 : amountOfGroups);
        return rolloutRepository.save(rollout);
    }

    private Rollout createRolloutGroups(final List<RolloutGroupCreate> groupList,
            final RolloutGroupConditions conditions, final Rollout rollout) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.CREATING);
        final JpaRollout savedRollout = (JpaRollout) rollout;
        final DistributionSetType distributionSetType = savedRollout.getDistributionSet().getType();

        // prepare the groups
        final List<RolloutGroup> groups = groupList.stream()
                .map(group -> prepareRolloutGroupWithDefaultConditions(group, conditions)).collect(Collectors.toList());
        groups.forEach(RolloutHelper::verifyRolloutGroupHasConditions);

        RolloutHelper.verifyRemainingTargets(calculateRemainingTargets(groups, savedRollout.getTargetFilterQuery(),
                savedRollout.getCreatedAt(), distributionSetType.getId()));

        // check if we need to enforce the 'max targets per group' quota
        if (quotaManagement.getMaxTargetsPerRolloutGroup() > 0) {
            validateTargetsInGroups(groups, savedRollout.getTargetFilterQuery(), savedRollout.getCreatedAt(),
                    distributionSetType.getId()).getTargetsPerGroup().forEach(this::assertTargetsPerRolloutGroupQuota);
        }

        // create and persist the groups (w/o filling them with targets)
        JpaRolloutGroup lastSavedGroup = null;
        for (final RolloutGroup srcGroup : groups) {
            final JpaRolloutGroup group = new JpaRolloutGroup();
            group.setName(srcGroup.getName());
            group.setDescription(srcGroup.getDescription());
            group.setRollout(savedRollout);
            group.setParent(lastSavedGroup);
            group.setStatus(RolloutGroupStatus.CREATING);
            group.setConfirmationRequired(srcGroup.isConfirmationRequired());

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

    private JpaRollout getRolloutOrThrowExceptionIfNotFound(final Long rolloutId) {
        return rolloutRepository.findById(rolloutId)
                .orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
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

    /**
     * Enforces the quota defining the maximum number of {@link Target}s per
     * {@link RolloutGroup}.
     *
     * @param requested number of targets to check
     */
    private void assertTargetsPerRolloutGroupQuota(final long requested) {
        final int quota = quotaManagement.getMaxTargetsPerRolloutGroup();
        QuotaHelper.assertAssignmentQuota(requested, quota, Target.class, RolloutGroup.class);
    }

    private RolloutGroupsValidation validateTargetsInGroups(final List<RolloutGroup> groups, final String baseFilter,
            final long totalTargets, final Long dsTypeId) {
        final List<Long> groupTargetCounts = new ArrayList<>(groups.size());
        Map<String, Long> targetFilterCounts;
        if (!RolloutHelper.isRolloutRetried(baseFilter)) {
            targetFilterCounts = groups.stream()
                    .map(group -> RolloutHelper.getGroupTargetFilter(baseFilter, group)).distinct()
                    .collect(Collectors.toMap(Function.identity(),
                            groupTargetFilter -> targetManagement.countByRsqlAndCompatible(groupTargetFilter, dsTypeId)));
        } else {
            targetFilterCounts = groups.stream()
                    .map(group -> RolloutHelper.getGroupTargetFilter(baseFilter, group)).distinct()
                    .collect(Collectors.toMap(Function.identity(),
                            groupTargetFilter -> targetManagement.countByFailedInRollout(
                                    RolloutHelper.getIdFromRetriedTargetFilter(baseFilter), dsTypeId)));
        }

        long unusedTargetsCount = 0;

        for (int i = 0; i < groups.size(); i++) {
            final RolloutGroup group = groups.get(i);
            final String groupTargetFilter = RolloutHelper.getGroupTargetFilter(baseFilter, group);
            RolloutHelper.verifyRolloutGroupTargetPercentage(group.getTargetPercentage());

            final long targetsInGroupFilter = targetFilterCounts.get(groupTargetFilter);
            final long overlappingTargets = countOverlappingTargetsWithPreviousGroups(baseFilter, groups, group, i,
                    targetFilterCounts);

            final long realTargetsInGroup;
            // Assume that targets which were not used in the previous groups
            // are used in this group
            if (overlappingTargets > 0 && unusedTargetsCount > 0) {
                realTargetsInGroup = targetsInGroupFilter - overlappingTargets + unusedTargetsCount;
                unusedTargetsCount = 0;
            } else {
                realTargetsInGroup = targetsInGroupFilter - overlappingTargets;
            }

            // new style percent - total percent
            final double percentFromRest = RolloutHelper.toPercentFromTheRest(group, groups);

            final long reducedTargetsInGroup = Math
                    .round(percentFromRest / 100 * (double) realTargetsInGroup);
            groupTargetCounts.add(reducedTargetsInGroup);
            unusedTargetsCount += realTargetsInGroup - reducedTargetsInGroup;
        }

        return new RolloutGroupsValidation(totalTargets, groupTargetCounts);
    }

    private long countOverlappingTargetsWithPreviousGroups(final String baseFilter, final List<RolloutGroup> groups,
            final RolloutGroup group, final int groupIndex, final Map<String, Long> targetFilterCounts) {
        // there can't be overlapping targets in the first group
        if (groupIndex == 0) {
            return 0;
        }
        final List<RolloutGroup> previousGroups = groups.subList(0, groupIndex);
        final String overlappingTargetsFilter = RolloutHelper.getOverlappingWithGroupsTargetFilter(baseFilter,
                previousGroups, group);

        if (targetFilterCounts.containsKey(overlappingTargetsFilter)) {
            return targetFilterCounts.get(overlappingTargetsFilter);
        } else {
            final long overlappingTargets = targetManagement.countByRsql(overlappingTargetsFilter);
            targetFilterCounts.put(overlappingTargetsFilter, overlappingTargets);
            return overlappingTargets;
        }
    }

    private long calculateRemainingTargets(final List<RolloutGroup> groups, final String targetFilter,
            final Long createdAt, final Long dsTypeId) {

        final TargetCount targets = calculateTargets(targetFilter, createdAt, dsTypeId);
        long totalTargets = targets.total();
        final String baseFilter = targets.filter();

        if (totalTargets == 0) {
            throw new ConstraintDeclarationException("Rollout target filter does not match any targets");
        }

        final RolloutGroupsValidation validation = validateTargetsInGroups(groups, baseFilter, totalTargets, dsTypeId);

        return totalTargets - validation.getTargetsInGroups();
    }

    private TargetCount calculateTargets(final String targetFilter, final Long createdAt, final Long dsTypeId) {
        String baseFilter;
        long totalTargets;
        if (!RolloutHelper.isRolloutRetried(targetFilter)) {
            baseFilter = RolloutHelper.getTargetFilterQuery(targetFilter, createdAt);
            totalTargets = targetManagement.countByRsqlAndCompatible(baseFilter, dsTypeId);
        } else {
            totalTargets = targetManagement.countByFailedInRollout(
                    RolloutHelper.getIdFromRetriedTargetFilter(targetFilter), dsTypeId);
            baseFilter = targetFilter;
        }

        return new TargetCount(totalTargets, baseFilter);
    }

    private record TargetCount(long total, String filter) {}
}
