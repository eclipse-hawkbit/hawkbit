/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.exception.AutoConfirmationAlreadyActiveException;
import org.eclipse.hawkbit.repository.exception.InvalidConfirmationFeedbackException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.builder.JpaActionStatusCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaAutoConfirmationStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation for {@link ConfirmationManagement}.
 */
@Slf4j
@Transactional(readOnly = true)
@Validated
public class JpaConfirmationManagement extends JpaActionManagement implements ConfirmationManagement {

    public static final String CONFIRMATION_CODE_MSG_PREFIX = "Confirmation status code: %d";

    private final EntityManager entityManager;
    private final EntityFactory entityFactory;
    private final TargetRepository targetRepository;

    public JpaConfirmationManagement(
            final TargetRepository targetRepository,
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository,
            final RepositoryProperties repositoryProperties, final QuotaManagement quotaManagement,
            final EntityManager entityManager, final EntityFactory entityFactory) {
        super(actionRepository, actionStatusRepository, quotaManagement, repositoryProperties);
        this.targetRepository = targetRepository;
        this.entityManager = entityManager;
        this.entityFactory = entityFactory;
    }

    @Override
    @Transactional
    public AutoConfirmationStatus activateAutoConfirmation(final String controllerId, final String initiator, final String remark) {
        log.trace(
                "'activateAutoConfirmation' was called with values: controllerId={}; initiator={}; remark={}", controllerId, initiator, remark);
        final JpaTarget target = targetRepository.getWithDetailsByControllerId(controllerId, JpaTarget_.GRAPH_TARGET_AUTO_CONFIRMATION_STATUS);
        if (target.getAutoConfirmationStatus() != null) {
            log.debug("'activateAutoConfirmation' was called for an controller id {} with active auto confirmation.", controllerId);
            throw new AutoConfirmationAlreadyActiveException(controllerId);
        }
        final JpaAutoConfirmationStatus confirmationStatus = new JpaAutoConfirmationStatus(initiator, remark, target);
        target.setAutoConfirmationStatus(confirmationStatus);
        // since the status is not part of the JpaTarget table (just ref) it might be needed to touch the entity to have updated lastModifiedAt
        JpaManagementHelper.touch(entityManager, targetRepository, target);
        final JpaTarget updatedTarget = targetRepository.save(target);
        final AutoConfirmationStatus autoConfStatus = updatedTarget.getAutoConfirmationStatus();
        if (autoConfStatus == null) {
            final String message = String.format(
                    "Persisted auto confirmation status is null. " +
                            "Cannot proceed with giving confirmations for active actions for device %s with initiator %s.",
                    controllerId, initiator);
            log.error("message");
            throw new IllegalStateException(message);
        }
        giveConfirmationForActiveActions(autoConfStatus);
        return autoConfStatus;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action confirmAction(final long actionId, final Integer code, final Collection<String> deviceMessages) {
        log.trace("Action with id {} confirm request is triggered.", actionId);
        final Action action = getActionAndThrowExceptionIfNotFound(actionId);
        assertActionCanAcceptFeedback(action);
        final List<String> messages = new ArrayList<>();
        if (deviceMessages != null) {
            messages.addAll(deviceMessages);
        }
        messages.add(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target confirmed action."
                + " Therefore, it will be set to the running state to proceed with the deployment.");
        final ActionStatusCreate statusCreate = createConfirmationActionStatus(action.getId(), code, messages)
                .status(Status.RUNNING);
        return addActionStatus((JpaActionStatusCreate) statusCreate);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action denyAction(final long actionId, final Integer code, final Collection<String> deviceMessages) {
        log.trace("Action with id {} deny request is triggered.", actionId);
        final Action action = getActionAndThrowExceptionIfNotFound(actionId);
        assertActionCanAcceptFeedback(action);
        final List<String> messages = new ArrayList<>();
        if (deviceMessages != null) {
            messages.addAll(deviceMessages);
        }
        messages.add(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target rejected action. Action will stay in confirmation pending state.");
        final ActionStatusCreate statusCreate = createConfirmationActionStatus(action.getId(), code, messages)
                .status(Status.WAIT_FOR_CONFIRMATION);
        return addActionStatus((JpaActionStatusCreate) statusCreate);
    }

    @Override
    @Transactional
    public void deactivateAutoConfirmation(String controllerId) {
        log.debug("Deactivate auto confirmation for controllerId '{}'", controllerId);
        final JpaTarget target = targetRepository.getByControllerId(controllerId);
        target.setAutoConfirmationStatus(null);
        // since the status is not part of the JpaTarget table (just ref) it might be needed to touch the entity to have updated lastModifiedAt
        JpaManagementHelper.touch(entityManager, targetRepository, target);
        targetRepository.save(target);
    }

    @Override
    public Optional<AutoConfirmationStatus> getStatus(final String controllerId) {
        return Optional.of(targetRepository.getWithDetailsByControllerId(controllerId, JpaTarget_.GRAPH_TARGET_AUTO_CONFIRMATION_STATUS))
                .map(JpaTarget::getAutoConfirmationStatus);
    }

    @Override
    public List<Action> findActiveActionsWaitingConfirmation(final String controllerId) {
        return Collections.unmodifiableList(findActiveActionsHavingStatus(controllerId, Status.WAIT_FOR_CONFIRMATION));
    }

    @Override
    protected void onActionStatusUpdate(final JpaActionStatus newActionStatus, final JpaAction action) {
        if (newActionStatus.getStatus() == Status.RUNNING && action.isActive()) {
            action.setStatus(Status.RUNNING);
        }
    }

    private static void assertActionCanAcceptFeedback(final Action action) {
        if (!action.isActive()) {
            final String msg = String.format(
                    "Confirming action %s is not possible since the action is not active anymore.", action.getId());
            log.warn(msg);
            throw new InvalidConfirmationFeedbackException(InvalidConfirmationFeedbackException.Reason.ACTION_CLOSED,
                    msg);
        } else if (!action.isWaitingConfirmation()) {
            log.debug("Action is not waiting for confirmation, deny request.");
            final String msg = String.format("Action %s is not waiting for confirmation.", action.getId());
            log.warn(msg);
            throw new InvalidConfirmationFeedbackException(
                    InvalidConfirmationFeedbackException.Reason.NOT_AWAITING_CONFIRMATION, msg);
        }
    }

    private ActionStatusCreate createConfirmationActionStatus(final long actionId, final Integer code, final Collection<String> messages) {
        final ActionStatusCreate statusCreate = entityFactory.actionStatus().create(actionId);
        if (!CollectionUtils.isEmpty(messages)) {
            statusCreate.messages(messages);
        }
        if (code != null) {
            statusCreate.code(code);
            statusCreate.message(String.format(CONFIRMATION_CODE_MSG_PREFIX, code));
        }
        return statusCreate;
    }

    private List<Action> giveConfirmationForActiveActions(final AutoConfirmationStatus autoConfirmationStatus) {
        return findActiveActionsHavingStatus(autoConfirmationStatus.getTarget().getControllerId(), Status.WAIT_FOR_CONFIRMATION).stream()
                .map(action -> autoConfirmAction(action, autoConfirmationStatus))
                .toList();
    }

    private Action autoConfirmAction(final JpaAction action, final AutoConfirmationStatus autoConfirmationStatus) {
        if (!action.isWaitingConfirmation()) {
            log.debug("Auto-confirming action is not necessary, since action {} is in RUNNING state already.", action.getId());
            return action;
        }
        final JpaActionStatus actionStatus = (JpaActionStatus) entityFactory.actionStatus()
                .create(action.getId())
                .status(Status.RUNNING)
                .messages(Collections.singletonList(autoConfirmationStatus.constructActionMessage()))
                .build();
        log.debug(
                "Automatically confirm actionId '{}' due to active auto-confirmation initiated by '{}' and rollouts system user '{}'",
                action.getId(), autoConfirmationStatus.getInitiator(), autoConfirmationStatus.getCreatedBy());

        // do not make use of org.eclipse.hawkbit.repository.jpa.management.JpaActionManagement.handleAddUpdateActionStatus
        // to bypass the quota check. Otherwise, the action will not be confirmed in case of exceeded action status quota.
        action.setStatus(Status.RUNNING);
        actionStatus.setAction(action);

        actionStatusRepository.save(actionStatus);
        return actionRepository.save(action);
    }
}