/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event;

import java.util.Set;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEvent;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.MultiActionCancelEvent;
import org.eclipse.hawkbit.repository.event.remote.ServiceCancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.ServiceMultiActionAssignEvent;
import org.eclipse.hawkbit.repository.event.remote.ServiceMultiActionCancelEvent;
import org.eclipse.hawkbit.repository.event.remote.ServiceTargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.ServiceTargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.ServiceTargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.ServiceTargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.MultiActionAssignEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
public final class EventPublisherHolder {

    @Value("${org.eclipse.hawkbit.events.remote-enabled:true}")
    private boolean remoteEventsEnabled;
    @Value("${org.eclipse.hawkbit.events.remote.destination:fanoutEventChannel}")
    private String fanoutEventChannel;
    @Value("${org.eclipse.hawkbit.events.remote-service-enabled:true}")
    private boolean remoteServiceEventsEnabled;
    @Value("${org.eclipse.hawkbit.events.remote.service.destination:serviceEventChannel}")
    private String serviceEventChannel;


    private static final EventPublisherHolder SINGLETON = new EventPublisherHolder();
    private ApplicationEventPublisher delegateEventPublisher;
    private StreamBridge streamBridge;

    public static EventPublisherHolder getInstance() {
        return SINGLETON;
    }

    @PostConstruct
    private void validateRemoteEventConfig() {
        if (remoteEventsEnabled && streamBridge == null) {
            throw new IllegalStateException("'org.eclipse.hawkbit.events.remote-enabled' is true but streamBridge is not configured. Check if 'spring-cloud-starter-stream-rabbit' dependency is included.");
        }
    }

    public static final Set<Class<?>> SERVICE_EVENTS = Set.of(
            TargetCreatedEvent.class,
            TargetDeletedEvent.class,
            MultiActionAssignEvent.class,
            MultiActionCancelEvent.class,
            TargetAssignDistributionSetEvent.class,
            TargetAttributesRequestedEvent.class,
            CancelTargetAssignmentEvent.class
    );

    @Autowired
    public void setApplicationEventPublisher(final ApplicationEventPublisher delegate) {
        this.delegateEventPublisher = delegate;
    }

    @Autowired(required = false)
    public void setStreamBridge(final StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public ApplicationEventPublisher getEventPublisher() {
        return new RoutingEventPublisher(streamBridge, delegateEventPublisher);
    }

    class RoutingEventPublisher implements ApplicationEventPublisher {

        private final StreamBridge streamBridge;
        private final ApplicationEventPublisher delegate;

        public RoutingEventPublisher(final StreamBridge streamBridge, final ApplicationEventPublisher delegate) {
            this.streamBridge = streamBridge;
            this.delegate = delegate;
        }

        @Override
        public void publishEvent(final Object event) {
            routeEvent(event);
        }

        @Override
        public void publishEvent(final ApplicationEvent event) {
            routeEvent(event);
        }

        private void routeEvent(Object event) {
            if (remoteEventsEnabled && event instanceof AbstractRemoteEvent remoteEvent) {
                // send events to remote nodes
                publishRemotely(remoteEvent);
            } else {
                // publish locally
                publishLocally(event);
            }
        }

        private void publishRemotely(final AbstractRemoteEvent remoteEvent) {
            streamBridge.send(fanoutEventChannel, remoteEvent);

            // some events need to be processed only by single replica of a kind
            // wrap the entity event into a processing event and send it to the group channel
            if (shouldForwardAsServiceEvent(remoteEvent)) {
                final AbstractRemoteEvent serviceEvent = toServiceEvent(remoteEvent);
                if (serviceEvent != null) {
                    log.debug("Publishing Service event: {} to remote channel: {}", serviceEvent, serviceEventChannel);
                    streamBridge.send(serviceEventChannel, serviceEvent);
                } else {
                    log.error("No Service event created for: {}. Skipping send Service event to Group channel. {}", remoteEvent.getClass(),
                            serviceEventChannel);
                }
            }
        }

        private void publishLocally(final Object event) {
            delegate.publishEvent(event);

            // check if the event should be forwarded as a processing event even if it is not a remote event
            if (shouldForwardAsServiceEvent(event)) {
                final AbstractRemoteEvent serviceEvent = toServiceEvent((AbstractRemoteEvent) event);
                if (serviceEvent != null) {
                    log.debug("Publishing Service event: {} to locally.", serviceEvent);
                    delegate.publishEvent(serviceEvent);
                } else {
                    log.error("No Service event created for: {}. Skipping send Service event locally.", event.getClass());
                }
            }
        }
    }

    private boolean shouldForwardAsServiceEvent(final Object remoteEvent) {
        return remoteServiceEventsEnabled && SERVICE_EVENTS.contains(remoteEvent.getClass());
    }

    private AbstractRemoteEvent toServiceEvent(final AbstractRemoteEvent event) {
        if (event instanceof TargetAssignDistributionSetEvent targetAssignDistributionSetEvent) {
            return new ServiceTargetAssignDistributionSetEvent(targetAssignDistributionSetEvent);
        } else if (event instanceof MultiActionAssignEvent multiActionAssignEvent) {
            return new ServiceMultiActionAssignEvent(multiActionAssignEvent);
        } else if (event instanceof MultiActionCancelEvent multiActionCancelEvent) {
          return new ServiceMultiActionCancelEvent(multiActionCancelEvent);
        } else if (event instanceof CancelTargetAssignmentEvent cancelTargetAssignmentEvent) {
            return new ServiceCancelTargetAssignmentEvent(cancelTargetAssignmentEvent);
        } else if (event instanceof TargetDeletedEvent targetDeletedEvent) {
            return new ServiceTargetDeletedEvent(targetDeletedEvent);
        } else if (event instanceof TargetCreatedEvent targetCreatedEvent) {
            return new ServiceTargetCreatedEvent(targetCreatedEvent);
        } else if (event instanceof TargetAttributesRequestedEvent targetAttributesRequestedEvent) {
            return new ServiceTargetAttributesRequestedEvent(targetAttributesRequestedEvent);
        }
        return null;
    }
}