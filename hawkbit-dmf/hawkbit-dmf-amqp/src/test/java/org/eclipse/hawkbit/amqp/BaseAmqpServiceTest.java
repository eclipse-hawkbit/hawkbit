/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;

/**
 * Feature: Component Tests - Device Management Federation API<br/>
 * Story: Base Amqp Service Test
 */
@ExtendWith(MockitoExtension.class)
class BaseAmqpServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private BaseAmqpService baseAmqpService;

    @BeforeEach
    void setup() {
        baseAmqpService = new BaseAmqpService(rabbitTemplate);
    }

    /**
     * Verify that the message conversion works
     */
    @Test
    void convertMessageTest() {
        final DmfActionUpdateStatus actionUpdateStatus = createActionStatus();
        when(rabbitTemplate.getMessageConverter()).thenReturn(new Jackson2JsonMessageConverter());

        final Message message = rabbitTemplate.getMessageConverter().toMessage(actionUpdateStatus, createJsonProperties());
        final DmfActionUpdateStatus convertedActionUpdateStatus = baseAmqpService.convertMessage(message, DmfActionUpdateStatus.class);
        assertThat(convertedActionUpdateStatus).usingRecursiveComparison().isEqualTo(actionUpdateStatus);
    }

    /**
     * Tests invalid null message content
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    void convertMessageWithNullContent() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .as("Expected IllegalArgumentException for invalid (null) JSON")
                .isThrownBy(() -> createMessage(null));
    }

    /**
     * Tests invalid empty message content
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    void updateActionStatusWithEmptyContent() {
        final Message message = createMessage("".getBytes());
        assertThatExceptionOfType(MessageConversionException.class)
                .as("Expected MessageConversionException for invalid (empty) JSON")
                .isThrownBy(() -> baseAmqpService.convertMessage(message, DmfActionUpdateStatus.class));
    }

    /**
     * Tests invalid json message content
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    void updateActionStatusWithInvalidJsonContent() {
        final Message message = createMessage("Invalid Json".getBytes());
        when(rabbitTemplate.getMessageConverter()).thenReturn(new Jackson2JsonMessageConverter());

        assertThatExceptionOfType(MessageConversionException.class)
                .as("Expected MessageConversionException for invalid JSON")
                .isThrownBy(() -> baseAmqpService.convertMessage(message, DmfActionUpdateStatus.class));
    }

    private Message createMessage(final byte[] body) {
        return new Message(body, createJsonProperties());
    }

    private MessageProperties createJsonProperties() {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        return messageProperties;
    }

    private DmfActionUpdateStatus createActionStatus() {
        return new DmfActionUpdateStatus(1L, DmfActionStatus.RUNNING, null, 2L, List.of("Message 1", "Message 2"), 2);
    }

}
