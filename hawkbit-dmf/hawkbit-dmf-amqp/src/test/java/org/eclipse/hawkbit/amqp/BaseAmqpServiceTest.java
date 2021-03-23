/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@ExtendWith(MockitoExtension.class)
@Feature("Component Tests - Device Management Federation API")
@Story("Base Amqp Service Test")
public class BaseAmqpServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private BaseAmqpService baseAmqpService;

    @BeforeEach
    public void setup() {
        baseAmqpService = new BaseAmqpService(rabbitTemplate);
    }

    @Test
    @Description("Verify that the message conversion works")
    public void convertMessageTest() {
        final DmfActionUpdateStatus actionUpdateStatus = createActionStatus();
        when(rabbitTemplate.getMessageConverter()).thenReturn(new Jackson2JsonMessageConverter());

        final Message message = rabbitTemplate.getMessageConverter().toMessage(actionUpdateStatus,
                createJsonProperties());
        final DmfActionUpdateStatus convertedActionUpdateStatus = baseAmqpService.convertMessage(message,
                DmfActionUpdateStatus.class);

        assertThat(convertedActionUpdateStatus).isEqualToComparingFieldByField(actionUpdateStatus);
    }

    @Test
    @Description("Tests invalid null message content")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void convertMessageWithNullContent() {
        final Message message = createMessage(null);
        assertThatExceptionOfType(MessageConversionException.class)
                .as("Expected MessageConversionException for invalid JSON")
                .isThrownBy(() -> baseAmqpService.convertMessage(message, DmfActionUpdateStatus.class));
    }

    @Test
    @Description("Tests invalid empty message content")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void updateActionStatusWithEmptyContent() {
        final Message message = createMessage("".getBytes());
        assertThatExceptionOfType(MessageConversionException.class)
                .as("Expected MessageConversionException for invalid JSON")
                .isThrownBy(() -> baseAmqpService.convertMessage(message, DmfActionUpdateStatus.class));
    }

    @Test
    @Description("Tests invalid json message content")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void updateActionStatusWithInvalidJsonContent() {
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
        final DmfActionUpdateStatus actionUpdateStatus = new DmfActionUpdateStatus(1L, DmfActionStatus.RUNNING);
        actionUpdateStatus.setSoftwareModuleId(2L);
        actionUpdateStatus.addMessage("Message 1");
        actionUpdateStatus.addMessage("Message 2");
        return actionUpdateStatus;
    }

}
