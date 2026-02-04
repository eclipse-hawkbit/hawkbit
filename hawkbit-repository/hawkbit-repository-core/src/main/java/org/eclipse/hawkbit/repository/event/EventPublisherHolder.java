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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEvent;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.service.ActionCreatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.ActionUpdatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.CancelTargetAssignmentServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetAssignDistributionSetServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetAttributesRequestedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetCreatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetDeletedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetUpdatedServiceEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
public final class EventPublisherHolder {

    @Value("${hawkbit.events.remote.enabled:true}")
    private boolean remoteEventsEnabled;
    @Value("${hawkbit.events.remote.destination:fanoutEventChannel}")
    private String fanoutEventChannel;
    @Value("${hawkbit.events.remote-service-enabled:true}")
    private boolean remoteServiceEventsEnabled;
    @Value("${hawkbit.events.remote.service.destination:serviceEventChannel}")
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
            throw new IllegalStateException("'hawkbit.events.remote.enabled' is true but streamBridge is not configured. Check if 'spring-cloud-starter-stream-rabbit' dependency is included.");
        }
    }

    public static final Set<Class<?>> SERVICE_EVENTS = Set.of(
            TargetCreatedEvent.class,
            TargetUpdatedEvent.class,
            TargetDeletedEvent.class,
            TargetAssignDistributionSetEvent.class,
            CancelTargetAssignmentEvent.class,
            TargetAttributesRequestedEvent.class,
            ActionCreatedEvent.class,
            ActionUpdatedEvent.class
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
        public void publishEvent(@NonNull final Object event) {
            routeEvent(event);
        }

        @Override
        public void publishEvent(@NonNull final ApplicationEvent event) {
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

            // some events need to be processed only by single service replica
            // wrap the entity event into a service event and send it to the service channel
            if (shouldForwardAsServiceEvent(remoteEvent)) {
                final AbstractRemoteEvent serviceEvent = toServiceEvent(remoteEvent);
                if (serviceEvent != null) {
                    log.debug("Publishing Service event: {} to remote channel: {}", serviceEvent, serviceEventChannel);
                    streamBridge.send(serviceEventChannel, serviceEvent);
                } else {
                    log.error("No Service event created for: {}. Skipping send Service event to Service channel. {}",
                            remoteEvent.getClass(), serviceEventChannel);
                }
            }
        }

        private void publishLocally(final Object event) {
            delegate.publishEvent(event);

            // check if the event should be forwarded as a service event even if it is not a remote event
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

        /**
         * Checks if the event should be forwarded as a service event.
         * If remote service events are enabled and the event is one of the service events,
         *
         * @param remoteEvent the event to check whether it should be forwarded as a service event
         * @return true if the event should be forwarded as a service event, false otherwise
         */
        private boolean shouldForwardAsServiceEvent(final Object remoteEvent) {
            return remoteServiceEventsEnabled && SERVICE_EVENTS.contains(remoteEvent.getClass());
        }

        private AbstractRemoteEvent toServiceEvent(final AbstractRemoteEvent event) {
            if (event instanceof TargetCreatedEvent targetCreatedEvent) {
                return new TargetCreatedServiceEvent(targetCreatedEvent);
            } else if (event instanceof TargetUpdatedEvent targetUpdatedEvent) {
                return new TargetUpdatedServiceEvent(targetUpdatedEvent);
            } else if (event instanceof TargetDeletedEvent targetDeletedEvent) {
                return new TargetDeletedServiceEvent(targetDeletedEvent);
            } else if (event instanceof TargetAssignDistributionSetEvent targetAssignDistributionSetEvent) {
                return new TargetAssignDistributionSetServiceEvent(targetAssignDistributionSetEvent);
            }  else if (event instanceof CancelTargetAssignmentEvent cancelTargetAssignmentEvent) {
                return new CancelTargetAssignmentServiceEvent(cancelTargetAssignmentEvent);
            } else if (event instanceof TargetAttributesRequestedEvent targetAttributesRequestedEvent) {
                return new TargetAttributesRequestedServiceEvent(targetAttributesRequestedEvent);
            } else if (event instanceof ActionCreatedEvent actionCreatedEvent) {
                return new ActionCreatedServiceEvent(actionCreatedEvent);
            } else if (event instanceof ActionUpdatedEvent actionUpdatedEvent) {
                return new ActionUpdatedServiceEvent(actionUpdatedEvent);
            }
            return null;
        }
    }
}