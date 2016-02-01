/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.cache.CacheWriteNotify;
import org.eclipse.hawkbit.eventbus.event.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.model.Rollout_;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.rollout.condition.RolloutGroupActionEvaluator;
import org.eclipse.hawkbit.rollout.condition.RolloutGroupConditionEvaluator;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

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
    public Rollout createRollout(final Rollout rollout, final int amountGroup,
            final RolloutGroupConditions conditions) {
        final Rollout savedRollout = createRollout(rollout, amountGroup);
        return createRolloutGroups(amountGroup, conditions, savedRollout);
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
        final Rollout savedRollout = createRollout(rollout, amountGroup);
        executor.execute(() -> {
            final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setName("creatingRollout");
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            new TransactionTemplate(txManager, def).execute(status -> {
                createRolloutGroups(amountGroup, conditions, savedRollout);
                return null;
            });
        });
        return savedRollout;
    }

    private Rollout createRollout(final Rollout rollout, final int amountGroup) {
        verifyRolloutGroupParameter(amountGroup);
        final Long totalTargets = targetManagement.countTargetByTargetFilterQuery(rollout.getTargetFilterQuery());
        rollout.setTotalTargets(totalTargets.longValue());
        return rolloutRepository.save(rollout);
    }

    private void verifyRolloutGroupParameter(final int amountGroup) {
        if (amountGroup <= 0) {
            throw new IllegalArgumentException("the amountGroup must be greater than zero");
        } else if (amountGroup > 500) {
            throw new IllegalArgumentException("the amountGroup must not be greater than 500");
        }
    }

    /**
     * Method for creating rollout groups and calculating group sizes. Group
     * sizes are calculated by dividing the total count of targets through the
     * amount of given groups. In same cases this will lead to less rollout
     * groups than given by client.
     * 
     * @param amountGroup
     *            the amount of groups
     * @param conditions
     *            the rollout group conditions
     * @param savedRollout
     *            the rollout
     * @return the rollout with created groups
     */
    private Rollout createRolloutGroups(final int amountGroup, final RolloutGroupConditions conditions,
            final Rollout savedRollout) {
        int pageIndex = 0;
        int groupIndex = 0;
        final Long totalCount = savedRollout.getTotalTargets();
        final int groupSize = (int) Math.ceil((double) totalCount / (double) amountGroup);
        // validate if the amount of groups that will be created are the amount
        // of groups that the client what's to have created.
        int amountGroupValidated = amountGroup;
        if ((Math.ceil((double) totalCount / (double) groupSize)) != amountGroup) {
            amountGroupValidated--;
        }
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

            final RolloutGroup savedGroup = rolloutGroupRepository.save(group);

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

    /**
     * Starts a rollout which has been created. The rollout must be in
     * {@link RolloutStatus#READY} state. The according actions will be created
     * for each affected target in the rollout. The actions of the first group
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
    public Rollout startRollout(final Rollout rollout) {
        final Rollout mergedRollout = entityManager.merge(rollout);
        checkIfRolloutCanStarted(rollout, mergedRollout);
        return doStartRollout(mergedRollout);
    }

    /**
     * Starts a rollout asynchronously which has been created. The rollout must
     * be in {@link RolloutStatus#READY} state. The according actions will be
     * created asynchronously for each affected target in the rollout. The
     * actions of the first group will be started immediately
     * {@link RolloutGroupStatus#RUNNING} as the other groups will be
     * {@link RolloutGroupStatus#SCHEDULED} state.
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
    public Rollout startRolloutAsync(final Rollout rollout) {
        final Rollout mergedRollout = entityManager.merge(rollout);
        checkIfRolloutCanStarted(rollout, mergedRollout);
        mergedRollout.setStatus(RolloutStatus.STARTING);
        final Rollout updatedRollout = rolloutRepository.save(mergedRollout);
        executor.execute(() -> {
            final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setName("startingRollout");
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            new TransactionTemplate(txManager, def).execute(status -> {
                doStartRollout(updatedRollout);
                return null;
            });
        });
        return updatedRollout;

    }

    private Rollout doStartRollout(final Rollout rollout) {
        final DistributionSet distributionSet = rollout.getDistributionSet();
        final ActionType actionType = rollout.getActionType();
        final long forceTime = rollout.getForcedTime();
        final List<RolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutOrderByIdAsc(rollout);
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
            } else {
                // create only not active actions with status scheduled so they
                // can be activated later
                deploymentManagement.createScheduledAction(targetGroup, distributionSet, actionType, forceTime, rollout,
                        rolloutGroup);
                rolloutGroup.setStatus(RolloutGroupStatus.SCHEDULED);
            }
            rolloutGroupRepository.save(rolloutGroup);
            cleanupRolloutTargetGroupTable(rolloutGroup);
        }
        rollout.setStatus(RolloutStatus.RUNNING);
        return rolloutRepository.save(rollout);
    }

    private void cleanupRolloutTargetGroupTable(final RolloutGroup rolloutGroup) {
        // clean up the target group table because we don't need the
        // information anymore. We've created the necessary action entries
        // already. need to do via entity manager, otherwise an extra select
        // statement will be executed and single delete statements for each
        // targetGroup is executed, because of a combined unique ID.
        entityManager.createQuery("DELETE from RolloutTargetGroup r where r.rolloutGroup=:rolloutGroup")
                .setParameter("rolloutGroup", rolloutGroup).executeUpdate();
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
        if (!(RolloutStatus.PAUSED.equals(mergedRollout.getStatus()))) {
            throw new RolloutIllegalStateException("Rollout can only be resumed in state paused but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
        mergedRollout.setStatus(RolloutStatus.RUNNING);
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
            // between
            LOGGER.info("No rolloutcheck necessary for current scheduled check {}, next check at {}", lastCheck,
                    lastCheck + delayBetweenChecks);
            return;
        }

        final List<Rollout> rolloutsToCheck = rolloutRepository.findByLastCheckAndStatus(lastCheck,
                RolloutStatus.RUNNING);
        LOGGER.info("Found {} running rollouts to check", rolloutsToCheck.size());

        for (final Rollout rollout : rolloutsToCheck) {
            LOGGER.debug("Checking rollout {}", rollout);
            final List<RolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutAndStatus(rollout,
                    RolloutGroupStatus.RUNNING);

            if (rolloutGroups.isEmpty()) {
                // no running rollouts, probably there was an error
                // somewhere at the latest group. And the latest group has
                // been switched from running into error state. So we need
                // to find the latest group which
                executeLatestRolloutGroup(rollout);
            } else {
                LOGGER.debug("Rollout {} has {} running groups", rollout.getId(), rolloutGroups.size());
                executeRolloutGroups(rollout, rolloutGroups);
            }

            if (isRolloutComplete(rollout)) {
                LOGGER.info("Rollout {} is finished, setting finished status", rollout);
                rollout.setStatus(RolloutStatus.FINISHED);
                rolloutRepository.save(rollout);
            }
        }
    }

    private void executeRolloutGroups(final Rollout rollout, final List<RolloutGroup> rolloutGroups) {
        for (final RolloutGroup rolloutGroup : rolloutGroups) {
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

    private void executeLatestRolloutGroup(final Rollout rollout) {
        final List<RolloutGroup> latestRolloutGroup = rolloutGroupRepository
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
        return (rolloutRoot, query, criteriaBuilder) -> {
            final String searchTextToLower = searchText.toLowerCase();
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(rolloutRoot.get(Rollout_.name)), searchTextToLower),
                    criteriaBuilder.like(criteriaBuilder.lower(rolloutRoot.get(Rollout_.description)),
                            searchTextToLower));
        };
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
        final Slice<Rollout> findAll = criteriaNoCountDao.findAll(specs, pageable, Rollout.class);
        setRolloutStatusDetails(findAll);
        return findAll;
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
        return rolloutRepository.save(rollout);
    }

    /**
     * Get count of targets in different status in rollout.
     * 
     * @param page
     *            the page request to sort and limit the result
     * @return a list of rollouts with details of targets count for different
     *         statuses
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Page<Rollout> findAllRolloutsWithDetailedStatus(final Pageable page) {
        final Page<Rollout> rollouts = findAll(page);
        setRolloutStatusDetails(rollouts);
        return rollouts;

    }

    /**
     * Get count of targets in different status in rollout.
     *
     * @param rolloutId
     *            rollout id
     * @return rollout details of targets count for different statuses
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT)
    public Rollout findRolloutWithDetailedStatus(final Long rolloutId) {
        final Rollout rollout = findRolloutById(rolloutId);
        final List<TotalTargetCountActionStatus> rolloutStatusCountItems = actionRepository
                .getStatusCountByRolloutId(rolloutId);
        final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(rolloutStatusCountItems,
                rollout.getTotalTargets());
        rollout.setTotalTargetCountStatus(totalTargetCountStatus);
        return rollout;
    }

    private Map<Long, List<TotalTargetCountActionStatus>> getStatusCountItemForRollout(final List<Long> rolloutIds) {
        final List<TotalTargetCountActionStatus> resultList = actionRepository.getStatusCountByRolloutId(rolloutIds);
        return resultList.stream().collect(Collectors.groupingBy(TotalTargetCountActionStatus::getId));
    }

    private void setRolloutStatusDetails(final Slice<Rollout> rollouts) {
        final List<Long> rolloutIds = rollouts.getContent().stream().map(rollout -> rollout.getId())
                .collect(Collectors.toList());
        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRollout(
                rolloutIds);

        for (final Rollout rollout : rollouts) {
            final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                    allStatesForRollout.get(rollout.getId()), rollout.getTotalTargets());
            rollout.setTotalTargetCountStatus(totalTargetCountStatus);
        }
    }

    private void checkIfRolloutCanStarted(final Rollout rollout, final Rollout mergedRollout) {
        if (!(RolloutStatus.READY.equals(mergedRollout.getStatus()))) {
            throw new RolloutIllegalStateException("Rollout can only be started in state ready but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
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

    // ////////Rollout - changes ends here/////////////

}
