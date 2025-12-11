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

import static org.eclipse.hawkbit.context.AccessContext.asSystem;
import static org.eclipse.hawkbit.repository.RepositoryConstants.MAX_ACTION_COUNT;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.BATCH_ASSIGNMENTS_ENABLED;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrl;
import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrlResolver;
import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrlResolver.DownloadDescriptor;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfArtifact;
import org.eclipse.hawkbit.dmf.json.model.DmfArtifactHash;
import org.eclipse.hawkbit.dmf.json.model.DmfBatchDownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfConfirmRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfMetadata;
import org.eclipse.hawkbit.dmf.json.model.DmfMultiActionRequest;
import org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule;
import org.eclipse.hawkbit.dmf.json.model.DmfTarget;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.MultiActionAssignEvent;
import org.eclipse.hawkbit.repository.event.remote.MultiActionCancelEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.service.CancelTargetAssignmentServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.MultiActionAssignServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.MultiActionCancelServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetAssignDistributionSetServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetAttributesRequestedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetDeletedServiceEvent;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionProperties;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.utils.IpUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;

/**
 * {@link AmqpMessageDispatcherService} create all outgoing AMQP messages and delegate the messages to a {@link AmqpMessageSenderService}.
 * <p/>
 * Additionally, the dispatcher listener/subscribe for some target events e.g. assignment.
 */
@Slf4j
public class AmqpMessageDispatcherService extends BaseAmqpService {

    private static final int MAX_PROCESSING_SIZE = 1000;

    private final ArtifactUrlResolver artifactUrlHandler;
    private final AmqpMessageSenderService amqpSenderService;
    private final SystemManagement systemManagement;
    private final TargetManagement<? extends Target> targetManagement;
    private final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement;
    private final DistributionSetManagement<? extends DistributionSet> distributionSetManagement;
    private final DeploymentManagement deploymentManagement;
    private final RepositoryProperties repositoryProperties;

    @SuppressWarnings("java:S107")
    protected AmqpMessageDispatcherService(
            final RabbitTemplate rabbitTemplate,
            final AmqpMessageSenderService amqpSenderService, final ArtifactUrlResolver artifactUrlHandler,
            final SystemManagement systemManagement,
            final TargetManagement<? extends Target> targetManagement,
            final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement,
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement,
            final DeploymentManagement deploymentManagement,
            final RepositoryProperties repositoryProperties) {
        super(rabbitTemplate);
        this.artifactUrlHandler = artifactUrlHandler;
        this.amqpSenderService = amqpSenderService;
        this.systemManagement = systemManagement;
        this.targetManagement = targetManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.deploymentManagement = deploymentManagement;
        this.repositoryProperties = repositoryProperties;
    }

    public boolean isBatchAssignmentsEnabled() {
        return TenantConfigHelper.getAsSystem(BATCH_ASSIGNMENTS_ENABLED, Boolean.class);
    }

    /**
     * Method to send a message to a RabbitMQ Exchange after the Distribution set has been assign to a Target.
     *
     * @param targetAssignDistributionSetServiceEvent event to be processed
     */
    @EventListener(classes = TargetAssignDistributionSetServiceEvent.class)
    protected void targetAssignDistributionSet(final TargetAssignDistributionSetServiceEvent targetAssignDistributionSetServiceEvent) {
        final TargetAssignDistributionSetEvent assignedEvent = targetAssignDistributionSetServiceEvent.getRemoteEvent();
        final List<Target> filteredTargetList = getTargetsWithoutPendingCancellations(assignedEvent.getActions().keySet());

        if (!filteredTargetList.isEmpty()) {
            log.debug("targetAssignDistributionSet retrieved. I will forward it to DMF broker.");
            sendUpdateMessageToTargets(assignedEvent.getDistributionSetId(), assignedEvent.getActions(), filteredTargetList);
        }
    }

    /**
     * Listener for Multi-Action events.
     *
     * @param multiActionAssignServiceEvent the Multi-Action event to be processed
     */
    @EventListener(classes = MultiActionAssignServiceEvent.class)
    protected void onMultiActionAssign(final MultiActionAssignServiceEvent multiActionAssignServiceEvent) {
        final MultiActionAssignEvent multiActionAssignEvent = multiActionAssignServiceEvent.getRemoteEvent();
        log.debug("MultiActionAssignEvent received for {}", multiActionAssignEvent.getControllerIds());
        sendMultiActionRequestMessages(multiActionAssignEvent.getControllerIds());
    }

