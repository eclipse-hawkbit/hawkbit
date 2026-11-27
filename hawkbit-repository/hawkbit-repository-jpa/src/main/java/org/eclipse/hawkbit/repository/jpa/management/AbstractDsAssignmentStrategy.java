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

import static org.eclipse.hawkbit.repository.jpa.acm.AccessController.Operation.UPDATE;
import static org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor.afterCommit;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_STATUS_CODE_CANCELED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_STATUS_CODE_CANCELING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.JoinType;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.Jpa;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.utils.ServerActionStatusCode;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DistributionSet} to {@link Target} assignment strategy as utility for
 * {@link JpaDeploymentManagement}.
 */
@Slf4j
public abstract class AbstractDsAssignmentStrategy {

    private static final Integer CANCELING_DB_VALUE = new JpaAction.StatusConverter().convertToDatabaseColumn(Status.CANCELING);

    protected final TargetRepository targetRepository;
    protected final ActionRepository actionRepository;

    private final ActionStatusRepository actionStatusRepository;
    private final EntityManager entityManager;
    private final BooleanSupplier confirmationFlowConfig;
    private final RepositoryProperties repositoryProperties;

    @SuppressWarnings("java:S107")
    AbstractDsAssignmentStrategy(
            final TargetRepository targetRepository,
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository,
            final EntityManager entityManager,
            final BooleanSupplier confirmationFlowConfig, final RepositoryProperties repositoryProperties) {
        this.targetRepository = targetRepository;
        this.actionRepository = actionRepository;
        this.actionStatusRepository = actionStatusRepository;
        this.entityManager = entityManager;
        this.confirmationFlowConfig = confirmationFlowConfig;
        this.repositoryProperties = repositoryProperties;
    }

    /**
     * Builds (does not persist) the assignment action. Pure factory - does NOT enforce the actions-per-target quota;
     * callers must validate it beforehand (e.g. {@code JpaDeploymentManagement#enforceMaxActionsPerTarget}
     * for manual assignment, {@code JpaRolloutExecutor#assertActionsPerTargetQuota} for rollouts).
     */
    public JpaAction createTargetAction(
            final TargetWithActionType targetWithActionType, final List<JpaTarget> targets, final JpaDistributionSet set,
            final boolean confirmationFlowEnabled) {
        final Optional<JpaTarget> optTarget = targets.stream()
                .filter(t -> t.getControllerId().equals(targetWithActionType.getControllerId())).findFirst();

        return optTarget.map(target -> {
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
            if (StringUtils.hasText(targetWithActionType.getExternalRef())) {
                actionForTarget.setExternalRef(targetWithActionType.getExternalRef());
            }
            actionForTarget.setInitiatedBy(AccessContext.actor());
            return actionForTarget;
        }).orElseGet(() -> {
            log.warn("Cannot find target for targetWithActionType '{}'.", targetWithActionType.getControllerId());
            return null;
        });
    }

