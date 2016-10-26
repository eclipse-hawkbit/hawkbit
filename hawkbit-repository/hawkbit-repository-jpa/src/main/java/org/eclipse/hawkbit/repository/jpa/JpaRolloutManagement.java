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

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.RolloutFields;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.cache.CacheWriteNotify;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
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
    private NoCountPagingRepository criteriaNoCountDao;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private CacheWriteNotify cacheWriteNotify;

    @Autowired
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor executor;

    /*
     * set which stores the rollouts which are asynchronously creating. This is
     * necessary to verify rollouts which maybe stuck during creationg e.g.
     * because of database interruption, failures or even application crash.
     * !This is not cluster aware!
     */
    private static final Set<String> creatingRollouts = ConcurrentHashMap.newKeySet();

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
        final JpaRollout savedRollout = createRollout((JpaRollout) rollout, amountGroup);
        return createRolloutGroups(amountGroup, conditions, savedRollout);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public Rollout createRolloutAsync(final Rollout rollout, final int amountGroup,
            final RolloutGroupConditions conditions) {
        final JpaRollout savedRollout = createRollout((JpaRollout) rollout, amountGroup);
        creatingRollouts.add(savedRollout.getName());
        // need to flush the entity manager here to get the ID of the rollout,
        // because entity manager is set to FlushMode#Auto, entitymanager will
        // flush the Target entity, due the indirect relationship to the Rollout
        // entity without set an ID JPA is throwing a Invalid
        // 'org.springframework.dao.InvalidDataAccessApiUsageException: During
        // synchronization aect was found through a relationship that was not
        // marked cascade PERSIST'
        entityManager.flush();
        executor.execute(() -> {
            try {
                createRolloutGroupsInNewTransaction(amountGroup, conditions, savedRollout);
            } finally {
                creatingRollouts.remove(savedRollout.getName());
            }
        });
        return savedRollout;
    }

    private JpaRollout createRollout(final JpaRollout rollout, final int amountGroup) {
        verifyRolloutGroupParameter(amountGroup);
        final Long totalTargets = targetManagement.countTargetByTargetFilterQuery(rollout.getTargetFilterQuery());
        rollout.setTotalTargets(totalTargets.longValue());
        return rolloutRepository.save(rollout);
    }

    private static void verifyRolloutGroupParameter(final int amountGroup) {
        if (amountGroup <= 0) {
            throw new IllegalArgumentException("the amountGroup must be greater than zero");
        } else if (amountGroup > 500) {
            throw new IllegalArgumentException("the amountGroup must not be greater than 500");
        }
    }

    private Rollout createRolloutGroupsInNewTransaction(final int amountOfGroups,
            final RolloutGroupConditions conditions, final JpaRollout savedRollout) {
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("creatingRollout");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new TransactionTemplate(txManager, def)
                .execute(status -> createRolloutGroups(amountOfGroups, conditions, savedRollout));
    }

    /**
     * Method for creating rollout groups and calculating group sizes. Group
     * sizes are calculated by dividing the total count of targets through the
     * amount of given groups. In same cases this will lead to less rollout
     * groups than given by client.
     *
     * @param amountOfGroups
     *            the amount of groups
     * @param conditions
     *            the rollout group conditions
     * @param savedRollout
     *            the rollout
     * @return the rollout with created groups
     */
    private Rollout createRolloutGroups(final int amountOfGroups, final RolloutGroupConditions conditions,
            final JpaRollout savedRollout) {
        int pageIndex = 0;
        int groupIndex = 0;
        final Long totalCount = savedRollout.getTotalTargets();
        final int groupSize = (int) Math.ceil((double) totalCount / (double) amountOfGroups);
        // validate if the amount of groups that will be created are the amount
        // of groups that the client what's to have created.
        int amountGroupValidated = amountOfGroups;
        final int amountGroupCreation = (int) (Math.ceil((double) totalCount / (double) groupSize));
        if (amountGroupCreation == (amountOfGroups - 1)) {
            amountGroupValidated--;
        }
        RolloutGroup lastSavedGroup = null;
        while (pageIndex < totalCount) {
            groupIndex++;
            final String nameAndDesc = "group-" + groupIndex;
            final JpaRolloutGroup group = new JpaRolloutGroup();
            group.setName(nameAndDesc);
            group.setDescription(nameAndDesc);
            group.setRollout(savedRollout);
            group.setParent(lastSavedGroup);
            group.setSuccessCondition(conditions.getSuccessCondition());
            group.setSuccessConditionExp(conditions.getSuccessConditionExp());
            group.setErrorCondition(conditions.getErrorCondition());
            group.setErrorConditionExp(conditions.getErrorConditionExp());
            group.setErrorAction(conditions.getErrorAction());
            group.setErrorActionExp(conditions.getErrorActionExp());

            final JpaRolloutGroup savedGroup = rolloutGroupRepository.save(group);

            final Slice<Target> targetGroup = targetManagement.findTargetsAll(savedRollout.getTargetFilterQuery(),
                    new OffsetBasedPageRequest(pageIndex, groupSize, new Sort(Direction.ASC, "id")));
            savedGroup.setTotalTargets(targetGroup.getContent().size());

            lastSavedGroup = savedGroup;

            targetGroup
                    .forEach(target -> rolloutTargetGroupRepository.save(new RolloutTargetGroup(savedGroup, target)));
            cacheWriteNotify.rolloutGroupCreated(groupIndex, savedRollout.getId(), savedGroup.getId(),
                    amountGroupValidated, groupIndex);
            pageIndex += groupSize;
        }

        savedRollout.setStatus(RolloutStatus.READY);
        return rolloutRepository.save(savedRollout);
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
            // firstgroup can already be started
            if (iGroup == 0) {
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
        verifyStuckedRollouts();
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

    /**
     * Verifies and handles stucked rollouts in asynchronous creation or
     * starting state. If rollouts are created or started asynchronously it
     * might be that they keep in state {@link RolloutStatus#CREATING} or
     * {@link RolloutStatus#STARTING} due database or application interruption.
     * In case this happens, set the rollout to error state.
     */
    private void verifyStuckedRollouts() {
        final List<JpaRollout> rolloutsInCreatingState = rolloutRepository.findByStatus(RolloutStatus.CREATING);
        rolloutsInCreatingState.stream().filter(rollout -> !creatingRollouts.contains(rollout.getName()))
                .forEach(rollout -> {
                    LOGGER.warn(
                            "Determined error during rollout creation of rollout {}, stucking in creating state, setting to status",
                            rollout, RolloutStatus.ERROR_CREATING);
                    rollout.setStatus(RolloutStatus.ERROR_CREATING);
                    rolloutRepository.save(rollout);
                });

        final List<JpaRollout> rolloutsInStartingState = rolloutRepository.findByStatus(RolloutStatus.STARTING);
        rolloutsInStartingState.stream().filter(rollout -> !startingRollouts.contains(rollout.getName()))
                .forEach(rollout -> {
                    LOGGER.warn(
                            "Determined error during rollout starting of rollout {}, stucking in starting state, setting to status",
                            rollout, RolloutStatus.ERROR_STARTING);
                    rollout.setStatus(RolloutStatus.ERROR_STARTING);
                    rolloutRepository.save(rollout);
                });

    }

    private void executeRolloutGroups(final JpaRollout rollout, final List<JpaRolloutGroup> rolloutGroups) {
        for (final JpaRolloutGroup rolloutGroup : rolloutGroups) {

            checkIfTargetsOfRolloutGroupDeleted(rolloutGroup);

            // error state check, do we need to stop the whole
            // rollout because of error?
            final RolloutGroupErrorCondition errorCondition = rolloutGroup.getErrorCondition();
            final boolean isError = checkErrorState(rollout, rolloutGroup, errorCondition);
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

    private void checkIfTargetsOfRolloutGroupDeleted(final JpaRolloutGroup rolloutGroup) {

        final long countTargetsOfRolloutGroup = rolloutGroupManagement
                .findRolloutGroupTargets(rolloutGroup, new OffsetBasedPageRequest(0, 1, null)).getTotalElements();

        if (rolloutGroup.getTotalTargets() != countTargetsOfRolloutGroup) {
            // targets have been deleted and we have to update the
            // total target count in the rollout and the rollout group
            final JpaRollout jpaRollout = (JpaRollout) rolloutGroup.getRollout();
            final long updatedTargetCount = jpaRollout.getTotalTargets()
                    - (rolloutGroup.getTotalTargets() - countTargetsOfRolloutGroup);
            jpaRollout.setTotalTargets(updatedTargetCount);
            final JpaRolloutGroup jpaRolloutGroup = rolloutGroup;
            jpaRolloutGroup.setTotalTargets((int) countTargetsOfRolloutGroup);

            rolloutRepository.save(jpaRollout);
            rolloutGroupRepository.save(jpaRolloutGroup);
        }

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

    private boolean checkErrorState(final Rollout rollout, final RolloutGroup rolloutGroup,
            final RolloutGroupErrorCondition errorCondition) {
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