    /**
     * Listener for Multi-Action events.
     *
     * @param multiActionCancelServiceEvent the Multi-Action event to be processed
     */
    @EventListener(classes = MultiActionCancelServiceEvent.class)
    protected void onMultiActionCancel(final MultiActionCancelServiceEvent multiActionCancelServiceEvent) {
        final MultiActionCancelEvent multiActionCancelEvent = multiActionCancelServiceEvent.getRemoteEvent();
        log.debug("MultiActionCancelEvent received for {}", multiActionCancelEvent.getControllerIds());
        sendMultiActionRequestMessages(multiActionCancelEvent.getControllerIds());
    }

    protected void sendUpdateMessageToTarget(
            final ActionProperties actionsProps, final Target target,
            final Map<SoftwareModule, Map<String, String>> softwareModules) {
        final Map<String, ActionProperties> actionProp = new HashMap<>();
        actionProp.put(target.getControllerId(), actionsProps);
        sendUpdateMessageToTargets(actionProp, Collections.singletonList(target), softwareModules);
    }

    protected DmfDownloadAndUpdateRequest createDownloadAndUpdateRequest(
            final Target target, final Long actionId, final Map<SoftwareModule, Map<String, String>> softwareModules) {
        return new DmfDownloadAndUpdateRequest(
                actionId, asSystem(target::getSecurityToken), convertToAmqpSoftwareModules(target, softwareModules));
    }

    /**
     * Method to send a message to a RabbitMQ Exchange after the assignment of
     * the Distribution set to a Target has been canceled.
     *
     * @param cancelTargetAssignmentServiceEvent that is to be converted to a DMF message
     */
    @EventListener(classes = CancelTargetAssignmentServiceEvent.class)
    protected void targetCancelAssignmentToDistributionSet(final CancelTargetAssignmentServiceEvent cancelTargetAssignmentServiceEvent) {
        final CancelTargetAssignmentEvent cancelEvent = cancelTargetAssignmentServiceEvent.getRemoteEvent();
        final List<Target> eventTargets = partitionedParallelExecution(cancelEvent.getActions().keySet(), targetManagement::findByControllerId);
        eventTargets.forEach(target ->
                cancelEvent.getActionPropertiesForController(target.getControllerId())
                        .map(ActionProperties::getId)
                        .ifPresent(actionId -> sendCancelMessageToTarget
                                (cancelEvent.getTenant(), target.getControllerId(), actionId, IpUtil.addressToUri(target.getAddress()))));
    }

    /**
     * Method to send a message to a RabbitMQ Exchange after a Target was deleted.
     *
     * @param serviceTargetDeleteEvent the TargetDeletedEvent which holds the necessary data for sending a target delete message.
     */
    @EventListener(classes = TargetDeletedServiceEvent.class)
    protected void targetDelete(final TargetDeletedServiceEvent serviceTargetDeleteEvent) {
        final TargetDeletedEvent deleteEvent = serviceTargetDeleteEvent.getRemoteEvent();
        sendDeleteMessage(deleteEvent.getTenant(), deleteEvent.getControllerId(), deleteEvent.getTargetAddress());
    }

    @EventListener(classes = TargetAttributesRequestedServiceEvent.class)
    protected void targetTriggerUpdateAttributes(final TargetAttributesRequestedServiceEvent serviceTargetUpdateAttributesEvent) {
        final TargetAttributesRequestedEvent updateAttributesEvent = serviceTargetUpdateAttributesEvent.getRemoteEvent();
        sendUpdateAttributesMessageToTarget(
                updateAttributesEvent.getTenant(), updateAttributesEvent.getControllerId(),
                updateAttributesEvent.getTargetAddress());
    }

