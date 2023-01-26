/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import static org.eclipse.hawkbit.repository.RepositoryConstants.MAX_ACTION_COUNT;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED;

import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfAttributeUpdate;
import org.eclipse.hawkbit.dmf.json.model.DmfAutoConfirmation;
import org.eclipse.hawkbit.dmf.json.model.DmfCreateThing;
import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionProperties;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.security.SystemSecurityContext;
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
import org.springframework.util.StringUtils;

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

    private ControllerManagement controllerManagement;
    private ConfirmationManagement confirmationManagement;

    private final EntityFactory entityFactory;

    private final TenantConfigurationManagement tenantConfigurationManagement;

    private final SystemSecurityContext systemSecurityContext;

    private static final String THING_ID_NULL = "ThingId is null";

    private static final String EMPTY_MESSAGE_BODY = "\"\"";

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
     * @param systemSecurityContext
     *            the system Security Context
     * @param tenantConfigurationManagement
     *            the tenant configuration Management
     * @param confirmationManagement
     *            the confirmation management
     */
    public AmqpMessageHandlerService(final RabbitTemplate rabbitTemplate,
            final AmqpMessageDispatcherService amqpMessageDispatcherService,
            final ControllerManagement controllerManagement, final EntityFactory entityFactory,
            final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement, final ConfirmationManagement confirmationManagement) {
        super(rabbitTemplate);
        this.amqpMessageDispatcherService = amqpMessageDispatcherService;
        this.controllerManagement = controllerManagement;
        this.entityFactory = entityFactory;
        this.systemSecurityContext = systemSecurityContext;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.confirmationManagement = confirmationManagement;
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
     * @return a message if <null> no message is send back to sender
     */
    @RabbitListener(queues = "${hawkbit.dmf.rabbitmq.receiverQueue:dmf_receiver}", containerFactory = "listenerContainerFactory")
    public Message onMessage(final Message message,
            @Header(name = MessageHeaderKey.TYPE, required = false) final String type,
            @Header(name = MessageHeaderKey.TENANT, required = false) final String tenant) {
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
        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(tenant)) {
            throw new AmqpRejectAndDontRequeueException("Invalid message! tenant and type header are mandatory!");
        }

        final SecurityContext oldContext = SecurityContextHolder.getContext();
        try {
            final MessageType messageType = MessageType.valueOf(type);
            switch (messageType) {
            case THING_CREATED:
                setTenantSecurityContext(tenant);
                registerTarget(message, virtualHost);
                break;
            case THING_REMOVED:
                setTenantSecurityContext(tenant);
                deleteTarget(message);
                break;
            case EVENT:
                checkContentTypeJson(message);
                setTenantSecurityContext(tenant);
                handleIncomingEvent(message);
                break;
            case PING:
                if (isCorrelationIdNotEmpty(message)) {
                    amqpMessageDispatcherService.sendPingReponseToDmfReceiver(message, tenant, virtualHost);
                }
                break;
            default:
                logAndThrowMessageError(message, "No handle method was found for the given message type.");
            }
        } catch (final IllegalArgumentException ex) {
            throw new AmqpRejectAndDontRequeueException("Invalid message!", ex);
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
     * Method to create a new target or to find the target if it already exists
     * and update its poll time, status and optionally its name and attributes.
     *
     * @param message
     *            the message that contains replyTo property and optionally the
     *            name and attributes in body
     * @param virtualHost
     *            the virtual host
     */
    private void registerTarget(final Message message, final String virtualHost) {
        final String thingId = getStringHeaderKey(message, MessageHeaderKey.THING_ID, THING_ID_NULL);
        final String replyTo = message.getMessageProperties().getReplyTo();

        if (StringUtils.isEmpty(replyTo)) {
            logAndThrowMessageError(message, "No ReplyTo was set for the createThing message.");
        }

        try {
            final URI amqpUri = IpUtil.createAmqpUri(virtualHost, replyTo);
            final Target target;
            if (isOptionalMessageBodyEmpty(message)) {
                target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(thingId, amqpUri);
            } else {
                checkContentTypeJson(message);
                final DmfCreateThing thingCreateBody = convertMessage(message, DmfCreateThing.class);
                final DmfAttributeUpdate thingAttributeUpdateBody = thingCreateBody.getAttributeUpdate();

                target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(thingId, amqpUri,
                        thingCreateBody.getName());

                if (thingAttributeUpdateBody != null) {
                    controllerManagement.updateControllerAttributes(thingId, thingAttributeUpdateBody.getAttributes(),
                            getUpdateMode(thingAttributeUpdateBody));
                }
            }
            LOG.debug("Target {} reported online state.", thingId);
            sendUpdateCommandToTarget(target);
        } catch (final EntityAlreadyExistsException e) {
            throw new AmqpRejectAndDontRequeueException(
                    "Tried to register previously registered target, message will be ignored!", e);
        }
    }

    private static boolean isOptionalMessageBodyEmpty(final Message message) {
        // empty byte array message body is serialized to double-quoted string
        // by message converter and should also be considered as empty
        return isMessageBodyEmpty(message) || EMPTY_MESSAGE_BODY.equals(new String(message.getBody()));
    }

    private void sendUpdateCommandToTarget(final Target target) {
        if (isMultiAssignmentsEnabled()) {
            sendCurrentActionsAsMultiActionToTarget(target);
        } else {
            sendOldestActionToTarget(target);
        }
    }

    private void sendCurrentActionsAsMultiActionToTarget(final Target target) {
        final List<Action> actions = controllerManagement.findActiveActionsWithHighestWeight(target.getControllerId(),
                MAX_ACTION_COUNT);

        final Set<DistributionSet> distributionSets = actions.stream().map(Action::getDistributionSet)
                .collect(Collectors.toSet());
        final Map<Long, Map<SoftwareModule, List<SoftwareModuleMetadata>>> softwareModulesPerDistributionSet = distributionSets
                .stream().collect(Collectors.toMap(DistributionSet::getId, this::getSoftwareModulesWithMetadata));

        amqpMessageDispatcherService.sendMultiActionRequestToTarget(target.getTenant(), target, actions,
                action -> softwareModulesPerDistributionSet.get(action.getDistributionSet().getId()));
    }

    private void sendOldestActionToTarget(final Target target) {
        final Optional<Action> actionOptional = controllerManagement
                .findActiveActionWithHighestWeight(target.getControllerId());

        if (!actionOptional.isPresent()) {
            return;
        }

        final Action action = actionOptional.get();
        if (action.isCancelingOrCanceled()) {
            amqpMessageDispatcherService.sendCancelMessageToTarget(target.getTenant(), target.getControllerId(),
                    action.getId(), target.getAddress());
        } else {
            amqpMessageDispatcherService.sendUpdateMessageToTarget(new ActionProperties(action), action.getTarget(),
                    getSoftwareModulesWithMetadata(action.getDistributionSet()));
        }
    }

    private Map<SoftwareModule, List<SoftwareModuleMetadata>> getSoftwareModulesWithMetadata(
            final DistributionSet distributionSet) {
        final List<Long> smIds = distributionSet.getModules().stream().map(SoftwareModule::getId)
                .collect(Collectors.toList());

        final Map<Long, List<SoftwareModuleMetadata>> metadata = controllerManagement
                .findTargetVisibleMetaDataBySoftwareModuleId(smIds);

        return distributionSet.getModules().stream()
                .collect(Collectors.toMap(sm -> sm, sm -> metadata.getOrDefault(sm.getId(), Collections.emptyList())));

    }

    /**
     * Method to handle the different topics to an event.
     *
     * @param message
     *            the incoming event message.
     */
    private void handleIncomingEvent(final Message message) {
        switch (EventTopic.valueOf(getStringHeaderKey(message, MessageHeaderKey.TOPIC, "EventTopic is null"))) {
        case UPDATE_ACTION_STATUS:
            updateActionStatus(message);
            break;
        case UPDATE_ATTRIBUTES:
            updateAttributes(message);
            break;
        case UPDATE_AUTO_CONFIRM:
            setAutoConfirmationState(message);
            break;
        default:
            logAndThrowMessageError(message, "Got event without appropriate topic.");
            break;
        }

    }

    private void deleteTarget(final Message message) {
        final String thingId = getStringHeaderKey(message, MessageHeaderKey.THING_ID, THING_ID_NULL);
        controllerManagement.deleteExistingTarget(thingId);
    }

    private void updateAttributes(final Message message) {
        final DmfAttributeUpdate attributeUpdate = convertMessage(message, DmfAttributeUpdate.class);
        final String thingId = getStringHeaderKey(message, MessageHeaderKey.THING_ID, THING_ID_NULL);

        controllerManagement.updateControllerAttributes(thingId, attributeUpdate.getAttributes(),
              getUpdateMode(attributeUpdate));
    }

    private void setAutoConfirmationState(final Message message) {
        final DmfAutoConfirmation autoConfirmation = convertMessage(message, DmfAutoConfirmation.class);
        final String thingId = getStringHeaderKey(message, MessageHeaderKey.THING_ID, THING_ID_NULL);
        if (autoConfirmation.isEnabled()) {
            LOG.debug("Activate auto-confirmation for device {} using DMF. Initiator: {}. Remark: {}", thingId,
                    autoConfirmation.getInitiator(), autoConfirmation.getRemark());
            final String remark = autoConfirmation.getRemark() == null
                    ? "Activated using Device Management Federation API."
                    : autoConfirmation.getRemark();
            controllerManagement.activateAutoConfirmation(thingId, autoConfirmation.getInitiator(), remark);
        } else {
            LOG.debug("Deactivate auto-confirmation for device {} using DMF.", thingId);
            controllerManagement.deactivateAutoConfirmation(thingId);
        }
    }

    /**
     * Method to update the action status of an action through the event.
     *
     * @param message
     *            the object form the ampq message
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
            updatedAction = confirmationManagement.confirmAction(action.getId(),
                    actionUpdateStatus.getCode().orElse(null), messages);
        } else if (actionUpdateStatus.getActionStatus() == DmfActionStatus.DENIED) {
            updatedAction = confirmationManagement.denyAction(action.getId(), actionUpdateStatus.getCode().orElse(null),
                    messages);
        } else {
            final ActionStatusCreate actionStatus = entityFactory.actionStatus().create(action.getId()).status(status)
                    .messages(messages);
            actionUpdateStatus.getCode().ifPresent(code -> {
                actionStatus.code(code);
                actionStatus.message("Device reported status code: " + code);
            });
            updatedAction = (Status.CANCELED == status) ? controllerManagement.addCancelActionStatus(actionStatus)
                    : controllerManagement.addUpdateActionStatus(actionStatus);
        }

        if (shouldTargetProceed(updatedAction) || actionUpdateStatus.getActionStatus() == DmfActionStatus.CONFIRMED) {
            sendUpdateCommandToTarget(action.getTarget());
        }
    }

    private static boolean shouldTargetProceed(final Action action) {
        return !action.isActive() || (action.hasMaintenanceSchedule() && action.isMaintenanceWindowAvailable());
    }

    private static boolean isCorrelationIdNotEmpty(final Message message) {
        return StringUtils.hasLength(message.getMessageProperties().getCorrelationId());
    }

    // Exception squid:MethodCyclomaticComplexity - false positive, is a simple
    // mapping
    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    private static Status mapStatus(final Message message, final DmfActionUpdateStatus actionUpdateStatus,
            final Action action) {
        Status status = null;
        switch (actionUpdateStatus.getActionStatus()) {
        case DOWNLOAD:
            status = Status.DOWNLOAD;
            break;
        case RETRIEVED:
            status = Status.RETRIEVED;
            break;
        case RUNNING:
        case CONFIRMED:
            status = Status.RUNNING;
            break;
        case CANCELED:
            status = Status.CANCELED;
            break;
        case FINISHED:
            status = Status.FINISHED;
            break;
        case ERROR:
            status = Status.ERROR;
            break;
        case WARNING:
            status = Status.WARNING;
            break;
        case DOWNLOADED:
            status = Status.DOWNLOADED;
            break;
        case CANCEL_REJECTED:
            status = handleCancelRejectedState(message, action);
            break;
        case DENIED:
            status = Status.WAIT_FOR_CONFIRMATION;
            break;
        default:
            logAndThrowMessageError(message, "Status for action does not exisit.");
        }

        return status;
    }

    private static Status handleCancelRejectedState(final Message message, final Action action) {
        if (action.isCancelingOrCanceled()) {
            return Status.CANCEL_REJECTED;
        }
        logAndThrowMessageError(message,
                "Cancel rejected message is not allowed, if action is on state: " + action.getStatus());
        return null;
    }

    // Exception squid:S3655 - logAndThrowMessageError throws exception, i.e.
    // get will not be called
    @SuppressWarnings("squid:S3655")
    private Action checkActionExist(final Message message, final DmfActionUpdateStatus actionUpdateStatus) {
        final Long actionId = actionUpdateStatus.getActionId();

        LOG.debug("Target notifies intermediate about action {} with status {}.", actionId,
                actionUpdateStatus.getActionStatus());

        final Optional<Action> findActionWithDetails = controllerManagement.findActionWithDetails(actionId);
        if (!findActionWithDetails.isPresent()) {
            logAndThrowMessageError(message,
                    "Got intermediate notification about action " + actionId + " but action does not exist");
        }

        return findActionWithDetails.get();
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

    private boolean isMultiAssignmentsEnabled() {
        return getConfigValue(MULTI_ASSIGNMENTS_ENABLED, Boolean.class);
    }

    private <T extends Serializable> T getConfigValue(final String key, final Class<T> valueType) {
        return systemSecurityContext
                .runAsSystem(() -> tenantConfigurationManagement.getConfigurationValue(key, valueType).getValue());
    }

    // for testing
    public void setControllerManagement(final ControllerManagement controllerManagement) {
        this.controllerManagement = controllerManagement;
    }
}
