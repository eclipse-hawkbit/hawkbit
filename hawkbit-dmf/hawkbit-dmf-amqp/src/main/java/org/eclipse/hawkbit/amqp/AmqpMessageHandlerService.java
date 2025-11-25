/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import static org.eclipse.hawkbit.repository.RepositoryConstants.MAX_ACTION_COUNT;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfAttributeUpdate;
import org.eclipse.hawkbit.dmf.json.model.DmfAutoConfirmation;
import org.eclipse.hawkbit.dmf.json.model.DmfCreateThing;
import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate.ActionStatusCreateBuilder;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionProperties;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.util.IpUtil;
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
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link AmqpMessageHandlerService} handles all incoming target interaction AMQP messages (e.g. create target, check for updates etc.) for the
 * queue which is configured for the property hawkbit.dmf.rabbitmq.receiverQueue.
 */
@Slf4j
public class AmqpMessageHandlerService extends BaseAmqpService {

    private static final String THING_ID_NULL = "ThingId is null";
    private static final String EMPTY_MESSAGE_BODY = "\"\"";

    private final AmqpMessageDispatcherService amqpMessageDispatcherService;
    private final ConfirmationManagement confirmationManagement;
    private ControllerManagement controllerManagement;

    public AmqpMessageHandlerService(
            final RabbitTemplate rabbitTemplate,
            final AmqpMessageDispatcherService amqpMessageDispatcherService,
            final ControllerManagement controllerManagement,
            final ConfirmationManagement confirmationManagement) {
        super(rabbitTemplate);
        this.amqpMessageDispatcherService = amqpMessageDispatcherService;
        this.controllerManagement = controllerManagement;
        this.confirmationManagement = confirmationManagement;
    }

    /**
     * Method to handle all incoming DMF amqp messages.
     *
     * @param message incoming message
     * @param type the message type
     * @param tenant the contentType of the message
     * @return a message if <null> no message is send back to sender
     */
    @RabbitListener(queues = "${hawkbit.dmf.rabbitmq.receiverQueue:dmf_receiver}", containerFactory = "listenerContainerFactory")
    public Message onMessage(
            final Message message,
            @Header(name = MessageHeaderKey.TYPE, required = false) final String type,
            @Header(name = MessageHeaderKey.TENANT, required = false) final String tenant) {
        return onMessage(message, type, tenant, getRabbitTemplate().getConnectionFactory().getVirtualHost());
    }

    /**
     * Executed if a amqp message arrives.
     *
     * @param message the message
     * @param type the type
     * @param tenant the tenant
     * @param virtualHost the virtual host
     * @return the rpc message back to supplier.
     */
    public Message onMessage(final Message message, final String type, final String tenant, final String virtualHost) {
        if (ObjectUtils.isEmpty(type) || ObjectUtils.isEmpty(tenant)) {
            throw new AmqpRejectAndDontRequeueException("Invalid message! tenant and type header are mandatory!");
        }

        final SecurityContext oldContext = SecurityContextHolder.getContext();
        try {
            final MessageType messageType = MessageType.valueOf(type);
            switch (messageType) {
                case THING_CREATED: {
                    setTenantSecurityContext(tenant);
                    registerTarget(message, virtualHost);
                    break;
                }
                case THING_REMOVED: {
                    setTenantSecurityContext(tenant);
                    deleteTarget(message);
                    break;
                }
                case EVENT: {
                    checkContentTypeJson(message);
                    setTenantSecurityContext(tenant);
                    handleIncomingEvent(message);
                    break;
                }
                case PING: {
                    if (isCorrelationIdNotEmpty(message)) {
                        amqpMessageDispatcherService.sendPingResponseToDmfReceiver(message, tenant, virtualHost);
                    }
                    break;
                }
                default: {
                    logAndThrowMessageError(message, "No handle method was found for the given message type.");
                }
            }
        } catch (AssignmentQuotaExceededException ex) {
            throw new AmqpRejectAndDontRequeueException("Could not handle message due to quota violation!", ex);
        } catch (final IllegalArgumentException ex) {
            throw new AmqpRejectAndDontRequeueException("Invalid message!", ex);
        } finally {
            SecurityContextHolder.setContext(oldContext);
        }
        return null;
    }

    // for testing
    public void setControllerManagement(final ControllerManagement controllerManagement) {
        this.controllerManagement = controllerManagement;
    }