    protected void sendPingResponseToDmfReceiver(final Message ping, final String tenant, final String virtualHost) {
        final Message message = MessageBuilder
                .withBody(String.valueOf(System.currentTimeMillis()).getBytes())
                .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
                .setCorrelationId(ping.getMessageProperties().getCorrelationId())
                .setHeader(MessageHeaderKey.TYPE, MessageType.PING_RESPONSE)
                .setHeader(MessageHeaderKey.TENANT, tenant)
                .build();

        amqpSenderService.sendMessage(message,
                IpUtil.createAmqpUri(virtualHost, ping.getMessageProperties().getReplyTo()));
    }

    protected void sendCancelMessageToTarget(final String tenant, final String controllerId, final Long actionId, final URI address) {
        if (!IpUtil.isAmqpUri(address)) {
            return;
        }

        final DmfActionRequest actionRequest = new DmfActionRequest(actionId);
        final Message message = getMessageConverter().toMessage(
                actionRequest,
                createConnectorMessagePropertiesEvent(tenant, controllerId, EventTopic.CANCEL_DOWNLOAD));

        amqpSenderService.sendMessage(message, address);
    }

    protected DmfConfirmRequest createConfirmRequest(
            final Target target, final Long actionId, final Map<SoftwareModule, Map<String, String>> softwareModules) {
        return new DmfConfirmRequest(actionId, asSystem(target::getSecurityToken), convertToAmqpSoftwareModules(target, softwareModules));
    }

    void sendMultiActionRequestToTarget(
            final Target target, final List<Action> actions,
            final Function<SoftwareModule, Map<String, String>> getSoftwareModuleMetaData) {
        final URI targetAddress = IpUtil.addressToUri(target.getAddress());
        if (!IpUtil.isAmqpUri(targetAddress) || CollectionUtils.isEmpty(actions)) {
            return;
        }

        final DmfMultiActionRequest multiActionRequest = new DmfMultiActionRequest(
                actions.stream()
                        .map(action -> {
                            final DmfActionRequest actionRequest = createDmfActionRequest(
                                    target, action,
                                    action.getDistributionSet().getModules().stream()
                                            .collect(Collectors.toMap(Function.identity(), module -> {
                                                final Map<String, String> softwareModuleMetadata = getSoftwareModuleMetaData.apply(module);
                                                return softwareModuleMetadata == null ? Collections.emptyMap() : softwareModuleMetadata;
                                            })));
                            final int weight = getWeightConsideringDefault(action);
                            return new DmfMultiActionRequest.DmfMultiActionElement(getEventTypeForAction(action), actionRequest, weight);
                        })
                        .toList());

        final Message message = getMessageConverter().toMessage(
                multiActionRequest,
                createConnectorMessagePropertiesEvent(target.getTenant(), target.getControllerId(), EventTopic.MULTI_ACTION));
        amqpSenderService.sendMessage(message, targetAddress);
    }

    private int getWeightConsideringDefault(final Action action) {
        return action.getWeight().orElse(repositoryProperties.getActionWeightIfAbsent());
    }

    /**
     * Method to get the type of event depending on whether the action is a DOWNLOAD_ONLY action or if it has a valid maintenance window
     * available or not based on defined maintenance schedule. In case of no maintenance schedule or if there is a valid window available,
     * the topic {@link EventTopic#DOWNLOAD_AND_INSTALL} is returned else {@link EventTopic#DOWNLOAD} is returned.
     *
     * @param action current action properties.
     * @return {@link EventTopic} to use for message.
     */
    private static EventTopic getEventTypeForTarget(final ActionProperties action) {
        if (action.isWaitingConfirmation()) {
            return EventTopic.CONFIRM;
        }
        return (Action.ActionType.DOWNLOAD_ONLY == action.getActionType() || !action.isMaintenanceWindowAvailable())
                ? EventTopic.DOWNLOAD
                : EventTopic.DOWNLOAD_AND_INSTALL;
    }

    /**
     * Determines the {@link EventTopic} for the given {@link Action}, depending on its action type.
     *
     * @param action to obtain the corresponding {@link EventTopic} for
     * @return the {@link EventTopic} for this action
     */
    private static EventTopic getEventTypeForAction(final Action action) {
        if (action.isCancelingOrCanceled()) {
            return EventTopic.CANCEL_DOWNLOAD;
        }
        return getEventTypeForTarget(new ActionProperties(action));
    }

