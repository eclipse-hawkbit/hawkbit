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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEvent;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.ProcessingCancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.ProcessingMultiActionAssignEvent;
import org.eclipse.hawkbit.repository.event.remote.ProcessingTargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.ProcessingTargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.ProcessingTargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.MultiActionAssignEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
public final class EventPublisherHolder {

    @Value("${org.eclipse.hawkbit.events.remote-enabled:false}")
    private boolean remoteEventsEnabled;
    @Value("${org.eclipse.hawkbit.events.forward.processing.enabled:true}")
    private boolean processingEventsEnabled;
    @Value("${org.eclipse.hawkbit.events.remote.group.destination:groupEventChannel}")
    private String groupChannel;
    @Value("${org.eclipse.hawkbit.events.remote.fanout.destination:fanoutEventChannel}")
    private String fanoutChannel;

    private static final EventPublisherHolder SINGLETON = new EventPublisherHolder();
    private ApplicationEventPublisher delegateEventPublisher;
    private StreamBridge streamBridge;

    public static EventPublisherHolder getInstance() {
        return SINGLETON;
    }

    public static final Set<Class<?>> PROCESSING_REMOTE_EVENTS = Set.of(
            TargetAssignDistributionSetEvent.class,
            MultiActionAssignEvent.class,
            CancelTargetAssignmentEvent.class,
            TargetDeletedEvent.class,
            TargetAttributesRequestedEvent.class
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
            if (remoteEventsEnabled && streamBridge != null && event instanceof AbstractRemoteEvent remoteEvent) {
                // send events to remote nodes
                publishRemotely(remoteEvent);
            } else if (delegate != null) {
                // publish locally
                publishLocally(event);
            } else {
                log.error("Could not publish (neither remote nor local) Event: {}.", event);
            }
        }

        private void publishRemotely(final AbstractRemoteEvent remoteEvent) {
            streamBridge.send(fanoutChannel, remoteEvent);

            // some events need to be processed only by single replica of a kind
            // wrap the entity event into a grouped event and send it to the group channel
            if (shouldForwardAsProcessingEvent(remoteEvent)) {
                final AbstractRemoteEvent groupedEvent = toProcessingEvent(remoteEvent);
                if (groupedEvent != null) {
                    log.debug("Publishing grouped event: {} to channel: {}", groupedEvent, groupChannel);
                    streamBridge.send(groupChannel, groupedEvent);
                } else {
                    log.error("No grouped event created for: {}. Skipping send event to group channel.", remoteEvent.getClass());
                }
            }
        }

        private void publishLocally(final Object event) {
            delegate.publishEvent(event);

            // even though the event is published locally
            // we still want to check if it should be forwarded as a grouped event - e.g. monolith
            if (shouldForwardAsProcessingEvent(event)) {
                final AbstractRemoteEvent groupedEvent = toProcessingEvent((AbstractRemoteEvent) event);
                if (groupedEvent != null) {
                    log.debug("Publishing grouped event: {} to locally: {}", groupedEvent, groupChannel);
                    delegate.publishEvent(groupedEvent);
                } else {
                    log.error("No grouped event created for: {}. Skipping send event to group channel.", event.getClass());
                }
            }
        }
    }

    private boolean shouldForwardAsProcessingEvent(final Object remoteEvent) {
        return processingEventsEnabled && PROCESSING_REMOTE_EVENTS.contains(remoteEvent.getClass());
    }

    private AbstractRemoteEvent toProcessingEvent(final AbstractRemoteEvent event) {
        if (event instanceof TargetAssignDistributionSetEvent) {
            return new ProcessingTargetAssignDistributionSetEvent((TargetAssignDistributionSetEvent) event);
        } else if (event instanceof MultiActionAssignEvent) {
            return new ProcessingMultiActionAssignEvent((MultiActionAssignEvent) event);
        } else if (event instanceof CancelTargetAssignmentEvent) {
            return new ProcessingCancelTargetAssignmentEvent((CancelTargetAssignmentEvent) event);
        } else if (event instanceof TargetDeletedEvent) {
            return new ProcessingTargetDeletedEvent((TargetDeletedEvent) event);
        } else if (event instanceof TargetAttributesRequestedEvent) {
            return new ProcessingTargetAttributesRequestedEvent((TargetAttributesRequestedEvent) event);
        }
        return null;
    }
}