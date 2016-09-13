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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.api.APIType;
import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.api.URLPlaceholder;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.Artifact;
import org.eclipse.hawkbit.dmf.json.model.ArtifactHash;
import org.eclipse.hawkbit.dmf.json.model.DownloadAndUpdateRequest;
import org.eclipse.hawkbit.dmf.json.model.SoftwareModule;
import org.eclipse.hawkbit.eventbus.EventSubscriber;
import org.eclipse.hawkbit.eventbus.event.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.eventbus.event.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.util.IpUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.eventbus.Subscribe;

/**
 * {@link AmqpMessageDispatcherService} create all outgoing AMQP messages and
 * delegate the messages to a {@link AmqpSenderService}.
 * 
 * Additionally the dispatcher listener/subscribe for some target events e.g.
 * assignment.
 *
 */
@EventSubscriber
public class AmqpMessageDispatcherService extends BaseAmqpService {

    @Autowired
    private ArtifactUrlHandler artifactUrlHandler;

    @Autowired
    private AmqpSenderService amqpSenderService;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Autowired
    private SystemManagement systemManagement;

    /**
     * Constructor.
     * 
     * @param rabbitTemplate
     *            the rabbitTemplate
     */
    @Autowired
    public AmqpMessageDispatcherService(final RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    /**
     * Method to send a message to a RabbitMQ Exchange after the Distribution
     * set has been assign to a Target.
     *
     * @param targetAssignDistributionSetEvent
     *            the object to be send.
     */
    @Subscribe
    public void targetAssignDistributionSet(final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent) {
        final URI targetAdress = targetAssignDistributionSetEvent.getTarget().getTargetInfo().getAddress();
        if (!IpUtil.isAmqpUri(targetAdress)) {
            return;
        }

        final String controllerId = targetAssignDistributionSetEvent.getTarget().getControllerId();
        final Collection<org.eclipse.hawkbit.repository.model.SoftwareModule> modules = targetAssignDistributionSetEvent
                .getSoftwareModules();
        final DownloadAndUpdateRequest downloadAndUpdateRequest = new DownloadAndUpdateRequest();
        downloadAndUpdateRequest.setActionId(targetAssignDistributionSetEvent.getActionId());

        final String targetSecurityToken = systemSecurityContext
                .runAsSystem(() -> targetAssignDistributionSetEvent.getTarget().getSecurityToken());
        downloadAndUpdateRequest.setTargetSecurityToken(targetSecurityToken);

        for (final org.eclipse.hawkbit.repository.model.SoftwareModule softwareModule : modules) {
            final SoftwareModule amqpSoftwareModule = convertToAmqpSoftwareModule(
                    targetAssignDistributionSetEvent.getTarget(), softwareModule);
            downloadAndUpdateRequest.addSoftwareModule(amqpSoftwareModule);
        }

        final Message message = getMessageConverter().toMessage(downloadAndUpdateRequest,
                createConnectorMessageProperties(targetAssignDistributionSetEvent.getTenant(), controllerId,
                        EventTopic.DOWNLOAD_AND_INSTALL));
        amqpSenderService.sendMessage(message, targetAdress);
    }

    /**
     * Method to send a message to a RabbitMQ Exchange after the assignment of
     * the Distribution set to a Target has been canceled.
     *
     * @param cancelTargetAssignmentDistributionSetEvent
     *            the object to be send.
     */
    @Subscribe
    public void targetCancelAssignmentToDistributionSet(
            final CancelTargetAssignmentEvent cancelTargetAssignmentDistributionSetEvent) {
        final String controllerId = cancelTargetAssignmentDistributionSetEvent.getControllerId();
        final Long actionId = cancelTargetAssignmentDistributionSetEvent.getActionId();
        final Message message = getMessageConverter().toMessage(actionId, createConnectorMessageProperties(
                cancelTargetAssignmentDistributionSetEvent.getTenant(), controllerId, EventTopic.CANCEL_DOWNLOAD));

        amqpSenderService.sendMessage(message, cancelTargetAssignmentDistributionSetEvent.getTargetAdress());

    }

    private static MessageProperties createConnectorMessageProperties(final String tenant, final String controllerId,
            final EventTopic topic) {
        final MessageProperties messageProperties = createMessageProperties();
        messageProperties.setHeader(MessageHeaderKey.TOPIC, topic);
        messageProperties.setHeader(MessageHeaderKey.THING_ID, controllerId);
        messageProperties.setHeader(MessageHeaderKey.TENANT, tenant);
        messageProperties.setHeader(MessageHeaderKey.TYPE, MessageType.EVENT);
        return messageProperties;
    }

    private static MessageProperties createMessageProperties() {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setHeader(MessageHeaderKey.CONTENT_TYPE, MessageProperties.CONTENT_TYPE_JSON);
        return messageProperties;
    }

    private SoftwareModule convertToAmqpSoftwareModule(final Target target,
            final org.eclipse.hawkbit.repository.model.SoftwareModule softwareModule) {
        final SoftwareModule amqpSoftwareModule = new SoftwareModule();
        amqpSoftwareModule.setModuleId(softwareModule.getId());
        amqpSoftwareModule.setModuleType(softwareModule.getType().getKey());
        amqpSoftwareModule.setModuleVersion(softwareModule.getVersion());

        final List<Artifact> artifacts = convertArtifacts(target, softwareModule.getLocalArtifacts());
        amqpSoftwareModule.setArtifacts(artifacts);
        return amqpSoftwareModule;
    }

    private List<Artifact> convertArtifacts(final Target target, final List<LocalArtifact> localArtifacts) {
        if (localArtifacts.isEmpty()) {
            return Collections.emptyList();
        }

        return localArtifacts.stream().map(localArtifact -> convertArtifact(target, localArtifact))
                .collect(Collectors.toList());
    }

    private Artifact convertArtifact(final Target target, final LocalArtifact localArtifact) {
        final Artifact artifact = new Artifact();

        artifact.setUrls(artifactUrlHandler
                .getUrls(new URLPlaceholder(systemManagement.getTenantMetadata().getTenant(),
                        systemManagement.getTenantMetadata().getId(), target.getControllerId(), target.getId(),
                        localArtifact.getSoftwareModule().getId(), localArtifact.getFilename(), localArtifact.getId(),
                        localArtifact.getSha1Hash()), APIType.DMF)
                .stream().collect(Collectors.toMap(e -> e.getProtocol(), e -> e.getRef())));

        artifact.setFilename(localArtifact.getFilename());
        artifact.setHashes(new ArtifactHash(localArtifact.getSha1Hash(), localArtifact.getMd5Hash()));
        artifact.setSize(localArtifact.getSize());
        return artifact;
    }

    public void setArtifactUrlHandler(final ArtifactUrlHandler artifactUrlHandler) {
        this.artifactUrlHandler = artifactUrlHandler;
    }

    public void setAmqpSenderService(final AmqpSenderService amqpSenderService) {
        this.amqpSenderService = amqpSenderService;
    }

    public void setSystemSecurityContext(final SystemSecurityContext systemSecurityContext) {
        this.systemSecurityContext = systemSecurityContext;
    }

}