    private static <T, R> List<R> partitionedParallelExecution(
            final Collection<T> controllerIds, final Function<Collection<T>, List<R>> loadingFunction) {
        // Ensure not exceeding the max value of MAX_PROCESSING_SIZE
        if (controllerIds.size() > MAX_PROCESSING_SIZE) {
            // Split the provided collection
            final Iterable<List<T>> partitions = ListUtils.partition(IterableUtils.toList(controllerIds), MAX_PROCESSING_SIZE);
            // Preserve the security context because it gets lost when executing
            // loading calls in new threads
            final SecurityContext context = SecurityContextHolder.getContext();
            // Handling remote request in parallel streams
            return StreamSupport.stream(partitions.spliterator(), true) //
                    .flatMap(partition -> withSecurityContext(() -> loadingFunction.apply(partition), context).stream())
                    .toList();
        }
        return loadingFunction.apply(controllerIds);
    }

    private static <T> T withSecurityContext(final Supplier<T> callable, final SecurityContext securityContext) {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(securityContext);
            return callable.get();
        } finally {
            SecurityContextHolder.setContext(oldContext);
        }
    }

    private static MessageProperties createConnectorMessagePropertiesEvent(
            final String tenant, final String controllerId, final EventTopic topic) {
        final MessageProperties messageProperties = createConnectorMessageProperties(tenant, controllerId);
        messageProperties.setHeader(MessageHeaderKey.TOPIC, topic);
        messageProperties.setHeader(MessageHeaderKey.TYPE, MessageType.EVENT);
        return messageProperties;
    }

    private static MessageProperties createConnectorMessagePropertiesDeleteThing(
            final String tenant, final String controllerId) {
        final MessageProperties messageProperties = createConnectorMessageProperties(tenant, controllerId);
        messageProperties.setHeader(MessageHeaderKey.TYPE, MessageType.THING_DELETED);
        return messageProperties;
    }

    private static MessageProperties createConnectorMessageProperties(final String tenant, final String controllerId) {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setHeader(MessageHeaderKey.CONTENT_TYPE, MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, controllerId);
        messageProperties.setHeader(MessageHeaderKey.TENANT, tenant);
        return messageProperties;
    }

    private static MessageProperties createMessagePropertiesBatch(final String tenant, final EventTopic topic) {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setHeader(MessageHeaderKey.CONTENT_TYPE, MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setHeader(MessageHeaderKey.TENANT, tenant);

        messageProperties.setHeader(MessageHeaderKey.TOPIC, topic);
        messageProperties.setHeader(MessageHeaderKey.TYPE, MessageType.EVENT);
        return messageProperties;
    }

    private static EventTopic getBatchEventTopicForAction(final ActionProperties action) {
        return (Action.ActionType.DOWNLOAD_ONLY == action.getActionType() || !action.isMaintenanceWindowAvailable())
                ? EventTopic.BATCH_DOWNLOAD
                : EventTopic.BATCH_DOWNLOAD_AND_INSTALL;
    }

    private List<Target> getTargetsWithoutPendingCancellations(final Set<String> controllerIds) {
        return partitionedParallelExecution(controllerIds, partition ->
                targetManagement.findByControllerId(partition).stream()
                        .filter(target -> {
                            if (hasPendingCancellations(target.getId())) {
                                log.debug("Target {} has pending cancellations. Will not send update message to it.",
                                        target.getControllerId());
                                return false;
                            }
                            return true;
                        }).toList());
    }

    private void sendUpdateMessageToTargets(
            final Long dsId, final Map<String, ActionProperties> actionsPropsByTargetId, final List<Target> targets) {
        distributionSetManagement.find(dsId).ifPresent(ds -> {
            final Map<SoftwareModule, Map<String, String>> softwareModules = getSoftwareModulesWithMetadata(ds);
            sendUpdateMessageToTargets(actionsPropsByTargetId, targets, softwareModules);
        });
    }

    private void sendUpdateMessageToTargets(
            final Map<String, ActionProperties> actionsPropsByTargetId,
            final List<Target> targets, final Map<SoftwareModule, Map<String, String>> softwareModules) {
        if (!targets.isEmpty() && isBatchAssignmentsEnabled()) {
            sendBatchUpdateMessage(actionsPropsByTargetId, targets, softwareModules);
        } else {
            targets.forEach(target -> {
                final ActionProperties actionProp = actionsPropsByTargetId.get(target.getControllerId());
                sendSingleUpdateMessage(actionProp, target, softwareModules);
            });
        }
    }

    private void sendMultiActionRequestMessages(final List<String> controllerIds) {
        final Map<String, List<Action>> controllerIdToActions = controllerIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        controllerId -> deploymentManagement.findActiveActionsWithHighestWeight(controllerId, MAX_ACTION_COUNT)));

        // gets all software modules for all action at once
        final Set<Long> allSmIds = controllerIdToActions.values().stream()
                .flatMap(actions -> actions.stream()
                        .map(Action::getDistributionSet)
                        .flatMap(ds -> ds.getModules().stream())
                        .map(SoftwareModule::getId))
                .collect(Collectors.toSet());
        final Map<Long, ? extends Map<String, String>> getSoftwareModuleMetadata =
                allSmIds.isEmpty()
                        ? Collections.emptyMap()
                        : softwareModuleManagement.findMetaDataBySoftwareModuleIdsAndTargetVisible(allSmIds);

        targetManagement.findByControllerId(controllerIds).forEach(target ->
                sendMultiActionRequestToTarget(
                        target, controllerIdToActions.get(target.getControllerId()), module -> getSoftwareModuleMetadata.get(module.getId())));
    }

    private DmfActionRequest createDmfActionRequest(
            final Target target, final Action action,
            final Map<SoftwareModule, Map<String, String>> softwareModules) {
        if (action.isCancelingOrCanceled()) {
            return new DmfActionRequest(action.getId());
        } else if (action.isWaitingConfirmation()) {
            return createConfirmRequest(target, action.getId(), softwareModules);
        }
        return createDownloadAndUpdateRequest(target, action.getId(), softwareModules);
    }

    private void sendSingleUpdateMessage(
            final ActionProperties action, final Target target, final Map<SoftwareModule, Map<String, String>> modules) {
        final String tenant = action.getTenant();

        final URI targetAddress = IpUtil.addressToUri(target.getAddress());
        if (!IpUtil.isAmqpUri(targetAddress)) {
            return;
        }

        DmfActionRequest request;
        if (action.isWaitingConfirmation()) {
            // For the moment the confirmation request is the same as download and update request.
            // It can be modified not to expose all the software modules in the future.
            request = createConfirmRequest(target, action.getId(), modules);
        } else {
            request = createDownloadAndUpdateRequest(target, action.getId(), modules);
        }

        final Message message = getMessageConverter().toMessage(
                request,
                createConnectorMessagePropertiesEvent(tenant, target.getControllerId(), getEventTypeForTarget(action)));
        amqpSenderService.sendMessage(message, targetAddress);
    }

    private void sendDeleteMessage(final String tenant, final String controllerId, final String targetAddress) {
        if (hasInvalidAddress(targetAddress)) {
            return;
        }

        final Message message = new Message("".getBytes(), createConnectorMessagePropertiesDeleteThing(tenant, controllerId));
        amqpSenderService.sendMessage(message, URI.create(targetAddress));
    }

    private boolean hasInvalidAddress(final String targetAddress) {
        return targetAddress == null || !IpUtil.isAmqpUri(URI.create(targetAddress));
    }

    private boolean hasPendingCancellations(final Long targetId) {
        return deploymentManagement.hasPendingCancellations(targetId);
    }

    private void sendUpdateAttributesMessageToTarget(final String tenant, final String controllerId, final String targetAddress) {
        if (hasInvalidAddress(targetAddress)) {
            return;
        }

        final Message message = new Message(
                "".getBytes(),
                createConnectorMessagePropertiesEvent(tenant, controllerId, EventTopic.REQUEST_ATTRIBUTES_UPDATE));

        amqpSenderService.sendMessage(message, URI.create(targetAddress));
    }

    private List<DmfSoftwareModule> convertToAmqpSoftwareModules(
            final Target target, final Map<SoftwareModule, Map<String, String>> softwareModules) {
        return Optional.ofNullable(softwareModules)
                .map(Map::entrySet)
                .map(Set::stream)
                .map(stream -> stream.map(entry -> convertToAmqpSoftwareModule(target, entry)).toList())
                .orElse(null);
    }

    private DmfSoftwareModule convertToAmqpSoftwareModule(final Target target, final Entry<SoftwareModule, Map<String, String>> entry) {
        return new DmfSoftwareModule(
                entry.getKey().getId(),
                entry.getKey().getType().getKey(),
                entry.getKey().getVersion(),
                entry.getKey().isEncrypted() ? Boolean.TRUE : null,
                convertArtifacts(target, entry.getKey().getArtifacts()),
                CollectionUtils.isEmpty(entry.getValue()) ? null : convertMetadata(entry.getValue()));
    }

    private List<DmfMetadata> convertMetadata(final Map<String, String> metadata) {
        return metadata.entrySet().stream().map(md -> new DmfMetadata(md.getKey(), md.getValue())).toList();
    }

    private List<DmfArtifact> convertArtifacts(final Target target, final List<Artifact> artifacts) {
        if (artifacts.isEmpty()) {
            return Collections.emptyList();
        }

        return artifacts.stream().map(artifact -> convertArtifact(target, artifact)).toList();
    }

    private DmfArtifact convertArtifact(final Target target, final Artifact artifact) {
        final TenantMetaData tenantMetadata = systemManagement.getTenantMetadataWithoutDetails();
        return new DmfArtifact(
                artifact.getFilename(),
                new DmfArtifactHash(artifact.getSha1Hash(), artifact.getMd5Hash()),
                artifact.getSize(),
                artifact.getLastModifiedAt(),
                artifactUrlHandler
                        .getUrls(new DownloadDescriptor(
                                        tenantMetadata.getTenant(), target.getControllerId(),
                                        artifact.getSoftwareModule().getId(), artifact.getFilename(), artifact.getSha1Hash()),
                                ArtifactUrlResolver.ApiType.DMF)
                        .stream()
                        .collect(Collectors.toMap(ArtifactUrl::protocol, ArtifactUrl::ref))
        );
    }

    private Map<SoftwareModule, Map<String, String>> getSoftwareModulesWithMetadata(final DistributionSet distributionSet) {
        final Map<Long, Map<String, String>> softwareModuleMetadata =
                softwareModuleManagement.findMetaDataBySoftwareModuleIdsAndTargetVisible(
                        distributionSet.getModules().stream()
                                .map(SoftwareModule::getId)
                                .toList());
        return distributionSet.getModules().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        module -> softwareModuleMetadata.getOrDefault(module.getId(), Collections.emptyMap())));
    }

    private void sendBatchUpdateMessage(
            final Map<String, ActionProperties> actions, final List<Target> targets,
            final Map<SoftwareModule, Map<String, String>> modules) {
        final List<DmfTarget> dmfTargets = targets.stream()
                .filter(target -> IpUtil.isAmqpUri(IpUtil.addressToUri(target.getAddress())))
                // as system - the security token is sent to DMF receiver
                .map(t -> new DmfTarget(actions.get(t.getControllerId()).getId(), t.getControllerId(), asSystem(t::getSecurityToken)))
                .toList();

        // due to the fact that all targets in a batch use the same set of software modules we don't generate target-specific urls
        final Target firstTarget = targets.get(0);
        final DmfBatchDownloadAndUpdateRequest batchRequest = new DmfBatchDownloadAndUpdateRequest(
                System.currentTimeMillis(),
                dmfTargets,
                Optional.ofNullable(modules)
                        .map(Map::entrySet)
                        .map(Set::stream)
                        .map(stream -> stream.map(entry -> convertToAmqpSoftwareModule(firstTarget, entry)).toList())
                        .orElse(null));

        // we use only the first action when constructing message as AccessContext and action type are the same
        // since all actions have the same trigger
        final ActionProperties firstAction = actions.values().iterator().next();
        final Message message = getMessageConverter().toMessage(
                batchRequest,
                createMessagePropertiesBatch(firstAction.getTenant(), getBatchEventTopicForAction(firstAction)));
        amqpSenderService.sendMessage(message, IpUtil.addressToUri(firstTarget.getAddress()));
    }
}