    private static void setSecurityContext(final Authentication authentication) {
        final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
        securityContextImpl.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContextImpl);
    }

    private static void setTenantSecurityContext(final String tenantId) {
        final AnonymousAuthenticationToken authenticationToken = new AnonymousAuthenticationToken(
                UUID.randomUUID().toString(), "AMQP-Controller",
                List.of(new SimpleGrantedAuthority(SpRole.CONTROLLER_ROLE_ANONYMOUS)));
        authenticationToken.setDetails(new TenantAwareAuthenticationDetails(tenantId, true));
        setSecurityContext(authenticationToken);
    }

    private static boolean isOptionalMessageBodyEmpty(final Message message) {
        // empty byte array message body is serialized to double-quoted string
        // by message converter and should also be considered as empty
        return isMessageBodyEmpty(message) || EMPTY_MESSAGE_BODY.equals(new String(message.getBody()));
    }

    private static boolean shouldTargetProceed(final Action action) {
        return !action.isActive() || (action.hasMaintenanceSchedule() && action.isMaintenanceWindowAvailable());
    }

    private static boolean isCorrelationIdNotEmpty(final Message message) {
        return StringUtils.hasLength(message.getMessageProperties().getCorrelationId());
    }

    @SuppressWarnings("java:S2637") // java:S2637 - logAndThrowMessageError throws exception, i.e. doesn't return null
    private static @NotNull Status mapStatus(final Message message, final DmfActionUpdateStatus actionUpdateStatus, final Action action) {
        Status status = null;
        switch (actionUpdateStatus.getActionStatus()) {
            case DOWNLOAD: {
                status = Status.DOWNLOAD;
                break;
            }
            case RETRIEVED: {
                status = Status.RETRIEVED;
                break;
            }
            case RUNNING, CONFIRMED: {
                status = Status.RUNNING;
                break;
            }
            case CANCELED: {
                status = Status.CANCELED;
                break;
            }
            case FINISHED: {
                status = Status.FINISHED;
                break;
            }
            case ERROR: {
                status = Status.ERROR;
                break;
            }
            case WARNING: {
                status = Status.WARNING;
                break;
            }
            case DOWNLOADED: {
                status = Status.DOWNLOADED;
                break;
            }
            case CANCEL_REJECTED: {
                status = handleCancelRejectedState(message, action);
                break;
            }
            case DENIED: {
                status = Status.WAIT_FOR_CONFIRMATION;
                break;
            }
            default: {
                logAndThrowMessageError(message, "Status for action does not exist.");
            }
        }

        return status;
    }

    private static Status handleCancelRejectedState(final Message message, final Action action) {
        if (action.isCancelingOrCanceled()) {
            return Status.CANCEL_REJECTED;
        }
        logAndThrowMessageError(
                message,
                "Cancel rejected message is not allowed, if action is on state: " + action.getStatus());
        return null;
    }

    /**
     * Retrieve the update mode from the given update message.
     */
    private static UpdateMode getUpdateMode(final DmfAttributeUpdate update) {
        final DmfUpdateMode mode = update.getMode();
        if (mode != null) {
            return UpdateMode.valueOf(mode.name());
        }
        return null;
    }

    /**
     * Method to create a new target or to find the target if it already exists
     * and update its poll time, status and optionally its name and attributes.
     *
     * @param message the message that contains replyTo property and optionally the name and attributes in body
     * @param virtualHost the virtual host
     */
    private void registerTarget(final Message message, final String virtualHost) {
        final String thingId = getStringHeaderKey(message, MessageHeaderKey.THING_ID, THING_ID_NULL);
        final String replyTo = message.getMessageProperties().getReplyTo();

        if (ObjectUtils.isEmpty(replyTo)) {
            logAndThrowMessageError(message, "No ReplyTo was set for the createThing message.");
        }

        try {
            final URI amqpUri = IpUtil.createAmqpUri(virtualHost, replyTo);
            final Target target;
            if (isOptionalMessageBodyEmpty(message)) {
                log.debug("Received \"THING_CREATED\" AMQP message for thing \"{}\" without body.", thingId);
                target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(thingId, amqpUri);
            } else {
                checkContentTypeJson(message);
                final DmfCreateThing thingCreateBody = convertMessage(message, DmfCreateThing.class);
                final DmfAttributeUpdate thingAttributeUpdateBody = thingCreateBody.getAttributeUpdate();

                log.debug(
                        "Received \"THING_CREATED\" AMQP message for thing \"{}\" with target name \"{}\" and type \"{}\".",
                        thingId, thingCreateBody.getName(), thingCreateBody.getType());

                target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(
                        thingId, amqpUri, thingCreateBody.getName(), thingCreateBody.getType());

                if (thingAttributeUpdateBody != null) {
                    controllerManagement.updateControllerAttributes(
                            thingId, thingAttributeUpdateBody.getAttributes(), getUpdateMode(thingAttributeUpdateBody));
                }
            }
            log.debug("Target {} reported online state.", thingId);
            sendUpdateCommandToTarget(target);
        } catch (final EntityAlreadyExistsException e) {
            throw new AmqpRejectAndDontRequeueException("Tried to register previously registered target, message will be ignored!", e);
        }
    }

    private void sendUpdateCommandToTarget(final Target target) {
        if (isMultiAssignmentsEnabled()) {
            sendCurrentActionsAsMultiActionToTarget(target);
        } else {
            sendOldestActionToTarget(target);
        }
    }

    private void sendCurrentActionsAsMultiActionToTarget(final Target target) {
        final List<Action> actions = controllerManagement.findActiveActionsWithHighestWeight(target.getControllerId(), MAX_ACTION_COUNT);

        // gets all software modules for all action at once
        final Set<Long> allSmIds = actions.stream()
                .map(Action::getDistributionSet)
                .flatMap(ds -> ds.getModules().stream())
                .map(SoftwareModule::getId)
                .collect(Collectors.toSet());
        final Map<Long, Map<String, String>> getSoftwareModuleMetadata =
                allSmIds.isEmpty() ? Collections.emptyMap() : controllerManagement.findTargetVisibleMetaDataBySoftwareModuleId(allSmIds);

        amqpMessageDispatcherService.sendMultiActionRequestToTarget(target, actions, module -> getSoftwareModuleMetadata.get(module.getId()));
    }

    private void sendOldestActionToTarget(final Target target) {
        final Optional<Action> actionOptional = controllerManagement.findActiveActionWithHighestWeight(target.getControllerId());
        if (actionOptional.isEmpty()) {
            return;
        }

        final Action action = actionOptional.get();
        if (action.isCancelingOrCanceled()) {
            amqpMessageDispatcherService.sendCancelMessageToTarget(
                    target.getTenant(), target.getControllerId(), action.getId(), IpUtil.addressToUri(target.getAddress()));
        } else {
            amqpMessageDispatcherService.sendUpdateMessageToTarget(
                    new ActionProperties(action), action.getTarget(), getSoftwareModulesWithMetadata(action.getDistributionSet()));
        }
    }

    private Map<SoftwareModule, Map<String, String>> getSoftwareModulesWithMetadata(final DistributionSet distributionSet) {
        final List<Long> smIds = distributionSet.getModules().stream().map(SoftwareModule::getId).toList();
        final Map<Long, Map<String, String>> metadata = controllerManagement.findTargetVisibleMetaDataBySoftwareModuleId(smIds);
        return distributionSet.getModules().stream().collect(Collectors.toMap(
                Function.identity(), sm -> metadata.getOrDefault(sm.getId(), Collections.emptyMap())));

    }

    /**
     * Method to handle the different topics to an event.
     *
     * @param message the incoming event message.
     */
    private void handleIncomingEvent(final Message message) {
        switch (EventTopic.valueOf(getStringHeaderKey(message, MessageHeaderKey.TOPIC, "EventTopic is null"))) {
            case UPDATE_ACTION_STATUS: {
                updateActionStatus(message);
                break;
            }
            case UPDATE_ATTRIBUTES: {
                updateAttributes(message);
                break;
            }
            case UPDATE_AUTO_CONFIRM: {
                setAutoConfirmationState(message);
                break;
            }
            default: {
                logAndThrowMessageError(message, "Got event without appropriate topic.");
                break;
            }
        }
    }

    @AuditLog(entity = "DMF", type = AuditLog.Type.DELETE, description = "Delete Target", logResponse = true)
    private void deleteTarget(final Message message) {
        final String thingId = getStringHeaderKey(message, MessageHeaderKey.THING_ID, THING_ID_NULL);
        controllerManagement.deleteExistingTarget(thingId);
    }

    private void updateAttributes(final Message message) {
        final DmfAttributeUpdate attributeUpdate = convertMessage(message, DmfAttributeUpdate.class);
        final String thingId = getStringHeaderKey(message, MessageHeaderKey.THING_ID, THING_ID_NULL);

        controllerManagement.updateControllerAttributes(thingId, attributeUpdate.getAttributes(), getUpdateMode(attributeUpdate));
    }

    private void setAutoConfirmationState(final Message message) {
        final DmfAutoConfirmation autoConfirmation = convertMessage(message, DmfAutoConfirmation.class);
        final String thingId = getStringHeaderKey(message, MessageHeaderKey.THING_ID, THING_ID_NULL);
        if (autoConfirmation.isEnabled()) {
            log.debug("Activate auto-confirmation for device {} using DMF. Initiator: {}. Remark: {}", thingId,
                    autoConfirmation.getInitiator(), autoConfirmation.getRemark());
            final String remark = autoConfirmation.getRemark() == null
                    ? "Activated using Device Management Federation API."
                    : autoConfirmation.getRemark();
            controllerManagement.activateAutoConfirmation(thingId, autoConfirmation.getInitiator(), remark);
        } else {
            log.debug("Deactivate auto-confirmation for device {} using DMF.", thingId);
            controllerManagement.deactivateAutoConfirmation(thingId);
        }
    }

    /**
     * Method to update the action status of an action through the event.
     *
     * @param message the object form the ampq message
     */
    private void updateActionStatus(final Message message) {
        final DmfActionUpdateStatus actionUpdateStatus = convertMessage(message, DmfActionUpdateStatus.class);
        final Action action = checkActionExist(message, actionUpdateStatus);

        final List<String> messages = actionUpdateStatus.getMessage();

        if (isCorrelationIdNotEmpty(message)) {
            messages.add(RepositoryConstants.SERVER_MESSAGE_PREFIX + "DMF message correlation-id "
                    + message.getMessageProperties().getCorrelationId());
        }

        final Status status = mapStatus(message, actionUpdateStatus, action);

        final Action updatedAction;
        if (actionUpdateStatus.getActionStatus() == DmfActionStatus.CONFIRMED) {
            updatedAction = confirmationManagement.confirmAction(action.getId(), actionUpdateStatus.getCode(), messages);
        } else if (actionUpdateStatus.getActionStatus() == DmfActionStatus.DENIED) {
            updatedAction = confirmationManagement.denyAction(action.getId(), actionUpdateStatus.getCode(), messages);
        } else {
            final ActionStatusCreateBuilder actionStatus = ActionStatusCreate.builder().actionId(action.getId())
                    .status(status);
            Optional.ofNullable(actionUpdateStatus.getCode()).ifPresentOrElse(
                    code -> {
                        actionStatus.code(code);
                        final List<String> withCodeReportedMessage = new ArrayList<>(messages);
                        withCodeReportedMessage.add("Device reported status code: " + code);
                        actionStatus.messages(withCodeReportedMessage);
                    },
                    () -> actionStatus.messages(messages));
            updatedAction = Status.CANCELED == status || Status.CANCEL_REJECTED == status
                    ? controllerManagement.addCancelActionStatus(actionStatus.build())
                    : controllerManagement.addUpdateActionStatus(actionStatus.build());
        }

        if (shouldTargetProceed(updatedAction) || actionUpdateStatus.getActionStatus() == DmfActionStatus.CONFIRMED) {
            sendUpdateCommandToTarget(action.getTarget());
        }
    }

    // Exception squid:S3655 - logAndThrowMessageError throws exception, i.e. get will not be called
    @SuppressWarnings("squid:S3655")
    private Action checkActionExist(final Message message, final DmfActionUpdateStatus actionUpdateStatus) {
        final Long actionId = actionUpdateStatus.getActionId();

        log.debug("Target notifies intermediate about action {} with status {}.", actionId,
                actionUpdateStatus.getActionStatus());

        final Optional<Action> findActionWithDetails = controllerManagement.findActionWithDetails(actionId);
        if (findActionWithDetails.isEmpty()) {
            logAndThrowMessageError(message, "Got intermediate notification about action " + actionId + " but action does not exist");
        }

        return findActionWithDetails.get();
    }

    private boolean isMultiAssignmentsEnabled() {
        return TenantConfigHelper.getAsSystem(MULTI_ASSIGNMENTS_ENABLED, Boolean.class);
    }
}