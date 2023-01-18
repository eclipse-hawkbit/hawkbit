/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.exception.AutoConfirmationAlreadyActiveException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidConfirmationFeedbackException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaActionStatusCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaAutoConfirmationStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation for {@link ConfirmationManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaConfirmationManagement extends JpaActionManagement implements ConfirmationManagement {

    public static final String CONFIRMATION_CODE_MSG_PREFIX = "Confirmation status code: %d";

    private static final Logger LOG = LoggerFactory.getLogger(JpaConfirmationManagement.class);

    private final EntityFactory entityFactory;
    private final TargetRepository targetRepository;

    /**
     * Constructor
     */
    protected JpaConfirmationManagement(final TargetRepository targetRepository,
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository,
            final RepositoryProperties repositoryProperties, final QuotaManagement quotaManagement,
            final EntityFactory entityFactory) {
        super(actionRepository, actionStatusRepository, quotaManagement, repositoryProperties);
        this.targetRepository = targetRepository;
        this.entityFactory = entityFactory;
    }

    @Override
    public List<Action> findActiveActionsWaitingConfirmation(final String controllerId) {
        return Collections.unmodifiableList(findActiveActionsHavingStatus(controllerId, Status.WAIT_FOR_CONFIRMATION));
    }

    @Override
    @Transactional
    public AutoConfirmationStatus activateAutoConfirmation(final String controllerId, final String initiator,
            final String remark) {
        LOG.trace("'activateAutoConfirmation' was called with values: controllerId={}; initiator={}; remark={}",
                controllerId, initiator, remark);
        final JpaTarget target = getTargetByControllerIdAndThrowIfNotFound(controllerId);
        if (target.getAutoConfirmationStatus() != null) {
            LOG.debug("'activateAutoConfirmation' was called for an controller id {} with active auto confirmation.",
                    controllerId);
            throw new AutoConfirmationAlreadyActiveException(controllerId);
        }
        final JpaAutoConfirmationStatus confirmationStatus = new JpaAutoConfirmationStatus(initiator, remark, target);
        target.setAutoConfirmationStatus(confirmationStatus);
        final JpaTarget updatedTarget = targetRepository.save(target);
        final AutoConfirmationStatus autoConfStatus = updatedTarget.getAutoConfirmationStatus();
        if (autoConfStatus == null) {
            final String message = String.format("Persisted auto confirmation status is null. "
                    + "Cannot proceed with giving confirmations for active actions for device %s with initiator %s.",
                    controllerId, initiator);
            LOG.error("message");
            throw new IllegalStateException(message);
        }
        giveConfirmationForActiveActions(autoConfStatus);
        return autoConfStatus;
    }

    @Override
    public Optional<AutoConfirmationStatus> getStatus(final String controllerId) {
        return Optional.of(getTargetByControllerIdAndThrowIfNotFound(controllerId)).map(JpaTarget::getAutoConfirmationStatus);
    }

    @Override
    @Transactional
    public List<Action> autoConfirmActiveActions(final String controllerId) {
        final JpaTarget target = getTargetByControllerIdAndThrowIfNotFound(controllerId);
        if (target.getAutoConfirmationStatus() == null) {
            // auto-confirmation is not active
            return Collections.emptyList();
        }
        return giveConfirmationForActiveActions(target.getAutoConfirmationStatus());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action confirmAction(final long actionId, final Integer code, final Collection<String> deviceMessages) {
        LOG.trace("Action with id {} confirm request is triggered.", actionId);
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
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action denyAction(final long actionId, final Integer code, final Collection<String> deviceMessages) {
        LOG.trace("Action with id {} deny request is triggered.", actionId);
        final Action action = getActionAndThrowExceptionIfNotFound(actionId);
        assertActionCanAcceptFeedback(action);
        final List<String> messages = new ArrayList<>();
        if (deviceMessages != null) {
            messages.addAll(deviceMessages);
        }
        messages.add(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target rejected action."
                + " Action will stay in confirmation pending state.");
        final ActionStatusCreate statusCreate = createConfirmationActionStatus(action.getId(), code, messages)
                .status(Status.WAIT_FOR_CONFIRMATION);
        return addActionStatus((JpaActionStatusCreate) statusCreate);
    }

    private ActionStatusCreate createConfirmationActionStatus(final long actionId, final Integer code,
            final Collection<String> messages) {
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

    private static void assertActionCanAcceptFeedback(final Action action) {
        if (!action.isActive()) {
            final String msg = String.format(
                    "Confirming action %s is not possible since the action is not active anymore.", action.getId());
            LOG.warn(msg);
            throw new InvalidConfirmationFeedbackException(InvalidConfirmationFeedbackException.Reason.ACTION_CLOSED,
                    msg);
        } else if (!action.isWaitingConfirmation()) {
            LOG.debug("Action is not waiting for confirmation, deny request.");
            final String msg = String.format("Action %s is not waiting for confirmation.", action.getId());
            LOG.warn(msg);
            throw new InvalidConfirmationFeedbackException(
                    InvalidConfirmationFeedbackException.Reason.NOT_AWAITING_CONFIRMATION, msg);
        }
    }

    private List<Action> giveConfirmationForActiveActions(final AutoConfirmationStatus autoConfirmationStatus) {
        final Target target = autoConfirmationStatus.getTarget();
        return findActiveActionsHavingStatus(target.getControllerId(), Status.WAIT_FOR_CONFIRMATION).stream()
                .map(action -> autoConfirmAction(action, autoConfirmationStatus)).collect(Collectors.toList());
    }

    private Action autoConfirmAction(final JpaAction action, final AutoConfirmationStatus autoConfirmationStatus) {
        if (!action.isWaitingConfirmation()) {
            LOG.debug("Auto-confirming action is not necessary, since action {} is in RUNNING state already.",
                    action.getId());
            return action;
        }
        final JpaActionStatus actionStatus = (JpaActionStatus) entityFactory.actionStatus().create(action.getId())
                .status(Status.RUNNING)
                .messages(Collections.singletonList(autoConfirmationStatus.constructActionMessage())).build();
        LOG.debug(
                "Automatically confirm actionId '{}' due to active auto-confirmation initiated by '{}' and rollouts system user '{}'",
                action.getId(), autoConfirmationStatus.getInitiator(), autoConfirmationStatus.getCreatedBy());

        // do not make use of
        // org.eclipse.hawkbit.repository.jpa.JpaActionManagement.handleAddUpdateActionStatus
        // to bypass the quota check. Otherwise the action will not be confirmed in case
        // of exceeded action status quota.
        action.setStatus(Status.RUNNING);
        actionStatus.setAction(action);

        actionStatusRepository.save(actionStatus);
        return actionRepository.save(action);
    }

    @Override
    @Transactional
    public void deactivateAutoConfirmation(String controllerId) {
        LOG.debug("Deactivate auto confirmation for controllerId '{}'", controllerId);
        final JpaTarget target = getTargetByControllerIdAndThrowIfNotFound(controllerId);
        target.setAutoConfirmationStatus(null);
        targetRepository.save(target);
    }

    private JpaTarget getTargetByControllerIdAndThrowIfNotFound(final String controllerId) {
        return targetRepository.findOne(TargetSpecifications.hasControllerId(controllerId))
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    @Override
    protected void onActionStatusUpdate(final Status updatedActionStatus, final JpaAction action) {
        if (updatedActionStatus == Status.RUNNING && action.isActive()) {
            action.setStatus(Status.RUNNING);
        }
    }
}
