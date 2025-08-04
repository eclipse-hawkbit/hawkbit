/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.event;

import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.service.ActionCreatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetCreatedServiceEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;

abstract class AbstractEventMessageConverterTest {

    protected final MessageConverter messageConverter;

    @Mock
    protected Message<Object> messageMock;

    @Mock
    protected Target targetMock;

    @Mock
    protected Action actionMock;

    @BeforeEach
    void before() {
        when(targetMock.getId()).thenReturn(1L);
        when(actionMock.getId()).thenReturn(1L);
        when(actionMock.getTenant()).thenReturn("test_tenant");
    }

    AbstractEventMessageConverterTest(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    /**
     * Verifies that the TargetCreatedEvent can be successfully serialized and deserialized
     */
    @Test
    void successfullySerializeAndDeserializeEvent() {
        final TargetCreatedEvent targetCreatedEvent = new TargetCreatedEvent(targetMock);
        // serialize
        assertSerializeAndDeserialize(targetCreatedEvent, TargetCreatedEvent.class);
        assertSerializeAndDeserialize(targetCreatedEvent, TargetCreatedEvent.class);
    }

    @Test
    void successfullySerializeAndDeserializeServiceEvent() {
        final TargetCreatedEvent targetCreatedEvent = new TargetCreatedEvent(targetMock);
        final TargetCreatedServiceEvent targetCreatedServiceEvent = new TargetCreatedServiceEvent(targetCreatedEvent);
        assertSerializeAndDeserialize(targetCreatedServiceEvent, TargetCreatedServiceEvent.class);
    }

    @Test
    void successfullySerializeAndDeserializeActionCreatedServiceEvent() {
        final ActionCreatedEvent actionCreatedEvent =
                new ActionCreatedEvent(actionMock, 1L, 2L, 3L);
        final ActionCreatedServiceEvent actionCreatedServiceEvent =
                new ActionCreatedServiceEvent(actionCreatedEvent);
        assertSerializeAndDeserialize(actionCreatedServiceEvent,ActionCreatedServiceEvent.class);
    }

    <T extends AbstractRemoteEvent> void assertSerializeAndDeserialize(T event, Class<? extends AbstractRemoteEvent> expectedClass) {
        // serialize
        Object serializedEvent = null;
        if (messageConverter instanceof EventProtoStuffMessageConverter protoStuff) {
            serializedEvent = protoStuff.convertToInternal(event, new MessageHeaders(new HashMap<>()), null);
        } else if (messageConverter instanceof EventJacksonMessageConverter jackson) {
            serializedEvent = jackson.convertToInternal(event, new MessageHeaders(new HashMap<>()), null);
        }
        assertThat(serializedEvent).isInstanceOf(byte[].class);

        // deserialize
        when(messageMock.getPayload()).thenReturn(serializedEvent);
        Object deserializedEvent = null;
        if (messageConverter instanceof EventProtoStuffMessageConverter protoStuff) {
            deserializedEvent = protoStuff.convertFromInternal(messageMock, AbstractRemoteEvent.class, null);
        } else if (messageConverter instanceof EventJacksonMessageConverter jackson) {
            deserializedEvent = jackson.convertFromInternal(messageMock, AbstractRemoteEvent.class, null);
        }
        assertThat(deserializedEvent)
                .isInstanceOf(expectedClass)
                .isEqualTo(event);
    }
}
