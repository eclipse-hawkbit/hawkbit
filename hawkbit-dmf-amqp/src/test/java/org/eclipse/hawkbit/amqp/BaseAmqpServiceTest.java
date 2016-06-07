/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.dmf.json.model.ActionUpdateStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

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
        final ActionUpdateStatus actionUpdateStatus = new ActionUpdateStatus();
        actionUpdateStatus.setActionId(1L);
        actionUpdateStatus.setSoftwareModuleId(2L);
        actionUpdateStatus.getMessage().add("Message 1");
        actionUpdateStatus.getMessage().add("Message 2");

        final Message message = rabbitTemplate.getMessageConverter().toMessage(actionUpdateStatus,
                new MessageProperties());
        ActionUpdateStatus convertedActionUpdateStatus = baseAmqpService.convertMessage(message,
                ActionUpdateStatus.class);

        assertThat(convertedActionUpdateStatus).as("Converted Action Status is wrong")
                .isEqualsToByComparingFields(actionUpdateStatus);

        convertedActionUpdateStatus = baseAmqpService.convertMessage(null, ActionUpdateStatus.class);
        assertThat(convertedActionUpdateStatus).as("Converted Object should be null when message is null").isNull();

        convertedActionUpdateStatus = baseAmqpService.convertMessage(new Message(null, new MessageProperties()),
                ActionUpdateStatus.class);
        assertThat(convertedActionUpdateStatus).as("Converted Object should be null when message body is null")
                .isNull();
    }

    @Test
    @Description("Verify that a conversion of a list from a message works")
    public void convertMessageListTest() {
        final List<ActionUpdateStatus> actionUpdateStatusList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final ActionUpdateStatus actionUpdateStatus = new ActionUpdateStatus();
            actionUpdateStatus.setActionId(Long.valueOf(i));
            actionUpdateStatus.setSoftwareModuleId(Long.valueOf(i));
            actionUpdateStatusList.add(actionUpdateStatus);
        }

        final Message message = rabbitTemplate.getMessageConverter().toMessage(actionUpdateStatusList,
                new MessageProperties());
        List<ActionUpdateStatus> convertedActionUpdateStatus = baseAmqpService.convertMessageList(message,
                ActionUpdateStatus.class);

        assertThat(convertedActionUpdateStatus).as("Converted Action Status list is wrong")
                .hasSameClassAs(actionUpdateStatusList);
        assertThat(convertedActionUpdateStatus).as("Converted Action Status list is wrong")
                .hasSameSizeAs(actionUpdateStatusList);

        convertedActionUpdateStatus = baseAmqpService.convertMessageList(null, ActionUpdateStatus.class);
        assertThat(convertedActionUpdateStatus).as("Converted list should be empty when message is null").isEmpty();

        convertedActionUpdateStatus = baseAmqpService.convertMessageList(new Message(null, new MessageProperties()),
                ActionUpdateStatus.class);
        assertThat(convertedActionUpdateStatus).as("Converted list should be empty when message body is null")
                .isEmpty();
    }

}
