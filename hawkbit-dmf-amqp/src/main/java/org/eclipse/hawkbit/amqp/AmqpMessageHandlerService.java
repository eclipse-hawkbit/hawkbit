/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.ActionUpdateStatus;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.eventbus.event.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.eclipse.hawkbit.repository.exception.TooManyStatusEntriesException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 *
 * {@link AmqpMessageHandlerService} handles all incoming target interaction
 * AMQP messages (e.g. create target, check for updates etc.) for the queue
 * which is configured for the property hawkbit.dmf.rabbitmq.receiverQueue.
 *
 */
public class AmqpMessageHandlerService extends BaseAmqpService {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpMessageHandlerService.class);

    private final AmqpMessageDispatcherService amqpMessageDispatcherService;

    private final ControllerManagement controllerManagement;

    private final EntityFactory entityFactory;

    /**
     * Constructor.
     * 
     * @param rabbitTemplate
     *            for converting messages
     * @param amqpMessageDispatcherService
     *            to sending events to DMF client
     * @param controllerManagement
     *            for target repo access
     * @param entityFactory
     *            to create entities
     */
    public AmqpMessageHandlerService(final RabbitTemplate rabbitTemplate,
            final AmqpMessageDispatcherService amqpMessageDispatcherService,
            final ControllerManagement controllerManagement, final EntityFactory entityFactory) {
        super(rabbitTemplate);
        this.amqpMessageDispatcherService = amqpMessageDispatcherService;
        this.controllerManagement = controllerManagement;
        this.entityFactory = entityFactory;
    }

    /**
     * Method to handle all incoming DMF amqp messages.
     *
     * @param message
     *            incoming message
     * @param type
     *            the message type
     * @param tenant
     *            the contentType of the message
     * 
     * @return a message if <null> no message is send back to sender
     */
    @RabbitListener(queues = "${hawkbit.dmf.rabbitmq.receiverQueue}", containerFactory = "listenerContainerFactory")
    public Message onMessage(final Message message, @Header(MessageHeaderKey.TYPE) final String type,
            @Header(MessageHeaderKey.TENANT) final String tenant) {
        return onMessage(message, type, tenant, getRabbitTemplate().getConnectionFactory().getVirtualHost());
    }

    /**
     * * Executed if a amqp message arrives.
     * 
     * @param message
     *            the message
     * @param type
     *            the type
     * @param tenant
     *            the tenant
     * @param virtualHost
     *            the virtual host
     * @return the rpc message back to supplier.
     */
    public Message onMessage(final Message message, final String type, final String tenant, final String virtualHost) {
        checkContentTypeJson(message);
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        try {
            final MessageType messageType = MessageType.valueOf(type);
            switch (messageType) {
            case THING_CREATED:
                setTenantSecurityContext(tenant);
                registerTarget(message, virtualHost);
                break;
            case EVENT:
                setTenantSecurityContext(tenant);
                final String topicValue = getStringHeaderKey(message, MessageHeaderKey.TOPIC, "EventTopic is null");
                final EventTopic eventTopic = EventTopic.valueOf(topicValue);
                handleIncomingEvent(message, eventTopic);
                break;
            default:
                logAndThrowMessageError(message, "No handle method was found for the given message type.");
            }
        } catch (final IllegalArgumentException ex) {
            throw new AmqpRejectAndDontRequeueException("Invalid message!", ex);
        } catch (final TenantNotExistException | TooManyStatusEntriesException e) {
            throw new AmqpRejectAndDontRequeueException(e);
        } finally {
            SecurityContextHolder.setContext(oldContext);
        }
        return null;
    }

    private static void setSecurityContext(final Authentication authentication) {
        final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
        securityContextImpl.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContextImpl);
    }

    private static void setTenantSecurityContext(final String tenantId) {
        final AnonymousAuthenticationToken authenticationToken = new AnonymousAuthenticationToken(
                UUID.randomUUID().toString(), "AMQP-Controller",
                Collections.singletonList(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS)));
        authenticationToken.setDetails(new TenantAwareAuthenticationDetails(tenantId, true));
        setSecurityContext(authenticationToken);
    }

    /**
     * Method to create a new target or to find the target if it already exists.
     *
     * @param targetID
     *            the ID of the target/thing
     * @param ip
     *            the ip of the target/thing
     */
    private void registerTarget(final Message message, final String virtualHost) {
        final String thingId = getStringHeaderKey(message, MessageHeaderKey.THING_ID, "ThingId is null");
        final String replyTo = message.getMessageProperties().getReplyTo();

        if (StringUtils.isEmpty(replyTo)) {
            logAndThrowMessageError(message, "No ReplyTo was set for the createThing Event.");
        }

        final URI amqpUri = IpUtil.createAmqpUri(virtualHost, replyTo);
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotexist(thingId, amqpUri);
        LOG.debug("Target {} reported online state.", thingId);

        lookIfUpdateAvailable(target);
    }

    private void lookIfUpdateAvailable(final Target target) {
        final Optional<Action> action = controllerManagement.findOldestActiveActionByTarget(target);
        if (!action.isPresent()) {
            return;
        }

        if (action.get().isCancelingOrCanceled()) {
            amqpMessageDispatcherService.targetCancelAssignmentToDistributionSet(new CancelTargetAssignmentEvent(
                    target.getOptLockRevision(), target.getTenant(), target, action.get().getId()));
            return;
        }

        final DistributionSet distributionSet = action.get().getDistributionSet();
        final List<SoftwareModule> softwareModuleList = controllerManagement
                .findSoftwareModulesByDistributionSet(distributionSet);
        amqpMessageDispatcherService.targetAssignDistributionSet(new TargetAssignDistributionSetEvent(
                target.getOptLockRevision(), target.getTenant(), target, action.get().getId(), softwareModuleList));

    }

    /**
     * Method to handle the different topics to an event.
     *
     * @param message
     *            the incoming event message.
     * @param topic
     *            the topic of the event.
     */
    private void handleIncomingEvent(final Message message, final EventTopic topic) {
        if (EventTopic.UPDATE_ACTION_STATUS.equals(topic)) {
            updateActionStatus(message);
            return;
        }
        logAndThrowMessageError(message, "Got event without appropriate topic.");
    }

    /**
     * Method to update the action status of an action through the event.
     *
     * @param actionUpdateStatus
     *            the object form the ampq message
     */
    private void updateActionStatus(final Message message) {
        final ActionUpdateStatus actionUpdateStatus = convertMessage(message, ActionUpdateStatus.class);
        final Action action = checkActionExist(message, actionUpdateStatus);

        final ActionStatus actionStatus = createActionStatus(message, actionUpdateStatus, action);
        updateLastPollTime(action.getTarget());

        switch (actionUpdateStatus.getActionStatus()) {
        case DOWNLOAD:
            actionStatus.setStatus(Status.DOWNLOAD);
            break;
        case RETRIEVED:
            actionStatus.setStatus(Status.RETRIEVED);
            break;
        case RUNNING:
            actionStatus.setStatus(Status.RUNNING);
            break;
        case CANCELED:
            actionStatus.setStatus(Status.CANCELED);
            break;
        case FINISHED:
            actionStatus.setStatus(Status.FINISHED);
            break;
        case ERROR:
            actionStatus.setStatus(Status.ERROR);
            break;
        case WARNING:
            actionStatus.setStatus(Status.WARNING);
            break;
        case CANCEL_REJECTED:
            handleCancelRejected(message, action, actionStatus);
            break;
        default:
            logAndThrowMessageError(message, "Status for action does not exisit.");
        }

        final Action addUpdateActionStatus = getUpdateActionStatus(actionStatus);

        if (!addUpdateActionStatus.isActive()) {
            lookIfUpdateAvailable(action.getTarget());
        }
    }

    private void updateLastPollTime(final Target target) {
        controllerManagement.updateTargetStatus(target.getTargetInfo(), null, System.currentTimeMillis(), null);
    }

    private ActionStatus createActionStatus(final Message message, final ActionUpdateStatus actionUpdateStatus,
            final Action action) {
        final ActionStatus actionStatus = entityFactory.generateActionStatus();
        actionUpdateStatus.getMessage().forEach(actionStatus::addMessage);

        if (ArrayUtils.isNotEmpty(message.getMessageProperties().getCorrelationId())) {
            actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "DMF message correlation-id "
                    + convertCorrelationId(message));
        }

        actionStatus.setAction(action);
        actionStatus.setOccurredAt(System.currentTimeMillis());
        return actionStatus;
    }

    private static String convertCorrelationId(final Message message) {
        return new String(message.getMessageProperties().getCorrelationId(), StandardCharsets.UTF_8);
    }

    private Action getUpdateActionStatus(final ActionStatus actionStatus) {
        if (actionStatus.getStatus().equals(Status.CANCELED)) {
            return controllerManagement.addCancelActionStatus(actionStatus);
        }
        return controllerManagement.addUpdateActionStatus(actionStatus);
    }

    private Action checkActionExist(final Message message, final ActionUpdateStatus actionUpdateStatus) {
        final Long actionId = actionUpdateStatus.getActionId();
        LOG.debug("Target notifies intermediate about action {} with status {}.", actionId,
                actionUpdateStatus.getActionStatus().name());

        if (actionId == null) {
            logAndThrowMessageError(message, "Invalid message no action id");
        }

        final Action action = controllerManagement.findActionWithDetails(actionId);

        if (action == null) {
            logAndThrowMessageError(message,
                    "Got intermediate notification about action " + actionId + " but action does not exist");
        }
        return action;
    }

    private static void handleCancelRejected(final Message message, final Action action,
            final ActionStatus actionStatus) {
        if (action.isCancelingOrCanceled()) {

            actionStatus.setStatus(Status.WARNING);

            // cancel action rejected, write warning status message and fall
            // back to running action status

        } else {
            logAndThrowMessageError(message,
                    "Cancel recjected message is not allowed, if action is on state: " + action.getStatus());
        }
    }

}
