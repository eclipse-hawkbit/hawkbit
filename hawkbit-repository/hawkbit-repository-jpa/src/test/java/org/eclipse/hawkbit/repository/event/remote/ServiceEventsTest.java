/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.service.CancelTargetAssignmentServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetAssignDistributionSetServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetAttributesRequestedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetCreatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetDeletedServiceEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher;

class ServiceEventsTest {

    private StreamBridge streamBridge;
    private ApplicationEventPublisher delegate;
    private ApplicationEventPublisher publisher;

    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchFieldException {
        streamBridge = mock(StreamBridge.class);
        delegate = mock(ApplicationEventPublisher.class);
        EventPublisherHolder.getInstance().setApplicationEventPublisher(delegate);
        EventPublisherHolder.getInstance().setStreamBridge(streamBridge);
        publisher = EventPublisherHolder.getInstance().getEventPublisher();

        // Set up fields via reflection
        var remoteEventsEnabledField = EventPublisherHolder.class.getDeclaredField("remoteEventsEnabled");
        remoteEventsEnabledField.setAccessible(true);
        remoteEventsEnabledField.set(EventPublisherHolder.getInstance(), true);

        var remoteServiceEventsEnabledField = EventPublisherHolder.class.getDeclaredField("remoteServiceEventsEnabled");
        remoteServiceEventsEnabledField.setAccessible(true);
        remoteServiceEventsEnabledField.set(EventPublisherHolder.getInstance(), true);

        var fanoutChannelField = EventPublisherHolder.class.getDeclaredField("fanoutEventChannel");
        fanoutChannelField.setAccessible(true);
        fanoutChannelField.set(EventPublisherHolder.getInstance(), "fanout");

        var groupChannelField = EventPublisherHolder.class.getDeclaredField("serviceEventChannel");
        groupChannelField.setAccessible(true);
        groupChannelField.set(EventPublisherHolder.getInstance(), "group");
    }

    @Test
    void testExpectedServiceEvents(){
        var expected = Set.of(
                TargetCreatedEvent.class,
                TargetUpdatedEvent.class,
                TargetDeletedEvent.class,
                TargetAssignDistributionSetEvent.class,
                CancelTargetAssignmentEvent.class,
                TargetAttributesRequestedEvent.class,
                ActionCreatedEvent.class,
                ActionUpdatedEvent.class
        );
        assertEquals(EventPublisherHolder.SERVICE_EVENTS, expected);
    }

    @Test
    void testProcessingTargetAssignDistributionSetEventIsSent() {
        TargetAssignDistributionSetEvent event = new TargetAssignDistributionSetEvent(mockAction());

        publisher.publishEvent(event);

        verify(streamBridge).send("fanout", event);
        verify(streamBridge).send(eq("group"), any(TargetAssignDistributionSetServiceEvent.class));
    }

    @Test
    void testProcessingTargetCreatedEventIsSent() {
        TargetCreatedEvent event = new TargetCreatedEvent(mock(Target.class));
        publisher.publishEvent(event);

        verify(streamBridge).send("fanout", event);
        verify(streamBridge).send(eq("group"), any(TargetCreatedServiceEvent.class));
    }

    @Test
    void testProcessingTargetDeletedEventIsSent() {
        TargetDeletedEvent event = new TargetDeletedEvent("testtenant", 1l, Target.class, "testControllerId", "address");
        publisher.publishEvent(event);

        verify(streamBridge).send("fanout", event);
        verify(streamBridge).send(eq("group"), any(TargetDeletedServiceEvent.class));
    }

    @Test
    void testProcessingTargetAttributesRequestedEventIsSent() {
        TargetAttributesRequestedEvent event = new TargetAttributesRequestedEvent("testtenant", 1l, Target.class, "testControllerId","address");
        publisher.publishEvent(event);

        verify(streamBridge).send("fanout", event);
        verify(streamBridge).send(eq("group"), any(TargetAttributesRequestedServiceEvent.class));
    }

    @Test
    void testCancelTargetAssignmentEventIsSent() {
        CancelTargetAssignmentEvent event = new CancelTargetAssignmentEvent(mockAction());

        publisher.publishEvent(event);

        verify(streamBridge).send("fanout", event);
        verify(streamBridge).send(eq("group"), any(CancelTargetAssignmentServiceEvent.class));
    }

    private Action mockAction() {
        final Action actionMock = mock(Action.class);
        final Target targetMock = mock(Target.class);
        final DistributionSet distributionSetMock = mock(DistributionSet.class);
        when(distributionSetMock.getId()).thenReturn(1L);
        when(actionMock.getDistributionSet()).thenReturn(distributionSetMock);
        when(actionMock.getId()).thenReturn(1l);
        when(actionMock.getTenant()).thenReturn("DEFAULT");
        when(actionMock.getTarget()).thenReturn(targetMock);
        when(actionMock.getActionType()).thenReturn(Action.ActionType.SOFT);
        when(targetMock.getControllerId()).thenReturn("target1");
        return actionMock;
    }
}