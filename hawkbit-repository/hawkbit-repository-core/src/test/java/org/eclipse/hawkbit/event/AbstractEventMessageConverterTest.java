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
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.service.ActionCreatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.ActionUpdatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetCreatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetDeletedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetUpdatedServiceEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;

abstract class AbstractEventMessageConverterTest {

    protected final MessageConverter messageConverter;

    @Mock
    protected Message<Object> messageMock;

    @Mock
    protected Target targetMock;

    @Mock
    protected Action actionMock;

    AbstractEventMessageConverterTest(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    /**
     * Verifies that the TargetCreatedEvent can be successfully serialized and deserialized
     */
    @Test
    void successfullySerializeAndDeserializeTargetEvent() {
        assertSerializeAndDeserialize(createTargetCreatedEvent(), TargetCreatedEvent.class);
        assertSerializeAndDeserialize(createTargetUpdatedEvent(), TargetUpdatedEvent.class);
        assertSerializeAndDeserialize(createTargetDeletedEvent(), TargetDeletedEvent.class);
    }

    @Test
    void successfullySerializeAndDeserializeTargetServiceEvent() {
        final TargetCreatedServiceEvent targetCreatedServiceEvent = new TargetCreatedServiceEvent(createTargetCreatedEvent());
        final TargetUpdatedServiceEvent targetUpdatedServiceEvent = new TargetUpdatedServiceEvent(createTargetUpdatedEvent());
        final TargetDeletedServiceEvent targetDeletedServiceEvent = new TargetDeletedServiceEvent(createTargetDeletedEvent());

        assertSerializeAndDeserialize(targetCreatedServiceEvent, TargetCreatedServiceEvent.class);
        assertSerializeAndDeserialize(targetUpdatedServiceEvent, TargetUpdatedServiceEvent.class);
        assertSerializeAndDeserialize(targetDeletedServiceEvent, TargetDeletedServiceEvent.class);
    }

    @Test
    void successfullySerializeAndDeserializeActionEvent() {
        final ActionCreatedEvent actionCreatedEvent = createActionCreatedEvent();
        final ActionUpdatedEvent actionUpdatedEvent = createActionUpdatedEvent();

        assertSerializeAndDeserialize(actionCreatedEvent, ActionCreatedEvent.class);
        assertSerializeAndDeserialize(actionUpdatedEvent, ActionUpdatedEvent.class);
    }

    @Test
    void successfullySerializeAndDeserializeActionServiceEvent() {
        final ActionCreatedServiceEvent actionCreatedServiceEvent =
                new ActionCreatedServiceEvent(createActionCreatedEvent());

        final ActionUpdatedServiceEvent actionUpdatedServiceEvent =
                new ActionUpdatedServiceEvent(createActionUpdatedEvent());

        assertSerializeAndDeserialize(actionCreatedServiceEvent, ActionCreatedServiceEvent.class);
        assertSerializeAndDeserialize(actionUpdatedServiceEvent, ActionUpdatedServiceEvent.class);
    }

    private TargetCreatedEvent createTargetCreatedEvent() {
        return new TargetCreatedEvent(targetMock);
    }

    private TargetUpdatedEvent createTargetUpdatedEvent() {
        return new TargetUpdatedEvent(targetMock);
    }

    private TargetDeletedEvent createTargetDeletedEvent() {
        return new TargetDeletedEvent("test_tenant", 1L, Target.class, "test_target", "test_reason");
    }

    private ActionCreatedEvent createActionCreatedEvent() {
        return new ActionCreatedEvent(actionMock, 1L, 2L, 3L);
    }

    private ActionUpdatedEvent createActionUpdatedEvent() {
        return new ActionUpdatedEvent(actionMock, 1L, 2L, 3L);
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
