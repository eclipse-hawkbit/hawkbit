/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.io.Serial;
import java.util.HashMap;

import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RemoteEntityEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;

@ExtendWith(MockitoExtension.class)
class EventProtoStuffMessageConverterTest {

    private final EventProtoStuffMessageConverter underTest = new EventProtoStuffMessageConverter();

    @Mock
    private Target targetMock;

    @Mock
    private Message<Object> messageMock;

    @BeforeEach
    void before() {
        when(targetMock.getId()).thenReturn(1L);
    }

    /**
     * Verifies that the TargetCreatedEvent can be successfully serialized and deserialized
     */
    @Test
    void successfullySerializeAndDeserializeEvent() {
        final TargetCreatedEvent targetCreatedEvent = new TargetCreatedEvent(targetMock);
        // serialize
        final Object serializedEvent = underTest.convertToInternal(targetCreatedEvent,
                new MessageHeaders(new HashMap<>()), null);
        assertThat(serializedEvent).isInstanceOf(byte[].class);

        // deserialize
        when(messageMock.getPayload()).thenReturn(serializedEvent);
        final Object deserializedEvent = underTest.convertFromInternal(messageMock, AbstractRemoteEvent.class, null);
        assertThat(deserializedEvent)
                .isInstanceOf(TargetCreatedEvent.class)
                .isEqualTo(targetCreatedEvent);
    }

    /**
     * Verifies that a MessageConversationException is thrown on missing event-type information encoding
     */
    @Test
    void missingEventTypeMappingThrowsMessageConversationException() {
        final DummyRemoteEntityEvent dummyEvent = new DummyRemoteEntityEvent(targetMock);
        final MessageHeaders messageHeaders = new MessageHeaders(new HashMap<>());

        assertThatExceptionOfType(MessageConversionException.class)
                .as("Missing MessageConversationException for un-defined event-type")
                .isThrownBy(() -> underTest.convertToInternal(dummyEvent, messageHeaders, null));
    }

    /**
     * Test event with which non-existing mapping to serialize.
     */
    private static final class DummyRemoteEntityEvent extends RemoteEntityEvent<Target> {

        @Serial
        private static final long serialVersionUID = 1L;

        private DummyRemoteEntityEvent(final Target target) {
            super(target);
        }
    }
}