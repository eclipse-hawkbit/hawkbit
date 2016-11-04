/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.RolloutFields;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.exception.RolloutVerificationException;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout_;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupActionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupConditionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link RolloutManagement}.
 */
@Validated
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public class JpaRolloutManagement implements RolloutManagement {
    private static final Logger LOGGER = LoggerFactory.getLogger(RolloutManagement.class);

    /**
     * Maximum amount of targets that are assigned to a Rollout Group in one transaction.
     */
    private static final long TRANSACTION_TARGETS = 1000;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private DeploymentManagement deploymentManagement;

    @Autowired
    private RolloutTargetGroupRepository rolloutTargetGroupRepository;

    @Autowired
    private RolloutGroupManagement rolloutGroupManagement;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private NoCountPagingRepository criteriaNoCountDao;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor executor;

    /*
     * set which stores the rollouts which are asynchronously starting. This is
     * necessary to verify rollouts which maybe stuck during starting e.g.
     * because of database interruption, failures or even application crash.
     * !This is not cluster aware!
     */
    private static final Set<String> startingRollouts = ConcurrentHashMap.newKeySet();

    @Override
    public Page<Rollout> findAll(final Pageable pageable) {
        return convertPage(rolloutRepository.findAll(pageable), pageable);
    }

    private static Page<Rollout> convertPage(final Page<JpaRollout> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    private static Slice<Rollout> convertPage(final Slice<JpaRollout> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, 0);
    }

    @Override
    public Page<Rollout> findAllWithDetailedStatusByPredicate(final String rsqlParam, final Pageable pageable) {

        final Specification<JpaRollout> specification = RSQLUtility.parse(rsqlParam, RolloutFields.class,
                virtualPropertyReplacer);

        final Page<JpaRollout> findAll = rolloutRepository.findAll(specification, pageable);
        setRolloutStatusDetails(findAll);
        return convertPage(findAll, pageable);
    }

    @Override
    public Rollout findRolloutById(final Long rolloutId) {
        return rolloutRepository.findOne(rolloutId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public Rollout createRollout(final Rollout rollout, final int amountGroup,
            final RolloutGroupConditions conditions) {
        RolloutHelper.verifyRolloutGroupParameter(amountGroup);
        final JpaRollout savedRollout = createRollout((JpaRollout) rollout);
        return createRolloutGroups(amountGroup, conditions, savedRollout);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public Rollout createRollout(final Rollout rollout,
                                 final List<RolloutGroup> groups,
                                 final RolloutGroupConditions conditions) {
        final JpaRollout savedRollout = createRollout((JpaRollout) rollout);
        if(groups != null) {
            return createRolloutGroups(groups, conditions, rollout);
        } else {
            return savedRollout;
        }
    }

    private JpaRollout createRollout(final JpaRollout rollout) {
        JpaRollout existingRollout = rolloutRepository.findByName(rollout.getName());
        if(existingRollout != null) {
            throw new EntityAlreadyExistsException(existingRollout.getName());
        }

        final Long totalTargets = targetManagement.countTargetByTargetFilterQuery(rollout.getTargetFilterQuery());
        if(totalTargets == 0) {
            throw new RolloutVerificationException("Rollout does not match any existing targets");
        }
        rollout.setTotalTargets(totalTargets);
        return rolloutRepository.save(rollout);
    }

    private Rollout createRolloutGroups(final int amountOfGroups, final RolloutGroupConditions conditions,
            final Rollout rollout) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.CREATING);
        RolloutHelper.verifyRolloutGroupConditions(conditions);

        final JpaRollout savedRollout = (JpaRollout) rollout;

        RolloutGroup lastSavedGroup = null;
        for (int i = 0; i < amountOfGroups; i++) {
            final String nameAndDesc = "group-" + (i + 1);
            final JpaRolloutGroup group = new JpaRolloutGroup();
            group.setName(nameAndDesc);
            group.setDescription(nameAndDesc);
            group.setRollout(savedRollout);
            group.setParent(lastSavedGroup);
            group.setStatus(RolloutGroupStatus.CREATING);

            group.setSuccessCondition(conditions.getSuccessCondition());
            group.setSuccessConditionExp(conditions.getSuccessConditionExp());

            group.setSuccessAction(conditions.getSuccessAction());
            group.setSuccessActionExp(conditions.getSuccessActionExp());

            group.setErrorCondition(conditions.getErrorCondition());
            group.setErrorConditionExp(conditions.getErrorConditionExp());

            group.setErrorAction(conditions.getErrorAction());
            group.setErrorActionExp(conditions.getErrorActionExp());

            group.setTargetPercentage(1.0F / (amountOfGroups - i) * 100);

            lastSavedGroup = rolloutGroupRepository.save(group);
            publishRolloutGroupCreatedEventAfterCommit(lastSavedGroup);

        }

        savedRollout.setRolloutGroupsCreated(amountOfGroups);
        return rolloutRepository.save(savedRollout);
    }

    private Rollout createRolloutGroups(final List<RolloutGroup> groupList, final RolloutGroupConditions conditions,
            final Rollout rollout) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.CREATING);
        final JpaRollout savedRollout = (JpaRollout) rollout;

        // Preparing the groups
        final List<RolloutGroup> groups = groupList.stream().map(
                group -> RolloutHelper.prepareRolloutGroupWithDefaultConditions(group, conditions))
                .collect(Collectors.toList());
        groups.forEach(RolloutHelper::verifyRolloutGroupHasConditions);

        verifyRolloutGroupTargetCounts(groups, savedRollout);

        // Persisting the groups
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

            group.setSuccessCondition(srcGroup.getSuccessCondition());
            group.setSuccessConditionExp(srcGroup.getSuccessConditionExp());

            group.setSuccessAction(srcGroup.getSuccessAction());
            group.setSuccessActionExp(srcGroup.getSuccessActionExp());

            group.setErrorCondition(srcGroup.getErrorCondition());
            group.setErrorConditionExp(srcGroup.getErrorConditionExp());

            group.setErrorAction(srcGroup.getErrorAction());
            group.setErrorActionExp(srcGroup.getErrorActionExp());

            lastSavedGroup = rolloutGroupRepository.save(group);
            publishRolloutGroupCreatedEventAfterCommit(lastSavedGroup);
        }

        savedRollout.setRolloutGroupsCreated(groups.size());
        return rolloutRepository.save(savedRollout);
    }

    private void publishRolloutGroupCreatedEventAfterCommit(final RolloutGroup group) {
        afterCommit
                .afterCommit(() -> eventPublisher.publishEvent(new RolloutGroupCreatedEvent(group, context.getId())));
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public void fillRolloutGroupsWithTargets(final Rollout rollout) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.CREATING);
        final JpaRollout jpaRollout = (JpaRollout) rollout;

        List<RolloutGroup> rolloutGroups = RolloutHelper.getOrderedGroups(rollout);
        int readyGroups = 0;
        int totalTargets = 0;
        for (RolloutGroup group : rolloutGroups) {
            if (group.getStatus() != RolloutGroupStatus.CREATING) {
                readyGroups++;
                totalTargets += group.getTotalTargets();
                continue;
            }

            group = fillRolloutGroupWithTargets(rollout, group);
            if(group.getStatus() == RolloutGroupStatus.READY) {
                readyGroups++;
                totalTargets += group.getTotalTargets();
            }
        }

        // When all groups are ready the rollout status can be changed to be ready, too.
        if(readyGroups == rolloutGroups.size()) {
            jpaRollout.setStatus(RolloutStatus.READY);
            jpaRollout.setTotalTargets(totalTargets);
            rolloutRepository.save(jpaRollout);
        }
    }

    private RolloutGroup fillRolloutGroupWithTargets(final Rollout rollout, final RolloutGroup group1) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.CREATING);

        JpaRolloutGroup group = (JpaRolloutGroup) group1;

        final String baseFilter = RolloutHelper.getTargetFilterQuery(rollout);
        final String groupTargetFilter;
        if (StringUtils.isNotEmpty(group.getTargetFilterQuery())) {
            groupTargetFilter = baseFilter + ";" + group.getTargetFilterQuery();
        } else {
            groupTargetFilter = baseFilter;
        }

        final List<RolloutGroup> readyGroups = RolloutHelper.getGroupsByStatusIncludingGroup(rollout,
                RolloutGroupStatus.READY, group);

        final long targetsInGroupFilter = targetManagement
                .countAllTargetsByTargetFilterQueryAndNotInRolloutGroups(readyGroups, groupTargetFilter);
        final long expectedInGroup = Math.round(group.getTargetPercentage() / 100 * (double) targetsInGroupFilter);
        final long currentlyInGroup = rolloutTargetGroupRepository.countByRolloutGroup(group);

        // Switch the Group status to READY, when there are enough Targets in
        // the Group
        if (currentlyInGroup >= expectedInGroup) {
            group.setStatus(RolloutGroupStatus.READY);
            return rolloutGroupRepository.save(group);
        }

        long targetsLeftToAdd = expectedInGroup - currentlyInGroup;

        try {
            do {
                // Add up to TRANSACTION_TARGETS of the left targets
                // In case a TransactionException is thrown this loop aborts
                targetsLeftToAdd -= assignTargetsToGroupInNewTransaction(rollout, group, groupTargetFilter,
                        Math.min(TRANSACTION_TARGETS, targetsLeftToAdd));
            } while (targetsLeftToAdd > 0);

            group.setStatus(RolloutGroupStatus.READY);
            group.setTotalTargets(rolloutTargetGroupRepository.countByRolloutGroup(group).intValue());
            return rolloutGroupRepository.save(group);

        } catch (TransactionException e) {
            LOGGER.warn("Transaction assigning Targets to RolloutGroup failed", e);
            return group;
        }
    }

    private long assignTargetsToGroupInNewTransaction(final Rollout rollout, final RolloutGroup group,
            final String targetFilter, final long limit) {
        final PageRequest pageRequest = new PageRequest(0, Math.toIntExact(limit));
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("assignTargetsToRolloutGroup");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new TransactionTemplate(txManager, def).execute(status -> {

            final List<RolloutGroup> readyGroups = RolloutHelper.getGroupsByStatusIncludingGroup(rollout,
                    RolloutGroupStatus.READY, group);

            Page<Target> targets = targetManagement.findAllTargetsByTargetFilterQueryAndNotInRolloutGroups(pageRequest,
                    readyGroups, targetFilter);

            targets.forEach(target -> rolloutTargetGroupRepository.save(new RolloutTargetGroup(group, target)));

            return targets.getTotalElements();
        });
    }

    private void verifyRolloutGroupTargetCounts(final List<RolloutGroup> groups, final JpaRollout rollout) {
        final String baseFilter = RolloutHelper.getTargetFilterQuery(rollout);
        final long totalTargets = targetManagement.countTargetByTargetFilterQuery(baseFilter);
        if (totalTargets == 0) {
            throw new RolloutVerificationException("Rollout target filter does not match any targets");
        }

        long targetCount = totalTargets;
        long unusedTargetsCount = 0;

        for (int i = 0; i < groups.size(); i++) {
            final RolloutGroup group = groups.get(i);
            RolloutHelper.verifyRolloutGroupTargetPercentage(group.getTargetPercentage());

            final long targetsInGroupFilter = countTargetsOfGroup(baseFilter, totalTargets, group);
            final long overlappingTargets = countOverlappingTargetsWithPreviousGroups(baseFilter, groups, group, i);

            final long realTargetsInGroup;
            // Assume that targets which were not used in the previous groups are used in this group
            if (overlappingTargets > 0 && unusedTargetsCount > 0) {
                realTargetsInGroup = targetsInGroupFilter - overlappingTargets + unusedTargetsCount;
                unusedTargetsCount = 0;
            } else {
                realTargetsInGroup = targetsInGroupFilter - overlappingTargets;
            }

            final long reducedTargetsInGroup = Math
                    .round(group.getTargetPercentage() / 100 * (double) realTargetsInGroup);
            targetCount -= reducedTargetsInGroup;
            unusedTargetsCount += realTargetsInGroup - reducedTargetsInGroup;

        }

        RolloutHelper.verifyRemainingTargets(targetCount);

    }

    private long countTargetsOfGroup(final String baseFilter, final long baseFilterCount, final RolloutGroup group) {
        if (StringUtils.isNotEmpty(group.getTargetFilterQuery())) {
            return targetManagement
                    .countTargetByTargetFilterQuery(baseFilter + ";" + group.getTargetFilterQuery());
        } else {
            return baseFilterCount;
        }
    }

    private long countOverlappingTargetsWithPreviousGroups(final String baseFilter, final List<RolloutGroup> groups,
            final RolloutGroup group, final int groupIndex) {
        // there can't be overlapping targets in the first group
        if(groupIndex == 0) {
            return 0;
        }
        final List<RolloutGroup> previousGroups = groups.subList(0, groupIndex);
        String overlappingTargetsFilter = RolloutHelper.getOverlappingWithGroupsTargetFilter(previousGroups, group);
        if (StringUtils.isNotEmpty(overlappingTargetsFilter)) {
            overlappingTargetsFilter = baseFilter + ";" + overlappingTargetsFilter;
        } else {
            overlappingTargetsFilter = baseFilter;
        }
        return targetManagement.countTargetByTargetFilterQuery(overlappingTargetsFilter);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public Rollout startRollout(final Rollout rollout) {
        final JpaRollout mergedRollout = entityManager.merge((JpaRollout) rollout);
        checkIfRolloutCanStarted(rollout, mergedRollout);
        return doStartRollout(mergedRollout);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public Rollout startRolloutAsync(final Rollout rollout) {
        final JpaRollout mergedRollout = entityManager.merge((JpaRollout) rollout);
        checkIfRolloutCanStarted(rollout, mergedRollout);
        mergedRollout.setStatus(RolloutStatus.STARTING);
        final JpaRollout updatedRollout = rolloutRepository.save(mergedRollout);
        startingRollouts.add(updatedRollout.getName());
        executor.execute(() -> {
            try {
                final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
                def.setName("startingRollout");
                def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                new TransactionTemplate(txManager, def).execute(status -> {
                    doStartRollout(updatedRollout);
                    return null;
                });
            } finally {
                startingRollouts.remove(updatedRollout.getName());
            }
        });
        return updatedRollout;

    }

    private Rollout doStartRollout(final JpaRollout rollout) {
        final DistributionSet distributionSet = rollout.getDistributionSet();
        final ActionType actionType = rollout.getActionType();
        final long forceTime = rollout.getForcedTime();
        final List<JpaRolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutOrderByIdAsc(rollout);
        for (int iGroup = 0; iGroup < rolloutGroups.size(); iGroup++) {
            final JpaRolloutGroup rolloutGroup = rolloutGroups.get(iGroup);
            final List<Target> targetGroup = targetRepository.findByRolloutTargetGroupRolloutGroup(rolloutGroup);
            if(targetGroup.isEmpty()) {
                rolloutGroup.setStatus(RolloutGroupStatus.FINISHED);

            } else if (iGroup == 0) {
                // first group can already be started
                final List<TargetWithActionType> targetsWithActionType = targetGroup.stream()
                        .map(t -> new TargetWithActionType(t.getControllerId(), actionType, forceTime))
                        .collect(Collectors.toList());
                deploymentManagement.assignDistributionSet(distributionSet.getId(), targetsWithActionType, rollout,
                        rolloutGroup);
                rolloutGroup.setStatus(RolloutGroupStatus.RUNNING);

            } else {
                // create only not active actions with status scheduled so they
                // can be activated later
                deploymentManagement.createScheduledAction(targetGroup, distributionSet, actionType, forceTime, rollout,
                        rolloutGroup);
                rolloutGroup.setStatus(RolloutGroupStatus.SCHEDULED);
            }
            rolloutGroupRepository.save(rolloutGroup);
        }
        rollout.setStatus(RolloutStatus.RUNNING);
        return rolloutRepository.save(rollout);
    }

    private void ensureAllGroupsAreScheduled(final Rollout rollout) {
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.STARTING);
        final JpaRollout jpaRollout = (JpaRollout) rollout;

        final List<JpaRolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutOrderByIdAsc(jpaRollout);
        long scheduledGroups = 0;
        for (int iGroup = 0; iGroup < rolloutGroups.size(); iGroup++) {
            final JpaRolloutGroup group = rolloutGroups.get(iGroup);
            if(group.getStatus() != RolloutGroupStatus.READY) {
                scheduledGroups++;
                continue;
            }

            final long targetsInGroup = rolloutTargetGroupRepository.countByRolloutGroup(group);
            final long countOfActions = actionRepository.countByRolloutAndRolloutGroup(jpaRollout, group);
            long actionsLeft = targetsInGroup - countOfActions;
            if(actionsLeft > 0 ) {
                actionsLeft -= createActionsForRolloutGroup(rollout, group);
            }

            if(actionsLeft <= 0) {
                group.setStatus(RolloutGroupStatus.SCHEDULED);
                rolloutGroupRepository.save(group);
                scheduledGroups++;
            }
        }

        if(scheduledGroups == rolloutGroups.size()) {

            // TODO set all actions of first group ACTIVE and set group status to RUNNING

            jpaRollout.setStatus(RolloutStatus.RUNNING);
            rolloutRepository.save(jpaRollout);
        }
    }

    private long createActionsForRolloutGroup(final Rollout rollout, final RolloutGroup group) {
        long totalActionsCreated = 0;
        try {
            long actionsCreated;
            do {
                actionsCreated = createActionsForTargetsInNewTransaction(rollout, group, TRANSACTION_TARGETS);
                totalActionsCreated += actionsCreated;
            } while(actionsCreated > 0);

        } catch (TransactionException e) {
            LOGGER.warn("Transaction assigning Targets to RolloutGroup failed", e);
            return 0;
        }
        return totalActionsCreated;
    }

    private long createActionsForTargetsInNewTransaction(final Rollout rollout, final RolloutGroup group,
            final long limit) {
        final DistributionSet distributionSet = rollout.getDistributionSet();
        final ActionType actionType = rollout.getActionType();
        final long forceTime = rollout.getForcedTime();

        final PageRequest pageRequest = new PageRequest(0, Math.toIntExact(limit));
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("assignTargetsToRolloutGroup");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new TransactionTemplate(txManager, def).execute(status -> {

            Page<Target> targets = targetManagement.findAllTargetsInRolloutGroupWithoutAction(pageRequest, group);
            deploymentManagement.createScheduledAction(targets.getContent(), distributionSet, actionType, forceTime,
                    rollout, group);

            return targets.getTotalElements();
        });
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public void pauseRollout(final Rollout rollout) {
        final JpaRollout mergedRollout = entityManager.merge((JpaRollout) rollout);
        if (mergedRollout.getStatus() != RolloutStatus.RUNNING) {
            throw new RolloutIllegalStateException("Rollout can only be paused in state running but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
        // setting the complete rollout only in paused state. This is sufficient
        // due the currently running groups will be completed and new groups are
        // not started until rollout goes back to running state again. The
        // periodically check for running rollouts will skip rollouts in pause
        // state.
        mergedRollout.setStatus(RolloutStatus.PAUSED);
        rolloutRepository.save(mergedRollout);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public void resumeRollout(final Rollout rollout) {
        final JpaRollout mergedRollout = entityManager.merge((JpaRollout) rollout);
        if (!(RolloutStatus.PAUSED.equals(mergedRollout.getStatus()))) {
            throw new RolloutIllegalStateException("Rollout can only be resumed in state paused but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
        mergedRollout.setStatus(RolloutStatus.RUNNING);
        rolloutRepository.save(mergedRollout);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public void checkRunningRollouts(final long delayBetweenChecks) {
        final long lastCheck = System.currentTimeMillis();
        final int updated = rolloutRepository.updateLastCheck(lastCheck, delayBetweenChecks, RolloutStatus.RUNNING);

        if (updated == 0) {
            // nothing to check, maybe another instance already checked in
            // between
            LOGGER.debug("No rolloutcheck necessary for current scheduled check {}, next check at {}", lastCheck,
                    lastCheck + delayBetweenChecks);
            return;
        }

        final List<JpaRollout> rolloutsToCheck = rolloutRepository.findByLastCheckAndStatus(lastCheck,
                RolloutStatus.RUNNING);
        LOGGER.info("Found {} running rollouts to check", rolloutsToCheck.size());

        for (final JpaRollout rollout : rolloutsToCheck) {
            LOGGER.debug("Checking rollout {}", rollout);

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
                LOGGER.info("Rollout {} is finished, setting finished status", rollout);
                rollout.setStatus(RolloutStatus.FINISHED);
                rolloutRepository.save(rollout);
            }
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
        final JpaRolloutGroup jpaRolloutGroup = rolloutGroup;
        jpaRolloutGroup.setTotalTargets((int) countTargetsOfRolloutGroup);
        rolloutRepository.save(jpaRollout);
        rolloutGroupRepository.save(jpaRolloutGroup);
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
        final Long groupsActiveLeft = rolloutGroupRepository.countByRolloutAndStatusOrStatus(rollout,
                RolloutGroupStatus.RUNNING, RolloutGroupStatus.SCHEDULED);
        return groupsActiveLeft == 0;
    }

    private boolean isRolloutGroupComplete(final JpaRollout rollout, final JpaRolloutGroup rolloutGroup) {
        final Long actionsLeftForRollout = actionRepository
                .countByRolloutAndRolloutGroupAndStatusNotAndStatusNotAndStatusNot(rollout, rolloutGroup,
                        Action.Status.ERROR, Action.Status.FINISHED, Action.Status.CANCELED);
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
                LOGGER.info("Rolloutgroup {} is finished, starting next group", rolloutGroup);
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
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public void checkCreatingRollouts(long delayBetweenChecks) {
        final long lastCheck = System.currentTimeMillis();
        final int updated = rolloutRepository.updateLastCheck(lastCheck, delayBetweenChecks, RolloutStatus.CREATING);
        if (updated == 0) {
            // nothing to check, maybe another instance already checked in
            // between
            LOGGER.debug("No rollouts creating check necessary for current scheduled check {}, next check at {}", lastCheck,
                    lastCheck + delayBetweenChecks);
            return;
        }

        final List<JpaRollout> rolloutsToCheck = rolloutRepository.findByLastCheckAndStatus(lastCheck,
                RolloutStatus.CREATING);
        LOGGER.info("Found {} creating rollouts to check", rolloutsToCheck.size());

        rolloutsToCheck.forEach(this::fillRolloutGroupsWithTargets);

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public void checkStartingRollouts(long delayBetweenChecks) {
        final long lastCheck = System.currentTimeMillis();
        final int updated = rolloutRepository.updateLastCheck(lastCheck, delayBetweenChecks, RolloutStatus.STARTING);
        if (updated == 0) {
            // nothing to check, maybe another instance already checked in
            // between
            LOGGER.debug("No rollouts starting check necessary for current scheduled check {}, next check at {}", lastCheck,
                    lastCheck + delayBetweenChecks);
            return;
        }

        final List<JpaRollout> rolloutsToCheck = rolloutRepository.findByLastCheckAndStatus(lastCheck,
                RolloutStatus.STARTING);
        LOGGER.info("Found {} starting rollouts to check", rolloutsToCheck.size());

        rolloutsToCheck.forEach(this::ensureAllGroupsAreScheduled);

    }

    @Override
    public Long countRolloutsAll() {
        return rolloutRepository.count();
    }

    @Override
    public Long countRolloutsAllByFilters(final String searchText) {
        return rolloutRepository.count(likeNameOrDescription(searchText));
    }

    private static Specification<JpaRollout> likeNameOrDescription(final String searchText) {
        return (rolloutRoot, query, criteriaBuilder) -> {
            final String searchTextToLower = searchText.toLowerCase();
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(rolloutRoot.get(JpaRollout_.name)), searchTextToLower),
                    criteriaBuilder.like(criteriaBuilder.lower(rolloutRoot.get(JpaRollout_.description)),
                            searchTextToLower));
        };
    }

    @Override
    public Slice<Rollout> findRolloutByFilters(final Pageable pageable, final String searchText) {
        final Specification<JpaRollout> specs = likeNameOrDescription(searchText);
        final Slice<JpaRollout> findAll = criteriaNoCountDao.findAll(specs, pageable, JpaRollout.class);
        setRolloutStatusDetails(findAll);
        return convertPage(findAll, pageable);
    }

    @Override
    public Rollout findRolloutByName(final String rolloutName) {
        return rolloutRepository.findByName(rolloutName);
    }

    /**
     * Update rollout details.
     *
     * @param rollout
     *            rollout to be updated
     *
     * @return Rollout updated rollout
     */
    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public Rollout updateRollout(final Rollout rollout) {
        Assert.notNull(rollout.getId());
        return rolloutRepository.save((JpaRollout) rollout);
    }

    /**
     * Get count of targets in different status in rollout.
     *
     * @param pageable
     *            the page request to sort and limit the result
     * @return a list of rollouts with details of targets count for different
     *         statuses
     *
     */
    @Override
    public Page<Rollout> findAllRolloutsWithDetailedStatus(final Pageable pageable) {
        final Page<JpaRollout> rollouts = rolloutRepository.findAll(pageable);
        setRolloutStatusDetails(rollouts);
        return convertPage(rollouts, pageable);

    }

    @Override
    public Rollout findRolloutWithDetailedStatus(final Long rolloutId) {
        final Rollout rollout = findRolloutById(rolloutId);
        final List<TotalTargetCountActionStatus> rolloutStatusCountItems = actionRepository
                .getStatusCountByRolloutId(rolloutId);
        final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(rolloutStatusCountItems,
                rollout.getTotalTargets());
        ((JpaRollout) rollout).setTotalTargetCountStatus(totalTargetCountStatus);
        return rollout;
    }

    private Map<Long, List<TotalTargetCountActionStatus>> getStatusCountItemForRollout(final List<Long> rolloutIds) {
        final List<TotalTargetCountActionStatus> resultList = actionRepository.getStatusCountByRolloutId(rolloutIds);
        return resultList.stream().collect(Collectors.groupingBy(TotalTargetCountActionStatus::getId));
    }

    private void setRolloutStatusDetails(final Slice<JpaRollout> rollouts) {
        final List<Long> rolloutIds = rollouts.getContent().stream().map(rollout -> rollout.getId())
                .collect(Collectors.toList());
        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRollout(
                rolloutIds);

        for (final Rollout rollout : rollouts) {
            final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                    allStatesForRollout.get(rollout.getId()), rollout.getTotalTargets());
            ((JpaRollout) rollout).setTotalTargetCountStatus(totalTargetCountStatus);
        }
    }

    private static void checkIfRolloutCanStarted(final Rollout rollout, final Rollout mergedRollout) {
        if (!(RolloutStatus.READY.equals(mergedRollout.getStatus()))) {
            throw new RolloutIllegalStateException("Rollout can only be started in state ready but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
    }

    @Override
    public float getFinishedPercentForRunningGroup(final Long rolloutId, final RolloutGroup rolloutGroup) {
        final long totalGroup = rolloutGroup.getTotalTargets();
        final Long finished = actionRepository.countByRolloutIdAndRolloutGroupIdAndStatus(rolloutId,
                rolloutGroup.getId(), Action.Status.FINISHED);
        if (totalGroup == 0) {
            // in case e.g. targets has been deleted we don't have any actions
            // left for this group, so the group is finished
            return 100;
        }
        // calculate threshold
        return ((float) finished / (float) totalGroup) * 100;
    }

}
