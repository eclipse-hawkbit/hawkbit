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

import static org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor.afterCommit;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import jakarta.persistence.criteria.JoinType;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.management.JpaDeploymentManagement.MaxAssignmentsExceededInfo;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DistributionSet} to {@link Target} assignment strategy as utility for
 * {@link JpaDeploymentManagement}.
 */
@Slf4j
public abstract class AbstractDsAssignmentStrategy {

    protected final TargetRepository targetRepository;
    protected final ActionRepository actionRepository;

    private final ActionStatusRepository actionStatusRepository;
    private final QuotaManagement quotaManagement;
    private final BooleanSupplier confirmationFlowConfig;
    private final RepositoryProperties repositoryProperties;
    private final Consumer<MaxAssignmentsExceededInfo> maxAssignmentExceededHandler;

    @SuppressWarnings("java:S107")
    AbstractDsAssignmentStrategy(
            final TargetRepository targetRepository,
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository,
            final QuotaManagement quotaManagement, final BooleanSupplier confirmationFlowConfig,
            final RepositoryProperties repositoryProperties,
            final Consumer<MaxAssignmentsExceededInfo> maxAssignmentExceededHandler) {
        this.targetRepository = targetRepository;
        this.actionRepository = actionRepository;
        this.actionStatusRepository = actionStatusRepository;
        this.quotaManagement = quotaManagement;
        this.confirmationFlowConfig = confirmationFlowConfig;
        this.repositoryProperties = repositoryProperties;
        this.maxAssignmentExceededHandler = maxAssignmentExceededHandler;
    }

    public JpaAction createTargetAction(
            final TargetWithActionType targetWithActionType,
            final List<JpaTarget> targets, final JpaDistributionSet set) {
        final Optional<JpaTarget> optTarget = targets.stream()
                .filter(t -> t.getControllerId().equals(targetWithActionType.getControllerId())).findFirst();

        // create the action
        return optTarget.map(target -> {
            assertActionsPerTargetQuota(target);
            final JpaAction actionForTarget = new JpaAction();
            actionForTarget.setActionType(targetWithActionType.getActionType());
            actionForTarget.setForcedTime(targetWithActionType.getForceTime());
            actionForTarget.setWeight(
                    targetWithActionType.getWeight() == null
                            ? repositoryProperties.getActionWeightIfAbsent()
                            : targetWithActionType.getWeight());
            actionForTarget.setActive(true);
            actionForTarget.setTarget(target);
            actionForTarget.setDistributionSet(set);
            actionForTarget.setMaintenanceWindowSchedule(targetWithActionType.getMaintenanceSchedule());
            actionForTarget.setMaintenanceWindowDuration(targetWithActionType.getMaintenanceWindowDuration());
            actionForTarget.setMaintenanceWindowTimeZone(targetWithActionType.getMaintenanceWindowTimeZone());
            actionForTarget.setInitiatedBy(AccessContext.actor());
            return actionForTarget;
        }).orElseGet(() -> {
            log.warn("Cannot find target for targetWithActionType '{}'.", targetWithActionType.getControllerId());
            return null;
        });
    }

    public JpaActionStatus createActionStatus(final JpaAction action, final String actionMessage) {
        final JpaActionStatus actionStatus = new JpaActionStatus();
        actionStatus.setAction(action);
        actionStatus.setTimestamp(action.getCreatedAt());

        if (StringUtils.hasText(actionMessage)) {
            actionStatus.addMessage(actionMessage);
        } else {
            actionStatus.addMessage(getActionMessage(action));
        }

        return actionStatus;
    }

