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
import org.eclipse.hawkbit.repository.event.remote.MultiActionEvent;
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

        private static final Set<Class<?>> GROUPED_REMOTE_EVENTS = Set.of(
                TargetAssignDistributionSetEvent.class,
                MultiActionEvent.class,
                CancelTargetAssignmentEvent.class,
                TargetDeletedEvent.class,
                TargetAttributesRequestedEvent.class
        );

        public RoutingEventPublisher(StreamBridge streamBridge, ApplicationEventPublisher delegate) {
            this.streamBridge = streamBridge;
            this.delegate = delegate;
        }

        @Override
        public void publishEvent(Object event) {
            routeEvent(event);
        }

        @Override
        public void publishEvent(ApplicationEvent event) {
            routeEvent(event);
        }

        private void routeEvent(Object event) {
            if (remoteEventsEnabled && streamBridge != null && event instanceof AbstractRemoteEvent) {
                if (GROUPED_REMOTE_EVENTS.contains(event.getClass())) {
                    streamBridge.send(groupChannel, event);
                } else {
                    streamBridge.send(fanoutChannel, event);
                }
            } else if (delegate != null) {
                delegate.publishEvent(event);
            } else {
                log.error("Could not publish (neither remote nor local) Event: {}.", event);
            }
        }
    }
}