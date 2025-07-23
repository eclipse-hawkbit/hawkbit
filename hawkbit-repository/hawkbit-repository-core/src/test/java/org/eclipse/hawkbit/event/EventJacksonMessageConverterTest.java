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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@ExtendWith(MockitoExtension.class)
class EventJacksonMessageConverterTest {

    private final TestEventJacksonMessageConverter underTest = new TestEventJacksonMessageConverter();

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
     * Test subclass to expose protected methods for testing.
     */
    private static class TestEventJacksonMessageConverter extends EventJacksonMessageConverter {
        @Override
        public Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
            return super.convertToInternal(payload, headers, conversionHint);
        }

        @Override
        public Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
            return super.convertFromInternal(message, targetClass, conversionHint);
        }
    }
}