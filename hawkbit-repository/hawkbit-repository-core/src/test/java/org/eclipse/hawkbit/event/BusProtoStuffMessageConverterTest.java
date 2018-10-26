/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.event.remote.entity.RemoteEntityEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;

import ru.yandex.qatools.allure.annotations.Description;

@RunWith(MockitoJUnitRunner.class)
public class BusProtoStuffMessageConverterTest {

    private final BusProtoStuffMessageConverter underTest = new BusProtoStuffMessageConverter();

    @Mock
    private Target targetMock;

    @Mock
    private Message<Object> messageMock;

    @Before
    public void before() {
        when(targetMock.getId()).thenReturn(1L);
    }

    @Test
    @Description("Verifies that the TargetCreatedEvent can be successfully serialized and deserialized")
    public void successfullySerializeAndDeserializeEvent() {
        final TargetCreatedEvent targetCreatedEvent = new TargetCreatedEvent(targetMock, "1");
        // serialize
        final Object serializedEvent = underTest.convertToInternal(targetCreatedEvent,
                new MessageHeaders(new HashMap<>()), null);
        assertThat(serializedEvent).isInstanceOf(byte[].class);

        // deserialize
        when(messageMock.getPayload()).thenReturn(serializedEvent);
        final Object deserializedEvent = underTest.convertFromInternal(messageMock, RemoteApplicationEvent.class, null);
        assertThat(deserializedEvent).isInstanceOf(TargetCreatedEvent.class);
        assertThat(deserializedEvent).isEqualTo(targetCreatedEvent);
    }

    @Test
    @Description("Verifies that a MessageConversationException is thrown on missing event-type information encoding")
    public void missingEventTypeMappingThrowsMessageConversationException() {
        final DummyRemoteEntityEvent dummyEvent = new DummyRemoteEntityEvent(targetMock, "applicationId");
        try {
            underTest.convertToInternal(dummyEvent, new MessageHeaders(new HashMap<>()), null);
            Assertions.fail("Missing MessageConversationException for un-defined event-type");
        } catch (final MessageConversionException e) {
            // expected exception
        }
    }

    /**
     * Test event with which non-existing mapping to serialize.
     */
    private final class DummyRemoteEntityEvent extends RemoteEntityEvent<Target> {

        private static final long serialVersionUID = 1L;

        private DummyRemoteEntityEvent(final Target target, final String applicationId) {
            super(target, applicationId);
        }

    }
}
