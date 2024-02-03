/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaActionStatusCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.ActionSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.eclipse.hawkbit.repository.model.Action.ActionType.DOWNLOAD_ONLY;
import static org.eclipse.hawkbit.repository.model.Action.Status.FINISHED;

/**
 * Implements utility methods for managing {@link Action}s
 */
@Slf4j
public class JpaActionManagement {

    protected final ActionRepository actionRepository;
    protected final ActionStatusRepository actionStatusRepository;
    protected final QuotaManagement quotaManagement;
    protected final RepositoryProperties repositoryProperties;

    public JpaActionManagement(final ActionRepository actionRepository,
            final ActionStatusRepository actionStatusRepository, final QuotaManagement quotaManagement,
            final RepositoryProperties repositoryProperties) {
        this.actionRepository = actionRepository;
        this.actionStatusRepository = actionStatusRepository;
        this.quotaManagement = quotaManagement;
        this.repositoryProperties = repositoryProperties;
    }

    List<Action> findActiveActionsWithHighestWeightConsideringDefault(final String controllerId,
            final int maxActionCount) {
        final List<Action> actions = new ArrayList<>();
        actions.addAll(
                actionRepository
                        .findAll(
                                ActionSpecifications.byTargetControllerIdAndActiveAndWeightIsNullFetchDS(controllerId, false),
                                PageRequest.of(
                                        0, maxActionCount,
                                        Sort.by(
                                                Sort.Order.desc(JpaAction_.WEIGHT),
                                                Sort.Order.asc(JpaAction_.ID))))
                        .getContent());

        actions.addAll(
                actionRepository
                        .findAll(
                                ActionSpecifications.byTargetControllerIdAndActiveAndWeightIsNullFetchDS(controllerId, true),
                                PageRequest.of(
                                        0, maxActionCount,
                                        Sort.by(
                                                Sort.Order.asc(JpaAction_.ID))))
                        .getContent());
        final Comparator<Action> actionImportance = Comparator.comparingInt(this::getWeightConsideringDefault)
                .reversed().thenComparing(Action::getId);
        return actions.stream().sorted(actionImportance).limit(maxActionCount).collect(Collectors.toList());
    }

    protected List<JpaAction> findActiveActionsHavingStatus(final String controllerId, final Action.Status status) {
        return actionRepository.findAll(
                ActionSpecifications.byTargetControllerIdAndIsActiveAndStatus(controllerId, status));
    }

    protected Action addActionStatus(final JpaActionStatusCreate statusCreate) {
        final Long actionId = statusCreate.getActionId();
        final JpaActionStatus actionStatus = statusCreate.build();
        final JpaAction action = getActionAndThrowExceptionIfNotFound(actionId);

        if (isUpdatingActionStatusAllowed(action, actionStatus)) {
            return handleAddUpdateActionStatus(actionStatus, action);
        }

        log.debug("Update of actionStatus {} for action {} not possible since action not active anymore.",
              actionStatus.getStatus(), action.getId());
        return action;
    }

    /**
     * ActionStatus updates are allowed mainly if the action is active. If the
     * action is not active we accept further status updates if permitted so by
     * repository configuration. In this case, only the values: Status.ERROR and
     * Status.FINISHED are allowed. In the case of a DOWNLOAD_ONLY action, we accept
     * status updates only once.
     */
    private boolean isUpdatingActionStatusAllowed(final JpaAction action, final JpaActionStatus actionStatus) {

        final boolean isIntermediateFeedback = (FINISHED != actionStatus.getStatus())
                && (Action.Status.ERROR != actionStatus.getStatus());

        final boolean isAllowedByRepositoryConfiguration = !repositoryProperties.isRejectActionStatusForClosedAction()
                && isIntermediateFeedback;

        final boolean isAllowedForDownloadOnlyActions = isDownloadOnly(action) && !isIntermediateFeedback;

        return action.isActive() || isAllowedByRepositoryConfiguration || isAllowedForDownloadOnlyActions;
    }

    public int getWeightConsideringDefault(final Action action) {
        return action.getWeight().orElse(repositoryProperties.getActionWeightIfAbsent());
    }

    protected JpaAction getActionAndThrowExceptionIfNotFound(final Long actionId) {
        return actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
    }

    protected static boolean isDownloadOnly(final JpaAction action) {
        return DOWNLOAD_ONLY == action.getActionType();
    }


    /**
     * Sets {@link TargetUpdateStatus} based on given {@link ActionStatus}.
     */
    private Action handleAddUpdateActionStatus(final JpaActionStatus actionStatus, final JpaAction action) {
        // information status entry - check for a potential DOS attack
        assertActionStatusQuota(action);
        assertActionStatusMessageQuota(actionStatus);
        actionStatus.setAction(action);

        onActionStatusUpdate(actionStatus.getStatus(), action);

        actionStatusRepository.save(actionStatus);

        action.setLastActionStatusCode(actionStatus.getCode().orElse(null));
        return actionRepository.save(action);
    }
    
    protected void onActionStatusUpdate(final Action.Status updatedActionStatus, final JpaAction action){
     // can be overwritten to intercept the persistence of the action status
    }
    
    protected void assertActionStatusQuota(final JpaAction action) {
        QuotaHelper.assertAssignmentQuota(action.getId(), 1, quotaManagement.getMaxStatusEntriesPerAction(),
                ActionStatus.class, Action.class, actionStatusRepository::countByActionId);
    }

    protected void assertActionStatusMessageQuota(final JpaActionStatus actionStatus) {
        QuotaHelper.assertAssignmentQuota(actionStatus.getId(), actionStatus.getMessages().size(),
                quotaManagement.getMaxMessagesPerActionStatus(), "Message", ActionStatus.class.getSimpleName(), null);
    }
}