    /**
     * Builds (does not persist) the initial action status. Pure factory - does NOT enforce the status-per-action quota
     * ({@code maxStatusEntriesPerAction}); that quota guards repeated device-feedback appends and is enforced at
     * ({@code JpaActionManagement#assertActionStatusQuota}).
     * This initial status is the first entry, so exempt.
     */
    public JpaActionStatus createActionStatus(final JpaAction action, final String actionMessage, final boolean confirmationFlowEnabled) {
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
     * Actions of targets without read access are skipped, update access to the targets of the others is asserted.
     *
     * @param targetsIds to override {@link Action}s
     */
    protected void overrideObsoleteUpdateActions(final Collection<Long> targetsIds) {
        if (targetsIds.isEmpty()) {
            return;
        }
        // write the pending changes first, so that the reads and the bulk updates below see them and no pending change of an
        // obsolete action is written (with an outdated version) after its bulk update
        entityManager.flush();
        // in pages (keyset by id), so that the obsolete actions are never all in memory. The cancelled actions don't match the
        // read anymore (CANCELING), the ones skipped because of a concurrent change are behind the last read id
        long lastId = 0;
        List<JpaAction> page;
        do {
            page = findObsoleteUpdateActions(targetsIds, lastId);
            if (!page.isEmpty()) {
                lastId = page.getLast().getId();
                cancelObsoleteUpdateActions(page);
            }
        } while (page.size() >= Constants.MAX_ENTRIES_IN_STATEMENT);
    }

    // The active, not yet canceling actions of the targets that can be cancelled (no required migration step). Through the
    // repository proxy, so the read access rules apply (actions of not readable targets are not cancelled). With their target
    // (cancel event: controller id) fetched in the same query, as a JOIN FETCH and not an entity graph, as EclipseLink loads
    // entity graph associations one by one. No COUNT query is issued.
    private List<JpaAction> findObsoleteUpdateActions(final Collection<Long> targetsIds, final long fromId) {
        return actionRepository.findAllWithoutCount((root, _, cb) -> {
            root.fetch(JpaAction_.target, JoinType.LEFT);
            // the action update events need the rollout and rollout group ids - the actions are detached when the events are built
            root.fetch(JpaAction_.rollout, JoinType.LEFT);
            root.fetch(JpaAction_.rolloutGroup, JoinType.LEFT);
            return cb.and(
                    cb.equal(root.get(JpaAction_.active), true),
                    cb.equal(root.get(JpaAction_.distributionSet).get(JpaDistributionSet_.requiredMigrationStep), false),
                    cb.notEqual(root.get(JpaAction_.status), Action.Status.CANCELING),
                    root.get(JpaAction_.target).get(AbstractJpaBaseEntity_.id).in(targetsIds),
                    cb.greaterThan(root.get(AbstractJpaBaseEntity_.id), fromId));
        }, PageRequest.of(0, Constants.MAX_ENTRIES_IN_STATEMENT, Sort.by(AbstractJpaBaseEntity_.ID))).getContent();
    }

    // Cancels the page with one bulk UPDATE instead of an entity update per action: fewer statements and no per-action version
    // check, which a concurrent device update of an obsolete action would fail (rolling back the whole batch). The UPDATE repeats
    // the conditions (active, not canceling), so an action changed concurrently in between (e.g. finished by the device) is
    // skipped, and increments the version, so a stale device-side save fails instead of overwriting the cancellation.
    // Bulk updates bypass the entity callbacks, so the ActionUpdatedEvents are published explicitly.
    private void cancelObsoleteUpdateActions(final List<JpaAction> page) {
        // the update access check the per-action save did (i.e. update access to the targets)
        actionRepository.getAccessController().ifPresent(accessController -> accessController.assertOperationAllowed(UPDATE, page));

        // the actions still to cancel, locked until the commit - so exactly these are updated below and get a status and events
        // here, not in findObsoleteUpdateActions because lock there will lock joins or fail
        final Set<Long> lockedIds = new HashSet<>(lockCancelable(page.stream().map(JpaAction::getId).toList()));
        if (lockedIds.size() < page.size()) {
            log.debug("{} of {} obsolete actions changed concurrently, not cancelled", page.size() - lockedIds.size(), page.size());
        }
        // the managed actions get stale with the bulk update - detach them, so they are not written again (with an outdated
        // version) and a later read in this transaction gets the current state, and set the new state for the events
        page.forEach(entityManager::detach);
        final List<JpaAction> toCancel = page.stream().filter(action -> lockedIds.contains(action.getId())).toList();
        if (toCancel.isEmpty()) {
            return;
        }
        final long now = System.currentTimeMillis();
        final String actor = AccessContext.actor();
        final List<JpaActionStatus> statuses = new ArrayList<>(toCancel.size());
        Integer code = null;
        for (final JpaAction action : toCancel) {
            action.setStatus(Status.CANCELING);
            action.setLastModifiedAt(now);
            action.setLastModifiedBy(actor);
            // document that the status has been retrieved
            final JpaActionStatus actionStatus = new JpaActionStatus(action, Status.CANCELING, now,
                    RepositoryConstants.SERVER_MESSAGE_PREFIX + "cancel obsolete action due to new update");
            // sets the code on the status and the last action status code on the (detached) action. The code is per tenant,
            // so the same for all actions
            if (ServerActionStatusCode.apply(actionStatus, ACTION_STATUS_CODE_CANCELING)) {
                code = actionStatus.getCode().orElse(null);
            }
            statuses.add(actionStatus);
        }

        final List<Long> ids = toCancel.stream().map(JpaAction::getId).toList();
        if (code == null) {
            actionRepository.setStatusOfActiveByIdsAndStatusNot(ids, Status.CANCELING, now, actor);
        } else {
            actionRepository.setStatusAndLastActionStatusCodeOfActiveByIdsAndStatusNot(ids, Status.CANCELING, code, now, actor);
        }

        // a reference instead of the detached action, so that persisting the status doesn't involve (merge / write) the action
        statuses.forEach(status -> status.setAction(entityManager.getReference(JpaAction.class, status.getAction().getId())));
        actionStatusRepository.saveAll(statuses);

        cancelAssignDistributionSetEvent(Collections.unmodifiableList(toCancel));
        toCancel.forEach(action -> afterCommit(action::fireUpdateEvent));
    }

    // Locks the actions of the given ids that are still active and not canceling (i.e. not changed concurrently since they were
    // read), in id order, and returns their ids. The bulk update locks the same rows until the commit anyway - locking them before
    // gives the exact set it changes, also on READ COMMITTED where an action set to canceling by a concurrent transaction would
    // otherwise look like cancelled by this one. Native, as JPA can't lock on an ids-only query; sp_action only, by primary key.
    private List<Long> lockCancelable(final List<Long> ids) {
        final String prefix = String.valueOf(Jpa.nativeQueryParamPrefix());
        final Query lockQuery = entityManager.createNativeQuery(
                "SELECT id FROM sp_action WHERE id IN (" + Jpa.formatNativeQueryInClause("id", ids) + ")" +
                        " AND active = " + prefix + "active AND status <> " + prefix + "canceling ORDER BY id FOR UPDATE");
        Jpa.setNativeQueryInParameter(lockQuery, "id", ids);
        lockQuery.setParameter("active", true);
        lockQuery.setParameter("canceling", CANCELING_DB_VALUE);
        return ((List<?>) lockQuery.getResultList()).stream().map(id -> ((Number) id).longValue()).toList();
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
            final JpaActionStatus actionStatus = new JpaActionStatus(action, Status.CANCELED, System.currentTimeMillis(),
                    RepositoryConstants.SERVER_MESSAGE_PREFIX + "close obsolete action due to new update");
            ServerActionStatusCode.apply(actionStatus, ACTION_STATUS_CODE_CANCELED);
            actionStatusRepository.save(actionStatus);
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
        // the event is created in the transaction - after commit the session is closed and the lazy action relations (e.g. target)
        // could not be loaded (Hibernate). The canceled actions exist (have ids), so the event could be created before the flush
        final CancelTargetAssignmentEvent event = new CancelTargetAssignmentEvent(action);
        afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher().publishEvent(event));
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
        // the event is created in the transaction - after commit the session is closed and the lazy action relations (e.g. target)
        // could not be loaded (Hibernate). The canceled actions exist (have ids), so the event could be created before the flush
        final CancelTargetAssignmentEvent event = new CancelTargetAssignmentEvent(tenant, actions);
        afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher().publishEvent(event));
    }
}
