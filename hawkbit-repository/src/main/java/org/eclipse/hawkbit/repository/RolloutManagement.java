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
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.cache.CacheWriteNotify;
import org.eclipse.hawkbit.eventbus.event.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.RolloutTargetsStatusCount.RolloutTargetStatus;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup_;
import org.eclipse.hawkbit.repository.model.RolloutStatusCountItem;
import org.eclipse.hawkbit.repository.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.model.RolloutTargetGroup_;
import org.eclipse.hawkbit.repository.model.Rollout_;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Target_;
import org.eclipse.hawkbit.rollout.condition.RolloutGroupActionEvaluator;
import org.eclipse.hawkbit.rollout.condition.RolloutGroupConditionEvaluator;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import com.google.common.eventbus.EventBus;

/**
 * RolloutManagement to control rollouts e.g. like creating, starting, resuming
 * and pausing rollouts. This service secures all the functionality based on the
 * {@link PreAuthorize} annotation on methods.
 */
@Validated
@Service
@EnableScheduling
@Transactional(readOnly = true)
public class RolloutManagement {

    private static final Logger logger = LoggerFactory.getLogger(RolloutManagement.class);

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
    private ActionRepository actionRepository;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private NoCountPagingRepository criteriaNoCountDao;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private CacheWriteNotify cacheWriteNotify;

    @Autowired
    private EventBus eventBus;

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor executor;

