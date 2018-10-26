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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@RunWith(MockitoJUnitRunner.class)
@Features("Component Tests - Device Management Federation API")
@Stories("Base Amqp Service Test")
public class BaseAmqpServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private BaseAmqpService baseAmqpService;

    @Before
    public void setup() {
        when(rabbitTemplate.getMessageConverter()).thenReturn(new Jackson2JsonMessageConverter());
        baseAmqpService = new BaseAmqpService(rabbitTemplate);

    }

    @Test
    @Description("Verify that the message conversion works")
    public void convertMessageTest() {
        final DmfActionUpdateStatus actionUpdateStatus = createActionStatus();

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
        try {
            baseAmqpService.convertMessage(new Message(null, createJsonProperties()), DmfActionUpdateStatus.class);
            fail("Expected MessageConversionException for inavlid JSON");
        } catch (final MessageConversionException e) {
            // expected
        }

    }

    @Test
    @Description("Tests invalid empty message content")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void updateActionStatusWithEmptyContent() {
        try {
            baseAmqpService.convertMessage(new Message("".getBytes(), createJsonProperties()),
                    DmfActionUpdateStatus.class);
            fail("Expected MessageConversionException for inavlid JSON");
        } catch (final MessageConversionException e) {
            // expected
        }
    }

    private MessageProperties createJsonProperties() {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        return messageProperties;
    }

    @Test
    @Description("Tests invalid json message content")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void updateActionStatusWithInvalidJsonContent() {
        try {
            baseAmqpService.convertMessage(new Message("Invalid Json".getBytes(), createJsonProperties()),
                    DmfActionUpdateStatus.class);
            fail("Expected MessageConversionException for inavlid JSON");
        } catch (final MessageConversionException e) {
            // expected
        }
    }

    private DmfActionUpdateStatus createActionStatus() {
        final DmfActionUpdateStatus actionUpdateStatus = new DmfActionUpdateStatus(1L, DmfActionStatus.RUNNING);
        actionUpdateStatus.setSoftwareModuleId(2L);
        actionUpdateStatus.addMessage("Message 1");
        actionUpdateStatus.addMessage("Message 2");
        return actionUpdateStatus;
    }

}
