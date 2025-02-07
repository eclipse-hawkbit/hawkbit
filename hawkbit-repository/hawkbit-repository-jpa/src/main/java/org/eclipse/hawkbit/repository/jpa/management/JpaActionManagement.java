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

import static org.eclipse.hawkbit.repository.model.Action.ActionType.DOWNLOAD_ONLY;
import static org.eclipse.hawkbit.repository.model.Action.Status.ERROR;
import static org.eclipse.hawkbit.repository.model.Action.Status.FINISHED;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaActionStatusCreate;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
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

/**
 * Implements utility methods for managing {@link Action}s
 */
@Slf4j
public class JpaActionManagement {

    protected final ActionRepository actionRepository;
    protected final ActionStatusRepository actionStatusRepository;
    protected final QuotaManagement quotaManagement;
    protected final RepositoryProperties repositoryProperties;

    public JpaActionManagement(
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository, final QuotaManagement quotaManagement,
            final RepositoryProperties repositoryProperties) {
        this.actionRepository = actionRepository;
        this.actionStatusRepository = actionStatusRepository;
        this.quotaManagement = quotaManagement;
        this.repositoryProperties = repositoryProperties;
    }

    public int getWeightConsideringDefault(final Action action) {
        return action.getWeight().orElse(repositoryProperties.getActionWeightIfAbsent());
    }

    protected static boolean isDownloadOnly(final JpaAction action) {
        return DOWNLOAD_ONLY == action.getActionType();
    }

    protected List<JpaAction> findActiveActionsHavingStatus(final String controllerId, final Action.Status status) {
        return actionRepository.findAll(ActionSpecifications.byTargetControllerIdAndIsActiveAndStatus(controllerId, status));
    }

    protected Action addActionStatus(final JpaActionStatusCreate statusCreate) {
        final Long actionId = statusCreate.getActionId();
        final JpaActionStatus actionStatus = statusCreate.build();
        final JpaAction action = getActionAndThrowExceptionIfNotFound(actionId);

        if (isUpdatingActionStatusAllowed(action, actionStatus)) {
            return handleAddUpdateActionStatus(actionStatus, action);
        }

        log.debug(
                "Update of actionStatus {} for action {} not possible since action not active anymore and not allowed as an action terminating.",
                actionStatus.getStatus(), action.getId());
        return action;
    }

    protected JpaAction getActionAndThrowExceptionIfNotFound(final Long actionId) {
        return actionRepository.findById(actionId).orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
    }

    protected void onActionStatusUpdate(final JpaActionStatus newActionStatus, final JpaAction action) {
        // can be overwritten to intercept the persistence of the action status
    }

    protected void assertActionStatusQuota(final JpaActionStatus newActionStatus, final JpaAction action) {
        if (isIntermediateStatus(newActionStatus)) {// check for quota only for intermediate statuses
            QuotaHelper.assertAssignmentQuota(action.getId(), 1, quotaManagement.getMaxStatusEntriesPerAction(),
                    ActionStatus.class, Action.class, actionStatusRepository::countByActionId);
        }
    }

    protected void assertActionStatusMessageQuota(final JpaActionStatus actionStatus) {
        QuotaHelper.assertAssignmentQuota(actionStatus.getId(), actionStatus.getMessages().size(),
                quotaManagement.getMaxMessagesPerActionStatus(), "Message", ActionStatus.class.getSimpleName(), null);
    }

    protected List<Action> findActiveActionsWithHighestWeightConsideringDefault(final String controllerId, final int maxActionCount) {
        return Stream.concat(
                        // get the highest actions with weight
                        actionRepository.findAll(
                                ActionSpecifications.byTargetControllerIdAndActiveAndWeightIsNull(controllerId, false),
                                "Action.ds",
                                PageRequest.of(
                                        0, maxActionCount,
                                        Sort.by(Sort.Order.desc(JpaAction_.WEIGHT), Sort.Order.asc(AbstractJpaBaseEntity_.ID)))).stream(),
                        // get the oldest actions without weight
                        actionRepository.findAll(
                                ActionSpecifications.byTargetControllerIdAndActiveAndWeightIsNull(controllerId, true),
                                "Action.ds",
                                PageRequest.of(0, maxActionCount, Sort.by(Sort.Order.asc(AbstractJpaBaseEntity_.ID)))).stream())
                .sorted(Comparator.comparingInt(this::getWeightConsideringDefault).reversed().thenComparing(Action::getId))
                .limit(maxActionCount)
                .map(Action.class::cast)
                .toList();
    }

    private static boolean isIntermediateStatus(final JpaActionStatus actionStatus) {
        return FINISHED != actionStatus.getStatus() && ERROR != actionStatus.getStatus();
    }

    /**
     * ActionStatus updates are allowed mainly if the action is active. If the
     * action is not active we accept further status updates if permitted so by
     * repository configuration. In this case, only the values: Status.ERROR and
     * Status.FINISHED are allowed. In the case of a DOWNLOAD_ONLY action, we accept
     * status updates only once.
     */
    private boolean isUpdatingActionStatusAllowed(final JpaAction action, final JpaActionStatus actionStatus) {
        if (action.isActive()) {
            return true;
        }

        if (isIntermediateStatus(actionStatus)) {
            return !repositoryProperties.isRejectActionStatusForClosedAction();
        } else {
            // in case of download_only action Status#DOWNLOADED is treated as 'final' already,
            // so we accept one final status from device in case it sends
            return isDownloadOnly(action) && action.getStatus() == Action.Status.DOWNLOADED;
        }
    }

    /**
     * Sets {@link TargetUpdateStatus} based on given {@link ActionStatus}.
     */
    private Action handleAddUpdateActionStatus(final JpaActionStatus actionStatus, final JpaAction action) {
        // information status entry - check for a potential DOS attack
        assertActionStatusQuota(actionStatus, action);
        assertActionStatusMessageQuota(actionStatus);
        actionStatus.setAction(action);

        onActionStatusUpdate(actionStatus, action);

        actionStatusRepository.save(actionStatus);

        action.setLastActionStatusCode(actionStatus.getCode().orElse(null));
        return actionRepository.save(action);
    }
}