    /**
     * Retrieves all rollouts.
     * 
     * @param page
     *            the page request to sort and limit the result
     * @return a page of found rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Page<Rollout> findAll(final Pageable page) {
        return rolloutRepository.findAll(page);
    }

    /**
     * Retrieves all rollouts found by the given specification.
     * 
     * @param specification
     *            the specification to filter rollouts
     * @param page
     *            the page request to sort and limit the result
     * @return a page of found rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Page<Rollout> findAllByPredicate(final Specification<Rollout> specification, final Pageable page) {
        return rolloutRepository.findAll(specification, page);
    }

    /**
     * Retrieves a specific rollout by its ID.
     * 
     * @param rolloutId
     *            the ID of the rollout to retrieve
     * @return the founded rollout or {@code null} if rollout with given ID does
     *         not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Rollout findRolloutById(final Long rolloutId) {
        return rolloutRepository.findOne(rolloutId);
    }

    /**
     * Retrieves a page of {@link RolloutGroup}s filtered by a given
     * {@link Rollout}.
     * 
     * @param rolloutId
     *            the ID of the rollout to filter the {@link RolloutGroup}s
     * @param page
     *            the page request to sort and limit the result
     * @return a page of found {@link RolloutGroup}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Page<RolloutGroup> findRolloutGroupsByRollout(final Long rolloutId, final Pageable page) {
        return rolloutGroupRepository.findByRolloutId(rolloutId, page);
    }

    /**
     * Retrieves a page of {@link RolloutGroup}s filtered by a given
     * {@link Rollout} and the given {@link Specification}.
     * 
     * @param rolloutId
     *            the ID of the rollout to filter the {@link RolloutGroup}s
     * @param specification
     *            the specification to filter the result set based on attributes
     *            of the {@link RolloutGroup}
     * @param page
     *            the page request to sort and limit the result
     * @return a page of found {@link RolloutGroup}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Page<RolloutGroup> findRolloutGroupsByPredicate(final Long rolloutId,
            final Specification<RolloutGroup> specification, final Pageable page) {
        return rolloutGroupRepository.findAll(new Specification<RolloutGroup>() {
            @Override
            public Predicate toPredicate(final Root<RolloutGroup> root, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                return cb.and(cb.equal(root.get(RolloutGroup_.rollout), rolloutId),
                        specification.toPredicate(root, query, cb));
            }
        }, page);
    }

    /**
     * Retrieves a single {@link RolloutGroup} by its ID.
     * 
     * @param rolloutGroupId
     *            the ID of the rollout group to find
     * @return the found {@link RolloutGroup} by its ID or {@code null} if it
     *         does not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public RolloutGroup findRolloutGroupById(final Long rolloutGroupId) {
        return rolloutGroupRepository.findOne(rolloutGroupId);
    }

    /**
     * Persists a new rollout entity. The filter within the
     * {@link Rollout#getTargetFilterQuery()} is used to retrieve the targets
     * which are effected by this rollout to create. The targets will then be
     * split up into groups. The size of the groups can be defined in the
     * {@code groupSize} parameter.
     * 
     * The rollout is not started. Only the preparation of the rollout is done,
     * persisting and creating all the necessary groups. The Rollout and the
     * groups are persisted in {@link RolloutStatus#READY} and
     * {@link RolloutGroupStatus#READY} so they can be started
     * {@link #startRollout(Rollout)}.
     * 
     * @param rollout
     *            the rollout entity to create
     * @param amountGroup
     *            the amount of groups to split the rollout into
     * @param conditions
     *            the rolloutgroup conditions and actions which should be
     *            applied for each {@link RolloutGroup}
     * @return the persisted rollout.
     * 
     * @throws IllegalArgumentException
     *             in case the given groupSize is zero or lower.
     */
    @Transactional
    @Modifying
    // @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Rollout createRollout(final Rollout rollout, final int amountGroup, final RolloutGroupConditions conditions) {
        verifyRolloutGroupParameter(amountGroup);
        rollout.setNew(true);
        final Rollout savedRollout = rolloutRepository.save(rollout);
        final Long totalCount = targetManagement.countTargetByTargetFilterQuery(savedRollout.getTargetFilterQuery());
        return createRolloutGroups(amountGroup, conditions, savedRollout, totalCount);
    }

    /**
     * Persists a new rollout entity. The filter within the
     * {@link Rollout#getTargetFilterQuery()} is used to retrieve the targets
     * which are effected by this rollout to create. The creation of the rollout
     * will be done synchronously and will be returned. The targets will then be
     * split up into groups. The size of the groups can be defined in the
     * {@code groupSize} parameter.
     * 
     * The creation of the rollout groups is executed asynchronously due it
     * might take some time to split up the targets into groups. The creation of
     * the {@link RolloutGroup} is published as event
     * {@link RolloutGroupCreatedEvent}.
     * 
     * The rollout is in status {@link RolloutStatus#CREATING} until all rollout
     * groups has been created and the targets are split up, then the rollout
     * will change the status to {@link RolloutStatus#READY}.
     * 
     * The rollout is not started. Only the preparation of the rollout is done,
     * persisting and creating all the necessary groups. The Rollout and the
     * groups are persisted in {@link RolloutStatus#READY} and
     * {@link RolloutGroupStatus#READY} so they can be started
     * {@link #startRollout(Rollout)}.
     * 
     * @param rollout
     *            the rollout to be created
     * @param amountGroup
     *            the number of groups should be created for the rollout and
     *            split up the targets
     * @param conditions
     *            the rolloutgroup conditions and actions which should be
     *            applied for each {@link RolloutGroup}
     * @return the created rollout entity in state
     *         {@link RolloutStatus#CREATING}
     */
    @Transactional
    @Modifying
    public Rollout createRolloutAsync(final Rollout rollout, final int amountGroup,
            final RolloutGroupConditions conditions) {
        verifyRolloutGroupParameter(amountGroup);
        rollout.setNew(true);
        final Rollout savedRollout = rolloutRepository.save(rollout);
        final Long totalCount = targetManagement.countTargetByTargetFilterQuery(savedRollout.getTargetFilterQuery());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
                def.setName("creatingRollout");
                def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                new TransactionTemplate(txManager, def).execute(new TransactionCallback<Void>() {
                    @Override
                    public Void doInTransaction(final TransactionStatus status) {
                        createRolloutGroups(amountGroup, conditions, savedRollout, totalCount);
                        return null;
                    }
                });
            }
        });
        return savedRollout;
    }

    private void verifyRolloutGroupParameter(final int amountGroup) {
        if (amountGroup <= 0) {
            throw new IllegalArgumentException("the amountGroup must be greater than zero");
        } else if (amountGroup > 500) {
            throw new IllegalArgumentException("the amountGroup must not be greater than 500");
        }
    }

    private Rollout createRolloutGroups(final int amountGroup, final RolloutGroupConditions conditions,
            final Rollout savedRollout, final Long totalCount) {
        int pageIndex = 0;
        int groupIndex = 0;
        final int groupSize = (int) Math.ceil(totalCount / amountGroup);
        RolloutGroup lastSavedGroup = null;
        while (pageIndex < totalCount) {
            groupIndex++;
            final RolloutGroup group = new RolloutGroup();
            group.setName("group-" + groupIndex);
            group.setRollout(savedRollout);
            group.setParent(lastSavedGroup);
            group.setSuccessCondition(conditions.getSuccessCondition());
            group.setSuccessConditionExp(conditions.getSuccessConditionExp());
            group.setErrorCondition(conditions.getErrorCondition());
            group.setErrorConditionExp(conditions.getErrorConditionExp());
            group.setErrorAction(conditions.getErrorAction());
            group.setErrorActionExp(conditions.getErrorActionExp());
            group.setNew(true);

            final RolloutGroup savedGroup = rolloutGroupRepository.save(group);
            lastSavedGroup = savedGroup;
            final Slice<Target> targetGroup = targetManagement.findTargetsAll(savedRollout.getTargetFilterQuery(),
                    new OffsetBasedPageRequest(pageIndex, groupSize, new Sort(Direction.ASC, "id")));
            targetGroup.forEach(target -> {
                rolloutTargetGroupRepository.save(new RolloutTargetGroup(savedGroup, target));
            });
            cacheWriteNotify.rolloutGroupCreated(groupIndex, savedRollout.getId(), amountGroup, groupIndex);
            pageIndex += groupSize;
        }

        savedRollout.setStatus(RolloutStatus.READY);
        return rolloutRepository.save(savedRollout);
    }

    /**
     * Starts a rollout which has been created. The rollout must be in
     * {@link RolloutStatus#READY} state. The according actions will be created
     * or each affected target in the rollout. The actions of the first group
     * will be started immediately {@link RolloutGroupStatus#RUNNING} as the
     * other groups will be {@link RolloutGroupStatus#SCHEDULED} state.
     * 
     * The rollout itself will be then also in {@link RolloutStatus#RUNNING}.
     * 
     * @param rollout
     *            the rollout to be started
     * 
     * @throws RolloutIllegalStateException
     *             if given rollout is not in {@link RolloutStatus#READY}. Only
     *             ready rollouts can be started.
     */
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    public void startRollout(final Rollout rollout) {
        final Rollout mergedRollout = entityManager.merge(rollout);
        if (mergedRollout.getStatus() != RolloutStatus.READY) {
            throw new RolloutIllegalStateException("Rollout can only be started in state ready but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
        final DistributionSet distributionSet = mergedRollout.getDistributionSet();
        final ActionType actionType = rollout.getActionType();
        final long forceTime = rollout.getForcedTime();
        final List<RolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutOrderByIdAsc(mergedRollout);
        for (int iGroup = 0; iGroup < rolloutGroups.size(); iGroup++) {
            final RolloutGroup rolloutGroup = rolloutGroups.get(iGroup);
            final List<Target> targetGroup = targetRepository.findByRolloutTargetGroupRolloutGroup(rolloutGroup);
            // firstgroup can already be started
            if (iGroup == 0) {
                final List<TargetWithActionType> targetsWithActionType = targetGroup.stream()
                        .map(t -> new TargetWithActionType(t.getControllerId(), actionType, forceTime))
                        .collect(Collectors.toList());
                deploymentManagement.assignDistributionSet(distributionSet.getId(), targetsWithActionType, rollout,
                        rolloutGroup);
                rolloutGroup.setStatus(RolloutGroupStatus.RUNNING);
                rolloutGroup.setNew(false);
                rolloutGroupRepository.save(rolloutGroup);
            }
            // create only not active actions with status scheduled so they can
            // be activated later
            else {
                deploymentManagement.createScheduledAction(targetGroup, distributionSet, actionType, forceTime,
                        mergedRollout, rolloutGroup);
                rolloutGroup.setStatus(RolloutGroupStatus.SCHEDULED);
                rolloutGroup.setNew(false);
                rolloutGroupRepository.save(rolloutGroup);
            }

            // clean up the target group table because we don't need the
            // information anymore. We've created the necessary action entries
            // already.
            // need to do via entity manager, otherwise an extra select
            // statement will be executed and single delete statements for each
            // targetGroup is executed, because of a combined unique ID.
            entityManager.createQuery("DELETE from RolloutTargetGroup r where r.rolloutGroup=:rolloutGroup")
                    .setParameter("rolloutGroup", rolloutGroup).executeUpdate();
        }
        // set rollout into running status
        mergedRollout.setStatus(RolloutStatus.RUNNING);
        mergedRollout.setNew(false);
        rolloutRepository.save(mergedRollout);
    }

    /**
     * Pauses a rollout which is currently running. The Rollout switches
     * {@link RolloutStatus#PAUSED}. {@link RolloutGroup}s which are currently
     * running will be untouched. {@link RolloutGroup}s which are
     * {@link RolloutGroupStatus#SCHEDULED} will not be started and keep in
     * {@link RolloutGroupStatus#SCHEDULED} state until the rollout is
     * {@link RolloutManagement#resumeRollout(Rollout)}.
     * 
     * Switching the rollout status to {@link RolloutStatus#PAUSED} is
     * sufficient due the {@link #checkRunningRollouts(long)} will not check
     * this rollout anymore.
     * 
     * @param rollout
     *            the rollout to be paused.
     * 
     * @throws RolloutIllegalStateException
     *             if given rollout is not in {@link RolloutStatus#RUNNING}.
     *             Only running rollouts can be paused.
     */
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    public void pauseRollout(final Rollout rollout) {
        final Rollout mergedRollout = entityManager.merge(rollout);
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
        mergedRollout.setNew(false);
        rolloutRepository.save(mergedRollout);
    }

    /**
     * Resumes a paused rollout. The rollout switches back to
     * {@link RolloutStatus#RUNNING} state which is then picked up again by the
     * {@link #checkRunningRollouts(long)}.
     * 
     * @param rollout
     *            the rollout to be resumed
     * @throws RolloutIllegalStateException
     *             if given rollout is not in {@link RolloutStatus#PAUSED}. Only
     *             paused rollouts can be resumed.
     */
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    public void resumeRollout(final Rollout rollout) {
        final Rollout mergedRollout = entityManager.merge(rollout);
        if (mergedRollout.getStatus() != RolloutStatus.PAUSED) {
            throw new RolloutIllegalStateException("Rollout can only be resumed in state paused but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
        mergedRollout.setStatus(RolloutStatus.RUNNING);
        mergedRollout.setNew(false);
        rolloutRepository.save(mergedRollout);
    }

    /**
     * Checking running rollouts. Rollouts which are checked updating the
     * {@link Rollout#setLastCheck(long)} to indicate that the current instance
     * is handling the specific rollout. This code should run as system-code.
     * 
     * <pre>
     * {@code
     *  SystemSecurityContext.runAsSystem(new Callable<Void>() {
     *     public Void call() throws Exception {
     *        //run system-code
     *     }
     * });
     *  }
     * </pre>
     * 
     * This method is attend to be called by a scheduler.
     * {@link RolloutScheduler}. And must be running in an transaction so it's
     * splitted from the scheduler.
     * 
     * Rollouts which are currently running are investigated, by means the
     * error- and finish condition of running groups in this rollout are
     * evaluated.
     * 
     * @param delayBetweenChecks
     *            the time in milliseconds of the delay between the further and
     *            this check. This check is only applied if the last check is
     *            less than (lastcheck-delay).
     */
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    public void checkRunningRollouts(final long delayBetweenChecks) {
        final long lastCheck = System.currentTimeMillis();
        final int updated = rolloutRepository.updateLastCheck(lastCheck, delayBetweenChecks, RolloutStatus.RUNNING);

        if (updated == 0) {
            // nothing to check, maybe another instance already checked in
            // between.
            logger.info("No rolloutcheck necessary for current scheduled check {}, next check at {}", lastCheck,
                    (lastCheck + delayBetweenChecks));
        }
        // Only check the running rollouts in case no one else already checked
        // e.g. another instance.
        else {
            final List<Rollout> rolloutsToCheck = rolloutRepository.findByLastCheckAndStatus(lastCheck,
                    RolloutStatus.RUNNING);
            logger.info("Found {} running rollouts to check", rolloutsToCheck.size());

            for (final Rollout rollout : rolloutsToCheck) {
                logger.debug("Checking rollout {}", rollout);
                final List<RolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutAndStatus(rollout,
                        RolloutGroupStatus.RUNNING);

                if (rolloutGroups.isEmpty()) {
                    // no running rollouts, probably there was an error
                    // somewhere at the latest group. And the latest group has
                    // been switched from running into error state. So we need
                    // to find the latest group which
                    final List<RolloutGroup> latestRolloutGroup = rolloutGroupRepository
                            .findByRolloutAndStatusNotOrderByIdDesc(rollout, RolloutGroupStatus.SCHEDULED);
                    if (!latestRolloutGroup.isEmpty()) {
                        executeRolloutGroupSuccessAction(rollout, latestRolloutGroup.get(0));
                    }
                } else {
                    logger.debug("Rollout {} has {} running groups", rollout.getId(), rolloutGroups.size());
                    for (final RolloutGroup rolloutGroup : rolloutGroups) {
                        // error state check, do we need to stop the whole
                        // rollout
                        // because of error?
                        final RolloutGroupErrorCondition errorCondition = rolloutGroup.getErrorCondition();
                        final boolean isError = checkErrorState(rollout, rolloutGroup, errorCondition);
                        if (isError) {
                            logger.info("Rollout {} {} has error, calling error action", rollout.getName(),
                                    rollout.getId());
                            callErrorAction(rollout, rolloutGroup);
                        } else {
                            // not in error so check finished state, do we need
                            // to
                            // start
                            // the next group?
                            final RolloutGroupSuccessCondition finishedCondition = rolloutGroup.getSuccessCondition();
                            checkFinishCondition(rollout, rolloutGroup, finishedCondition);
                            if (isRolloutGroupComplete(rollout, rolloutGroup)) {
                                rolloutGroup.setStatus(RolloutGroupStatus.FINISHED);
                                rolloutGroup.setNew(false);
                                rolloutGroupRepository.save(rolloutGroup);
                            }

                        }

                    }
                }

                if (isRolloutComplete(rollout)) {
                    logger.info("Rollout {} is finished, setting finished status", rollout);
                    rollout.setStatus(RolloutStatus.FINISHED);
                    rollout.setNew(false);
                    rolloutRepository.save(rollout);
                }
            }
        }
    }

    private void callErrorAction(final Rollout rollout, final RolloutGroup rolloutGroup) {
        try {
            context.getBean(rolloutGroup.getErrorAction().getBeanName(), RolloutGroupActionEvaluator.class).eval(
                    rollout, rolloutGroup, rolloutGroup.getErrorActionExp());
        } catch (final BeansException e) {
            logger.error("Something bad happend when accessing the error action bean {}", rolloutGroup.getErrorAction()
                    .getBeanName(), e);
        }
    }

    private boolean isRolloutComplete(final Rollout rollout) {
        final Long groupsActiveLeft = rolloutGroupRepository.countByRolloutAndStatusOrStatus(rollout,
                RolloutGroupStatus.RUNNING, RolloutGroupStatus.SCHEDULED);
        return groupsActiveLeft == 0;
    }

    private boolean isRolloutGroupComplete(final Rollout rollout, final RolloutGroup rolloutGroup) {
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
            logger.error("Something bad happend when accessing the error condition bean {}",
                    errorCondition.getBeanName(), e);
            return false;
        }
    }

    private boolean checkFinishCondition(final Rollout rollout, final RolloutGroup rolloutGroup,
            final RolloutGroupSuccessCondition finishCondition) {
        logger.trace("Checking finish condition {} on rolloutgroup {}", finishCondition, rolloutGroup);
        try {
            final boolean isFinished = context.getBean(finishCondition.getBeanName(),
                    RolloutGroupConditionEvaluator.class).eval(rollout, rolloutGroup,
                    rolloutGroup.getSuccessConditionExp());
            if (isFinished) {
                logger.info("Rolloutgroup {} is finished, starting next group", rolloutGroup);
                executeRolloutGroupSuccessAction(rollout, rolloutGroup);
            } else {
                logger.debug("Rolloutgroup {} is still running", rolloutGroup);
            }
            return isFinished;
        } catch (final BeansException e) {
            logger.error("Something bad happend when accessing the finish condition bean {}",
                    finishCondition.getBeanName(), e);
            return false;
        }
    }

    private void executeRolloutGroupSuccessAction(final Rollout rollout, final RolloutGroup rolloutGroup) {
        context.getBean(rolloutGroup.getSuccessAction().getBeanName(), RolloutGroupActionEvaluator.class).eval(rollout,
                rolloutGroup, rolloutGroup.getSuccessActionExp());
    }

    // ////////Rollout - changes starts here/////////////

    /**
     * Counts all {@link Target}s in the repository.
     *
     * @return number of targets
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Long countRolloutsAll() {
        return rolloutRepository.count();
    }

    /**
     * Count rollouts by specified filter text.
     * 
     * @param searchText
     *            name or description
     * @return total count rollouts for specified filter text.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Long countRolloutsAllByFilters(final String searchText) {
        return rolloutRepository.count(likeNameOrDescription(searchText));
    }

    private static Specification<Rollout> likeNameOrDescription(final String searchText) {
        final Specification<Rollout> spec = new Specification<Rollout>() {
            @Override
            public Predicate toPredicate(final Root<Rollout> rolloutRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                final String searchTextToLower = searchText.toLowerCase();
                final Predicate predicate = cb.or(cb.like(cb.lower(rolloutRoot.get(Rollout_.name)), searchTextToLower),
                        cb.like(cb.lower(rolloutRoot.get(Rollout_.description)), searchTextToLower));
                return predicate;
            }
        };
        return spec;
    }

    /**
     * * Retrieves a specific rollout by its ID.
     * 
     * @param pageable
     *            the page request to sort and limit the result
     * @param searchText
     *            search text which matches name or description of rollout
     * @return the founded rollout or {@code null} if rollout with given ID does
     *         not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Slice<Rollout> findRolloutByFilters(final Pageable pageable, @NotEmpty final String searchText) {
        final Specification<Rollout> specs = likeNameOrDescription(searchText);
        return criteriaNoCountDao.findAll(specs, pageable, Rollout.class);
    }

    /**
     * Retrieves a specific rollout by its name.
     * 
     * @param rolloutName
     *            the name of the rollout to retrieve
     * @return the founded rollout or {@code null} if rollout with given name
     *         does not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
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
    @NotNull
    @Transactional
    @Modifying
    // @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Rollout updateRollout(@NotNull final Rollout rollout) {
        Assert.notNull(rollout.getId());
        rollout.setNew(false);
        return rolloutRepository.save(rollout);
    }

    /**
     * Get count of targets in different status in rollout.
     * 
     * @param rolloutId
     *            rollout id
     * @return RolloutTargetsStatus details of targets count for different
     *         statuses
     * 
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public RolloutTargetsStatusCount getRolloutDetailedStatus(final Long rolloutId) {
        // TODO add test case

        final RolloutTargetsStatusCount rolloutTargetsStatus = new RolloutTargetsStatusCount();
        final List<RolloutStatusCountItem<Object>> list = getStatusCountItemForRollout(rolloutId);
        populateRolloutTargetStatuscount(rolloutTargetsStatus, list);

        final Object[] statusWithCountList = rolloutTargetsStatus.getStatusCountDetails().values().stream()
                .filter(x -> x > 0).toArray();
        if (statusWithCountList.length == 0) {
            rolloutTargetsStatus.getStatusCountDetails().put(RolloutTargetStatus.NOTSTARTED,
                    targetRepository.countByRolloutTargetGroupRolloutGroupRolloutId(rolloutId));
        }
        return rolloutTargetsStatus;
    }

    /**
     * Get count of targets in different status in rollout group.
     * 
     * @param rolloutGroupId
     *            rollout group id
     * @return RolloutTargetsStatusCount details of targets count for different
     *         statuses
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public RolloutTargetsStatusCount getRolloutGroupDetailedStatus(final Long rolloutGroupId) {
        // TODO add test case

        final RolloutTargetsStatusCount rolloutTargetsStatus = new RolloutTargetsStatusCount();
        final List<RolloutStatusCountItem<Object>> list = getStatusCountItemForRolloutGroup(rolloutGroupId);
        populateRolloutTargetStatuscount(rolloutTargetsStatus, list);
        final Object[] statusWithCountList = rolloutTargetsStatus.getStatusCountDetails().values().stream()
                .filter(x -> x > 0).toArray();
        if (statusWithCountList.length == 0) {
            rolloutTargetsStatus.getStatusCountDetails().put(RolloutTargetStatus.NOTSTARTED,
                    targetRepository.countByRolloutTargetGroupRolloutGroupId(rolloutGroupId));
        }
        return rolloutTargetsStatus;

    }

    /***
     * Get finished percentage details for a specified group which is in running
     * state.
     * 
     * @param rollout
     *            {@link Rollout}
     * @param rolloutGroup
     *            {@link RolloutGroup}
     * @return percentage finished
     */
    // TODO: Need to check with Michael on this
    public float getFinishedPercentForRunningGroup(final Rollout rollout, final RolloutGroup rolloutGroup) {
        final Long totalGroup = actionRepository.countByRolloutAndRolloutGroup(rollout, rolloutGroup);
        final Long finished = actionRepository.countByRolloutAndRolloutGroupAndStatus(rollout, rolloutGroup,
                Action.Status.FINISHED);
        if (totalGroup == 0) {
            // in case e.g. targets has been deleted we don't have any actions
            // left for this group, so the group is finished
            return 100;
        }
        // calculate threshold
        return ((float) finished / (float) totalGroup) * 100;
    }

    /**
     * Get targets of specified rollout group.
     * 
     * @param rolloutGroup
     *            rollout group
     * @param page
     *            * the page request to sort and limit the result
     * 
     * @return Page<Target> list of targets of a rollout group
     */
    public Page<Target> getRolloutGroupTargets(final RolloutGroup rolloutGroup, final Pageable page) {
        if (rolloutGroup != null && rolloutGroup.getRollout().getStatus() == RolloutStatus.READY) {
            // in case of status ready the action has not been created yet and
            // the relation information between target and rollout-group is
            // stored in the #TargetRolloutGroup.
            return targetRepository.findByRolloutTargetGroupRolloutGroupId(rolloutGroup.getId(), page);
        }
        return targetRepository.findByActionsRolloutGroup(rolloutGroup, page);
    }

    /**
     * Get targets of specified rollout group.
     * 
     * @param rolloutGroup
     *            rollout group
     * @param specification
     *            the specification for filtering the targets of a rollout group
     * @param page
     *            the page request to sort and limit the result
     * 
     * @return Page<Target> list of targets of a rollout group
     */
    public Page<Target> findRolloutGroupTargets(final RolloutGroup rolloutGroup,
            final Specification<Target> specification, final Pageable page) {
        if (rolloutGroup.getRollout().getStatus() == RolloutStatus.READY) {
            // in case of status ready the action has not been created yet and
            // the relation information between target and rollout-group is
            // stored in the #TargetRolloutGroup.
            return targetRepository.findAll(new Specification<Target>() {
                @Override
                public Predicate toPredicate(final Root<Target> root, final CriteriaQuery<?> query,
                        final CriteriaBuilder cb) {
                    final ListJoin<Target, RolloutTargetGroup> rolloutTargetJoin = root
                            .join(Target_.rolloutTargetGroup);
                    return cb.and(specification.toPredicate(root, query, cb),
                            cb.equal(rolloutTargetJoin.get(RolloutTargetGroup_.rolloutGroup), rolloutGroup));
                }
            }, page);
        }

        return targetRepository.findAll(new Specification<Target>() {
            @Override
            public Predicate toPredicate(final Root<Target> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
                final ListJoin<Target, Action> actionsJoin = root.join(Target_.actions);
                return cb.and(specification.toPredicate(root, query, cb),
                        cb.equal(actionsJoin.get(Action_.rolloutGroup), rolloutGroup));
            }
        }, page);
    }

    private List<RolloutStatusCountItem<Object>> getStatusCountItemForRollout(final Long rolloutId) {
        final List<Object[]> resultList = actionRepository.getStatusCountByRolloutId(rolloutId);
        final List<RolloutStatusCountItem<Object>> reportItems = resultList.stream()
                .map(r -> new RolloutStatusCountItem<>(r[0], ((Number) r[1]).longValue())).collect(Collectors.toList());

        return reportItems;
    }

    private List<RolloutStatusCountItem<Object>> getStatusCountItemForRolloutGroup(final Long rolloutId) {
        final List<Object[]> resultList = actionRepository.getStatusCountByRolloutGroupId(rolloutId);
        final List<RolloutStatusCountItem<Object>> reportItems = resultList.stream()
                .map(r -> new RolloutStatusCountItem<>(r[0], ((Number) r[1]).longValue())).collect(Collectors.toList());

        return reportItems;
    }

    private void populateRolloutTargetStatuscount(final RolloutTargetsStatusCount rolloutTargetsStatus,
            final List<RolloutStatusCountItem<Object>> list) {
        Long cancelledItemCount = 0L;
        Long runningItemsCount = 0L;
        for (final RolloutStatusCountItem<Object> item : list) {
            if (item.getStatus().equals(Action.Status.SCHEDULED)) {
                rolloutTargetsStatus.getStatusCountDetails().put(RolloutTargetStatus.READY, item.getCount());
            } else if (item.getStatus().equals(Action.Status.ERROR)) {
                rolloutTargetsStatus.getStatusCountDetails().put(RolloutTargetStatus.ERROR, item.getCount());
            } else if (item.getStatus().equals(Action.Status.FINISHED)) {
                rolloutTargetsStatus.getStatusCountDetails().put(RolloutTargetStatus.FINISHED, item.getCount());
            } else if (Arrays.asList(Action.Status.RETRIEVED, Action.Status.RUNNING, Action.Status.WARNING,
                    Action.Status.DOWNLOAD).contains(item.getStatus())) {
                runningItemsCount = runningItemsCount + item.getCount();
            } else if (Arrays.asList(Action.Status.CANCELED, Action.Status.CANCELING).contains(item.getStatus())) {
                cancelledItemCount = cancelledItemCount + item.getCount();
            }
        }
        rolloutTargetsStatus.getStatusCountDetails().put(RolloutTargetStatus.RUNNING, runningItemsCount);
        rolloutTargetsStatus.getStatusCountDetails().put(RolloutTargetStatus.CANCELLED, cancelledItemCount);
    }


    // ////////Rollout - changes ends here/////////////

}