    protected void sendTargetUpdatedEvent(final JpaTarget target) {
        afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetUpdatedEvent(target)));
    }

    /**
     * Cancels {@link Action}s that are no longer necessary and sends
     * cancellations to the controller.
     * <p/>
     * No access control applied
     *
     * @param targetsIds to override {@link Action}s
     */
    protected void overrideObsoleteUpdateActions(final Collection<Long> targetsIds) {
        // Figure out if there are potential target/action combinations that
        // need to be considered for cancellation
        final List<JpaAction> activeActions = actionRepository.findAll((root, query, cb) -> {
            root.fetch(JpaAction_.target, JoinType.LEFT);
            return cb.and(
                    cb.equal(root.get(JpaAction_.active), true),
                    cb.equal(root.get(JpaAction_.distributionSet).get(JpaDistributionSet_.requiredMigrationStep), false),
                    cb.notEqual(root.get(JpaAction_.status), Action.Status.CANCELING),
                    root.get(JpaAction_.target).get(AbstractJpaBaseEntity_.id).in(targetsIds)
            );
        });

        activeActions.forEach(action -> {
            action.setStatus(Status.CANCELING);

            // document that the status has been retrieved
            actionStatusRepository.save(new JpaActionStatus(action, Status.CANCELING, System.currentTimeMillis(),
                    RepositoryConstants.SERVER_MESSAGE_PREFIX + "cancel obsolete action due to new update"));
            actionRepository.save(action);
        });

        if (!activeActions.isEmpty()) {
            cancelAssignDistributionSetEvent(Collections.unmodifiableList(activeActions));
        }
    }

    /**
     * Closes {@link Action}s that are no longer necessary without sending a
     * hint to the controller.
     * <p/>
     * No access control applied
     *
     * @param targetsIds to override {@link Action}s
     */
    protected void closeObsoleteUpdateActions(final Collection<Long> targetsIds) {
        // Figure out if there are potential target/action combinations that
        // need to be considered for cancellation
        final List<JpaAction> activeActions = actionRepository.findAll((root, query, cb) -> {
            root.fetch(JpaAction_.target, JoinType.LEFT);
            return cb.and(
                    cb.equal(root.get(JpaAction_.active), true),
                    cb.equal(root.get(JpaAction_.distributionSet).get(JpaDistributionSet_.requiredMigrationStep), false),
                    root.get(JpaAction_.target).get(AbstractJpaBaseEntity_.id).in(targetsIds)
            );
        });

        activeActions.forEach(action -> {
            action.setStatus(Status.CANCELED);
            action.setActive(false);

            // document that the status has been retrieved
            actionStatusRepository.save(new JpaActionStatus(action, Status.CANCELED, System.currentTimeMillis(),
                    RepositoryConstants.SERVER_MESSAGE_PREFIX + "close obsolete action due to new update"));
            actionRepository.save(action);
        });
    }

    /**
     * Sends the {@link CancelTargetAssignmentEvent} for a specific action to
     * the eventPublisher.
     *
     * @param action the action of the assignment
     */
    protected void cancelAssignDistributionSetEvent(final Action action) {
        afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new CancelTargetAssignmentEvent(action)));
    }

    protected boolean isConfirmationFlowEnabled() {
        return confirmationFlowConfig.getAsBoolean();
    }

    /**
     * Find targets to be considered for assignment.
     *
     * @param controllerIDs as provided by repository caller
     * @param distributionSetId to assign
     * @return list of targets up to {@link Constants#MAX_ENTRIES_IN_STATEMENT}
     */
    abstract List<JpaTarget> findTargetsForAssignment(final List<String> controllerIDs, final long distributionSetId);

    abstract void sendTargetUpdatedEvents(final DistributionSet set, final List<JpaTarget> targets);

    /**
     * Update status and DS fields of given target.
     *
     * @param distributionSet to set
     * @param targetIds to change
     */
    abstract void setAssignedDistributionSetAndTargetStatus(final JpaDistributionSet distributionSet, final List<List<Long>> targetIds);

    /**
     * Cancels actions that can be canceled (i.e.
     * {@link DistributionSet#isRequiredMigrationStep() is <code>false</code>})
     * as a result of the new assignment and returns all {@link Target}s where
     * such actions existed.
     *
     * @param targetIds to cancel actions for
     */
    abstract void cancelActiveActions(List<List<Long>> targetIds);

    /**
     * Cancels actions that can be canceled (i.e.
     * {@link DistributionSet#isRequiredMigrationStep() is <code>false</code>})
     * as a result of the new assignment and returns all {@link Target}s where
     * such actions existed.
     *
     * @param targetIds to cancel actions for
     */
    abstract void closeActiveActions(List<List<Long>> targetIds);

    abstract void sendDeploymentEvents(final List<DistributionSetAssignmentResult> assignmentResults);

    private static String getActionMessage(final Action action) {
        final RolloutGroup rolloutGroup = action.getRolloutGroup();
        if (rolloutGroup != null) {
            final Rollout rollout = rolloutGroup.getRollout();
            return String.format("Initiated by Rollout Group '%s' [Rollout %s:%s]", rolloutGroup.getName(), rollout.getName(), rollout.getId());
        }
        return String.format("Assignment initiated by user '%s'", action.getInitiatedBy());
    }

    private void cancelAssignDistributionSetEvent(final List<Action> actions) {
        if (CollectionUtils.isEmpty(actions)) {
            return;
        }
        final String tenant = actions.get(0).getTenant();
        afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new CancelTargetAssignmentEvent(tenant, actions)));
    }

    private void assertActionsPerTargetQuota(final Target target) {
        final int quota = quotaManagement.getMaxActionsPerTarget();
        try {
            QuotaHelper.assertAssignmentQuota(target.getId(), 1, quota, Action.class, Target.class, actionRepository::countByTargetId);
        } catch (AssignmentQuotaExceededException e) {
            maxAssignmentExceededHandler.accept(new MaxAssignmentsExceededInfo(target.getId(), 1, e));
        }
    }
}